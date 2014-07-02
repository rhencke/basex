package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Comment fragment.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Christian Gruen
 */
public final class CComm extends CNode {
  /**
   * Constructor.
   * @param sctx static context
   * @param info input info
   * @param comment comment
   */
  public CComm(final StaticContext sctx, final InputInfo info, final Expr comment) {
    super(sctx, info, comment);
    type = SeqType.COM;
  }

  @Override
  public FComm item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    final Iter iter = ctx.iter(exprs[0]);

    final TokenBuilder tb = new TokenBuilder();
    boolean more = false;
    for(Item it; (it = iter.next()) != null;) {
      if(more) tb.add(' ');
      tb.add(it.string(ii));
      more = true;
    }
    return new FComm(FComm.parse(tb.finish(), info));
  }

  @Override
  public Expr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return new CComm(sc, info, exprs[0].copy(ctx, scp, vs));
  }

  @Override
  public String description() {
    return info(COMMENT);
  }

  @Override
  public String toString() {
    return toString(COMMENT);
  }
}
