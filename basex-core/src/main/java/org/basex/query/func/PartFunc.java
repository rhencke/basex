package org.basex.query.func;

import static org.basex.query.util.Err.*;

import java.util.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Partial function application.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public final class PartFunc extends Arr {
  /** Static context. */
  private final StaticContext sc;
  /** Positions of the placeholders. */
  private final int[] holes;

  /**
   * Constructor.
   * @param sc static context
   * @param info input info
   * @param expr function expression
   * @param args argument expressions
   * @param holes positions of the placeholders
   */
  public PartFunc(final StaticContext sc, final InputInfo info, final Expr expr, final Expr[] args,
      final int[] holes) {

    super(info, Array.add(args, expr));
    this.sc = sc;
    this.holes = holes;
    type = SeqType.FUN_O;
  }

  @Override
  public Expr compile(final QueryContext ctx, final VarScope scp) throws QueryException {
    super.compile(ctx, scp);
    return optimize(ctx, scp);
  }

  @Override
  public Expr optimize(final QueryContext ctx, final VarScope scp) throws QueryException {
    final Expr f = exprs[exprs.length - 1];
    if(allAreValues()) return preEval(ctx);

    final SeqType t = f.type();
    if(t.instanceOf(SeqType.FUN_O) && t.type != FuncType.ANY_FUN) {
      final FuncType ft = (FuncType) t.type;
      final int arity = exprs.length + holes.length - 1;
      if(ft.args.length != arity) throw INVARITY.get(info, f, arity);
      final SeqType[] ar = new SeqType[holes.length];
      for(int i = 0; i < holes.length; i++) ar[i] = ft.args[holes[i]];
      type = FuncType.get(ft.ret, ar).seqType();
    }

    return this;
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    final Expr fn = exprs[exprs.length - 1];
    final FItem f = (FItem) checkType(fn.item(ctx, ii), FuncType.ANY_FUN);
    final FuncType ft = f.funcType();

    final int arity = exprs.length + holes.length - 1;
    if(f.arity() != arity) throw INVARITY.get(ii, f, arity);
    final Expr[] args = new Expr[arity];

    final VarScope scp = new VarScope(sc);
    final Var[] vars = new Var[holes.length];
    int p = -1;
    for(int i = 0; i < holes.length; i++) {
      while(++p < holes[i]) args[p] = exprs[p - i].value(ctx);
      vars[i] = scp.newLocal(ctx, f.argName(holes[i]), null, false);
      args[p] = new VarRef(info, vars[i]);
      vars[i].refineType(ft.args[p], ctx, ii);
    }
    while(++p < args.length) args[p] = exprs[p - holes.length].value(ctx);

    final Ann ann = f.annotations();
    final FuncType tp = FuncType.get(ann, vars, ft.ret);
    final DynFuncCall fc = new DynFuncCall(info, sc, ann.contains(Ann.Q_UPDATING), f, args);
    return new FuncItem(sc, ann, null, vars, tp, fc, ctx.value, ctx.pos, ctx.size, scp.stackSize());
  }

  @Override
  public void checkUp() throws QueryException {
    checkNoneUp(Arrays.copyOf(exprs, exprs.length - 1));
  }

  @Override
  public Value value(final QueryContext ctx) throws QueryException {
    return item(ctx, info);
  }

  @Override
  public Expr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return new PartFunc(sc, info, exprs[exprs.length - 1].copy(ctx, scp, vs),
        copyAll(ctx, scp, vs, Arrays.copyOf(exprs, exprs.length - 1)), holes.clone());
  }

  @Override
  public void plan(final FElem plan) {
    final FElem e = planElem();
    final int es = exprs.length, hs = holes.length;
    exprs[es - 1].plan(e);
    int p = -1;
    for(int i = 0; i < hs; i++) {
      while(++p < holes[i]) exprs[p - i].plan(e);
      final FElem a = new FElem(QueryText.ARG);
      e.add(a.add(planAttr(QueryText.POS, Token.token(i))));
    }
    while(++p < es + hs - 1) exprs[p - hs].plan(e);
    plan.add(e);
  }

  @Override
  public String toString() {
    final TokenBuilder tb = new TokenBuilder(exprs[exprs.length - 1].toString()).add('(');
    int p = -1;
    final int es = exprs.length, hs = holes.length;
    for(int i = 0; i < hs; i++) {
      while(++p < holes[i])
        tb.add(p > 0 ? QueryText.SEP : "").add(exprs[p - i].toString());
      tb.add(p > 0 ? QueryText.SEP : "").add('?');
    }
    while(++p < es + hs - 1) tb.add(QueryText.SEP).add(exprs[p - hs].toString());
    return tb.add(')').toString();
  }

  /**
   * Returns the function annotations.
   * @return annotations
   */
  public Ann annotations() {
    final Expr fn = exprs[exprs.length - 1];
    if(!(fn instanceof FItem)) return null;
    return ((FItem) fn).annotations();
  }
}
