package org.basex.query.func.fn;

import org.basex.core.MainOptions;
import org.basex.query.*;
import org.basex.query.ann.Ann;
import org.basex.query.ann.Annotation;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.list.AnnList;
import org.basex.query.value.*;
import org.basex.query.value.item.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class FnFoldLeft extends StandardFunc {
  @Override
  public Value value(final QueryContext qc) throws QueryException {
    final Iter iter = qc.iter(exprs[0]);
    final FItem fun = checkArity(exprs[2], 2, qc);
    Value res = qc.value(exprs[1]);
    for(Item it; (it = iter.next()) != null;) {
      qc.checkStop();
      res = fun.invokeValue(qc, info, res, it);
    }
    return res;
  }

  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    final Iter iter = qc.iter(exprs[0]);
    final FItem fun = checkArity(exprs[2], 2, qc);

    // don't convert to a value if not necessary
    Item it = iter.next();
    if(it == null) return qc.iter(exprs[1]);

    Value res = qc.value(exprs[1]);
    do {
      qc.checkStop();
      res = fun.invokeValue(qc, info, res, it);
    } while((it = iter.next()) != null);
    return res.iter();
  }


  /**
   * Checks if unrolling conditions are given.
   * @param cc compilation context
   * @param anns annotations
   * @param expr expression
   * @return result of check
   */
  private static boolean unroll(final CompileContext cc, final AnnList anns, final Expr expr) {
    Ann ann = anns.get(Annotation._BASEX_UNROLL);
    final long limit;
    if (ann == null) {
      limit = cc.qc.context.options.get(MainOptions.UNROLLLIMIT);
    } else {
      final Item[] args1 = ann.args();
      limit = args1.length > 0 ? ((ANum) args1[0]).itr() : Long.MAX_VALUE;
    }
    return expr.size() < limit;
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    if(allAreValues() && unroll(cc, ((FuncItem)exprs[2]).annotations(), exprs[0])) {
      // unroll the loop
      final Value seq = (Value) exprs[0];
      Expr e = exprs[1];
      for(final Item it : seq) {
        e = new DynFuncCall(info, sc, exprs[2], e, it).optimize(cc);
      }
      cc.info(QueryText.OPTUNROLL_X, this);
      return e;
    }
    return this;
  }
}
