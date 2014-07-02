package org.basex.query.ft;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.node.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.ft.*;
import org.basex.util.hash.*;

/**
 * FTOptions expression.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Christian Gruen
 */
public final class FTOpts extends FTExpr {
  /** FTOptions. */
  private final FTOpt opt;

  /**
   * Constructor.
   * @param info input info
   * @param expr expression
   * @param opt ft options
   */
  public FTOpts(final InputInfo info, final FTExpr expr, final FTOpt opt) {
    super(info, expr);
    this.opt = opt;
  }

  @Override
  public FTExpr compile(final QueryContext ctx, final VarScope scp) throws QueryException {
    final FTOpt tmp = ctx.ftOpt();
    ctx.ftOpt(opt.copy(tmp));
    if(opt.sw != null && ctx.value != null && ctx.value.data() != null)
      opt.sw.comp(ctx.value.data());
    exprs[0] = exprs[0].compile(ctx, scp);
    ctx.ftOpt(tmp);
    return exprs[0];
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(), opt, exprs[0]);
  }

  @Override
  public String toString() {
    return exprs[0].toString() + opt;
  }

  @Override
  public FTNode item(final QueryContext ctx, final InputInfo ii) {
    // shouldn't be called, as compile returns argument
    throw Util.notExpected();
  }

  @Override
  public FTIter iter(final QueryContext ctx) {
    // shouldn't be called, as compile returns argument
    throw Util.notExpected();
  }

  @Override
  public FTExpr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return new FTOpts(info, exprs[0].copy(ctx, scp, vs), new FTOpt().copy(opt));
  }
}
