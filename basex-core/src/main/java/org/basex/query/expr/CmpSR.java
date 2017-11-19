package org.basex.query.expr;

import static org.basex.query.QueryError.*;
import static org.basex.query.QueryText.*;

import java.util.*;

import org.basex.data.*;
import org.basex.index.*;
import org.basex.index.query.*;
import org.basex.query.*;
import org.basex.query.expr.CmpG.*;
import org.basex.query.expr.index.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.util.collation.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * String range expression.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class CmpSR extends Single {
  /** Collation (can be {@code null}). */
  private final Collation coll;
  /** Minimum. */
  private final byte[] min;
  /** Include minimum value. */
  private final boolean mni;
  /** Maximum. */
  private final byte[] max;
  /** Include maximum value. */
  private final boolean mxi;

  /** Flag for atomic evaluation. */
  private boolean atomic;

  /**
   * Constructor.
   * @param expr (compiled) expression
   * @param min minimum value
   * @param mni include minimum value
   * @param max maximum value
   * @param mxi include maximum value
   * @param coll collation (can be {@code null})
   * @param info input info
   */
  private CmpSR(final Expr expr, final byte[] min, final boolean mni, final byte[] max,
      final boolean mxi, final Collation coll, final InputInfo info) {

    super(info, expr, SeqType.BLN);
    this.coll = coll;
    this.min = min;
    this.mni = mni;
    this.max = max;
    this.mxi = mxi;
  }

  @Override
  public Expr compile(final CompileContext cc) throws QueryException {
    return super.compile(cc).optimize(cc);
  }

  @Override
  public Expr optimize(final CompileContext cc) throws QueryException {
    final SeqType st = expr.seqType();
    atomic = st.zeroOrOne() && !st.mayBeArray();
    return expr instanceof Value ? cc.preEval(this) : this;
  }

  /**
   * Tries to convert the specified expression into a range expression.
   * @param cmp expression to be converted
   * @param cc compilation context
   * @return new or original expression
   * @throws QueryException query exception
   */
  static Expr get(final CmpG cmp, final CompileContext cc) throws QueryException {
    final Expr e1 = cmp.exprs[0], e2 = cmp.exprs[1];
    if(e1.has(Flag.NDT) || !(e2 instanceof AStr)) return cmp;

    final byte[] d = ((AStr) e2).string(cmp.info);
    ParseExpr ex = null;
    switch(cmp.op.op) {
      case GE: ex = new CmpSR(e1, d,    true,  null, true,  cmp.coll, cmp.info); break;
      case GT: ex = new CmpSR(e1, d,    false, null, true,  cmp.coll, cmp.info); break;
      case LE: ex = new CmpSR(e1, null, true,  d,    true,  cmp.coll, cmp.info); break;
      case LT: ex = new CmpSR(e1, null, true,  d,    false, cmp.coll, cmp.info); break;
      default:
    }
    return ex != null ? ex.optimize(cc) : cmp;
  }

  @Override
  public Bln item(final QueryContext qc, final InputInfo ii) throws QueryException {
    // atomic evaluation of arguments (faster)
    if(atomic) {
      final Item it = expr.item(qc, info);
      return Bln.get(it != null && eval(it));
    }

    // iterative evaluation
    final Iter iter = expr.atomIter(qc, info);
    for(Item it; (it = iter.next()) != null;) {
      qc.checkStop();
      if(eval(it)) return Bln.TRUE;
    }
    return Bln.FALSE;
  }

  /**
   * Evaluates the range for the specified item.
   * @param it item to be evaluated
   * @return result of check
   * @throws QueryException query exception
   */
  private boolean eval(final Item it) throws QueryException {
    if(!it.type.isStringOrUntyped()) throw diffError(it, Str.ZERO, info);
    final byte[] s = it.string(info);
    final int mn = min == null ?  1 :
      coll == null ? Token.diff(s, min) : coll.compare(s, min);
    final int mx = max == null ? -1 :
      coll == null ? Token.diff(s, max) : coll.compare(s, max);
    return (mni ? mn >= 0 : mn > 0) && (mxi ? mx <= 0 : mx < 0);
  }

  /**
   * Creates an intersection of the existing and the specified expressions.
   * @param c range comparison
   * @return resulting expression or {@code null}
   */
  Expr intersect(final CmpSR c) {
    // skip intersection if expressions to be compared are different
    if(!(coll == null && c.expr.equals(expr))) return null;

    // find common minimum and maximum value
    final byte[] mn = min == null ? c.min : c.min == null ? min : Token.max(min, c.min);
    final byte[] mx = max == null ? c.max : c.max == null ? max : Token.min(max, c.max);

    if(mn != null && mx != null) {
      final int d = Token.diff(mn, mx);
      // remove comparisons that will never yield results
      if(d > 0) return Bln.FALSE;
      if(d == 0) {
        // return simplified comparison for exact hit, or false if value is not included
        return mni && mxi ? new CmpG(expr, Str.get(mn), OpG.EQ, null, null, info) : Bln.FALSE;
      }
    }
    return new CmpSR(c.expr, mn, mni && c.mni, mx, mxi && c.mxi, null, info);
  }

  @Override
  public boolean indexAccessible(final IndexInfo ii) {
    // only default collation is supported, and min/max values are required
    if(coll != null || min == null || max == null) return false;

    // accept only location path, string and equality expressions
    final Data data = ii.db.data();
    // sequential main memory scan is usually faster than range index access
    if(data == null ? !ii.enforce() : data.inMemory()) return false;

    final IndexType type = ii.type(expr, null);
    if(type == null) return false;

    // create range access
    final StringRange sr = new StringRange(type, min, mni, max, mxi);
    ii.costs = ii.costs(data, sr);
    if(ii.costs == null) return false;

    final TokenBuilder tb = new TokenBuilder();
    tb.add(mni ? '[' : '(').addExt(min).add(',').addExt(max).add(mxi ? ']' : ')');
    ii.create(new StringRangeAccess(info, sr, ii.db), true, info,
        Util.info(OPTINDEX_X_X, type + " string range", tb));
    return true;
  }

  @Override
  public Expr copy(final CompileContext cc, final IntObjMap<Var> vm) {
    final CmpSR cmp = new CmpSR(expr.copy(cc, vm), min, mni, max, mxi, coll, info);
    cmp.atomic = atomic;
    return cmp;
  }

  @Override
  public boolean equals(final Object obj) {
    if(this == obj) return true;
    if(!(obj instanceof CmpSR)) return false;
    final CmpSR c = (CmpSR) obj;
    return Token.eq(min, c.min) && mni == c.mni && Token.eq(max, c.max) && mxi && c.mxi &&
        Objects.equals(coll, c.coll) && super.equals(obj);
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(MIN, min != null ? min : "", MAX, max != null ? max : ""), expr);
  }

  @Override
  public String description() {
    return "string range comparison";
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(PAREN1);
    if(min != null) sb.append('"').append(min).append('"').append(mni ? " <= " : " < ");
    sb.append(expr);
    if(max != null) sb.append(mxi ? " <= " : " < ").append('"').append(max).append('"');
    return sb.append(PAREN2).toString();
  }
}
