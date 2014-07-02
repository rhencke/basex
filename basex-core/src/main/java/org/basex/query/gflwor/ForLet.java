package org.basex.query.gflwor;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.gflwor.GFLWOR.Clause;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * FLWOR {@code for}/{@code let} clause.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Christian Gruen
 */
abstract class ForLet extends GFLWOR.Clause {
  /** Item variable. */
  public final Var var;
  /** Bound expression. */
  public Expr expr;

  /**
   * Constructor.
   * @param info input info
   * @param var variable
   * @param expr expression
   * @param vars variable
   */
  protected ForLet(final InputInfo info, final Var var, final Expr expr, final Var... vars) {
    super(info, vars);
    this.var = var;
    this.expr = expr;
  }

  @Override
  public Clause compile(final QueryContext ctx, final VarScope scp) throws QueryException {
    expr = expr.compile(ctx, scp);
    return optimize(ctx, scp);
  }

  @Override
  public boolean has(final Flag flag) {
    return expr.has(flag);
  }

  @Override
  public final boolean removable(final Var v) {
    return expr.removable(v);
  }

  @Override
  public final VarUsage count(final Var v) {
    return expr.count(v);
  }

  @Override
  public final Clause inline(final QueryContext ctx, final VarScope scp, final Var v, final Expr e)
      throws QueryException {

    final Expr sub = expr.inline(ctx, scp, v, e);
    if(sub == null) return null;
    expr = sub;
    // call compile instead of optimize, because many new optimizations may be triggered by inlining
    return compile(ctx, scp);
  }

  @Override
  public final void checkUp() throws QueryException {
    checkNoUp(expr);
  }

  @Override
  public final int exprSize() {
    return expr.exprSize();
  }
}
