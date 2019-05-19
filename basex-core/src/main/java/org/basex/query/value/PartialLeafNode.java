package org.basex.query.value;

import org.basex.query.util.fingertree.NodeLike;
import org.basex.util.Array;

public abstract class PartialLeafNode<
    N extends LeafNode<N, P, E>,
    P extends PartialLeafNode<N, P, E>,
    E
> extends LeafNodeLike<N, P, E> {

  protected PartialLeafNode(E[] values) {
    super(values);
  }

  @Override
  public int append(NodeLike<E, E>[] nodes, int pos) {
    if (pos == 0) {
      nodes[0] = this;
      return 1;
    }

    final NodeLike<E, E> left = nodes[pos - 1];
    if (left instanceof PartialLeafNode) {
      @SuppressWarnings("unchecked") final E[] ls = ((P) left).values;
      final E[] rs = values;
      final int l = ls.length, r = rs.length, n = l + r;
      final E[] vals = newValuesArray(n);
      Array.copy(ls, l, vals);
      Array.copyFromStart(rs, r, vals, l);
      nodes[pos - 1] = n < LeafNode.MIN_LEAF ? newPartialLeafNode(vals) : newLeafNode(vals);
      return pos;
    }

    @SuppressWarnings("unchecked") final E[] ls = ((N) left).values;
    final E[] rs = values;
    final int l = ls.length, r = rs.length, n = l + r;
    if (n <= LeafNode.MAX_LEAF) {
      final E[] vals = newValuesArray(n);
      Array.copy(ls, l, vals);
      Array.copyFromStart(rs, r, vals, l);
      nodes[pos - 1] = newLeafNode(vals);
      return pos;
    }

    final int ll = n / 2, rl = n - ll, move = l - ll;
    final E[] newLeft = newValuesArray(ll), newRight = newValuesArray(rl);
    Array.copy(ls, ll, newLeft);
    Array.copyToStart(ls, ll, move, newRight);
    Array.copyFromStart(rs, r, newRight, move);
    nodes[pos - 1] = newLeafNode(newLeft);
    nodes[pos] = newLeafNode(newRight);
    return pos + 1;
  }
}
