package org.basex.query.value;

import org.basex.query.util.fingertree.Node;
import org.basex.query.util.fingertree.NodeLike;
import org.basex.util.Array;
import org.basex.util.Util;

/**
 * A leaf node containing {@link Value}s.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Leo Woerteler
 */
public abstract class LeafNode<
    N extends LeafNode<N, P, E>,
    P extends PartialLeafNode<N, P, E>,
    E
> extends LeafNodeLike<N, P, E> implements Node<E, E> {
  /**
   * Minimum size of a leaf.
   */
  static final int MIN_LEAF = 8;
  /**
   * Maximum size of a leaf.
   */
  static final int MAX_LEAF = 2 * MIN_LEAF - 1;
  /**
   * Minimum number of elements in a digit.
   */
  static final int MIN_DIGIT = MIN_LEAF / 2;
  /**
   * Maximum number of elements in a digit.
   */
  static final int MAX_DIGIT = MAX_LEAF + MIN_DIGIT;
  /**
   * Maximum size of a small array.
   */
  static final int MAX_SMALL = 2 * MIN_DIGIT - 1;


  protected LeafNode(final E[] values) {
    super(values);
    assert values.length >= MIN_LEAF && values.length <= MAX_LEAF;
  }

  public boolean insert(final Node<E, E>[] siblings, final long pos, final E val) {
    final int p = (int) pos, n = values.length;
    final E[] vals = newValuesArray(n + 1);
    Array.copy(values, p, vals);
    vals[p] = val;
    Array.copy(values, p, n - p, vals, p + 1);

    if (n < MAX_LEAF) {
      // there is capacity
      siblings[1] = newLeafNode(vals);
      return false;
    }

    @SuppressWarnings("unchecked") final N left = (N) siblings[0];
    if (left != null && left.values.length < MAX_LEAF) {
      // push elements to the left sibling
      final E[] lvals = left.values;
      final int l = lvals.length, diff = MAX_LEAF - l, move = (diff + 1) / 2;
      final E[] newLeft = newValuesArray(l + move), newRight = newValuesArray(n + 1 - move);
      Array.copy(lvals, l, newLeft);
      Array.copyFromStart(vals, move, newLeft, l);
      Array.copyToStart(vals, move, newRight.length, newRight);
      siblings[0] = newLeafNode(newLeft);
      siblings[1] = newLeafNode(newRight);
      return false;
    }

    @SuppressWarnings("unchecked") final N right = (N) siblings[2];
    if (right != null && right.values.length < MAX_LEAF) {
      // push elements to the right sibling
      final E[] rvals = right.values;
      final int r = rvals.length, diff = MAX_LEAF - r, move = (diff + 1) / 2,
          l = n + 1 - move;
      final E[] newLeft = newValuesArray(l), newRight = newValuesArray(r + move);
      Array.copy(vals, l, newLeft);
      Array.copyToStart(vals, l, move, newRight);
      Array.copyFromStart(rvals, r, newRight, move);
      siblings[1] = newLeafNode(newLeft);
      siblings[2] = newLeafNode(newRight);
      return false;
    }

    // split the node
    final int l = vals.length / 2, r = vals.length - l;
    final E[] newLeft = newValuesArray(l), newRight = newValuesArray(r);
    Array.copy(vals, l, newLeft);
    Array.copyToStart(vals, l, r, newRight);
    siblings[3] = siblings[2];
    siblings[1] = newLeafNode(newLeft);
    siblings[2] = newLeafNode(newRight);
    return true;
  }

  @Override
  public Node<E, E> reverse() {
    final int n = values.length;
    final E[] out = newValuesArray(n);
    for (int i = 0; i < n; i++) out[i] = values[n - 1 - i];
    return newLeafNode(out);
  }

  @Override
  public N set(final long pos, final E val) {
    final E[] vals = values.clone();
    vals[(int) pos] = val;
    return newLeafNode(vals);
  }

  @Override
  public NodeLike<E, E>[] remove(Node<E, E> left, Node<E, E> right, long pos) {
    final int p = (int) pos, n = values.length;
    @SuppressWarnings("unchecked") final NodeLike<E, E>[] out = new NodeLike[]{left, null, right};
    if (n > MIN_LEAF) {
      // we do not have to split
      final E[] vals = newValuesArray(n - 1);
      Array.copy(values, p, vals);
      Array.copy(values, p + 1, n - 1 - p, vals, p);
      out[1] = newLeafNode(vals);
      return out;
    }

    @SuppressWarnings("unchecked") final N leftLeaf = (N) left;
    if (leftLeaf != null && leftLeaf.arity() > MIN_LEAF) {
      // steal from the left neighbor
      final E[] lvals = leftLeaf.values;
      final int l = lvals.length, diff = l - MIN_LEAF, move = (diff + 1) / 2;
      final int ll = l - move, rl = n - 1 + move;
      final E[] newLeft = newValuesArray(ll), newRight = newValuesArray(rl);

      Array.copy(lvals, ll, newLeft);
      Array.copyToStart(lvals, ll, move, newRight);
      Array.copyFromStart(values, p, newRight, move);
      Array.copy(values, p + 1, n - 1 - p, newRight, move + p);
      out[0] = newLeafNode(newLeft);
      out[1] = newLeafNode(newRight);
      return out;
    }

    @SuppressWarnings("unchecked") final N rightLeaf = (N) right;
    if (rightLeaf != null && rightLeaf.arity() > MIN_LEAF) {
      // steal from the right neighbor
      final E[] rvals = rightLeaf.values;
      final int r = rvals.length, diff = r - MIN_LEAF, move = (diff + 1) / 2;
      final int ll = n - 1 + move, rl = r - move;
      final E[] newLeft = newValuesArray(ll), newRight = newValuesArray(rl);

      Array.copy(values, p, newLeft);
      Array.copy(values, p + 1, n - 1 - p, newLeft, p);
      Array.copyFromStart(rvals, move, newLeft, n - 1);
      Array.copyToStart(rvals, move, rl, newRight);
      out[1] = newLeafNode(newLeft);
      out[2] = newLeafNode(newRight);
      return out;
    }

    if (leftLeaf != null) {
      // merge with left neighbor
      final E[] lvals = leftLeaf.values;
      final int l = lvals.length, r = values.length;
      final E[] vals = newValuesArray(l + r - 1);
      Array.copy(lvals, l, vals);
      Array.copyFromStart(values, p, vals, l);
      Array.copy(values, p + 1, r - 1 - p, vals, l + p);
      out[0] = newLeafNode(vals);
      return out;
    }

    if (rightLeaf != null) {
      // merge with right neighbor
      final E[] rvals = rightLeaf.values;
      final int l = values.length, r = rvals.length;
      final E[] vals = newValuesArray(l - 1 + r);
      Array.copy(values, p, vals);
      Array.copy(values, p + 1, l - 1 - p, vals, p);
      Array.copyFromStart(rvals, r, vals, l - 1);
      out[2] = newLeafNode(vals);
      return out;
    }

    // underflow
    final E[] vals = newValuesArray(n - 1);
    Array.copy(values, p, vals);
    Array.copy(values, p + 1, n - 1 - p, vals, p);
    out[1] = newPartialLeafNode(vals);
    return out;
  }

  @Override
  public int append(final NodeLike<E, E>[] nodes, final int pos) {
    if (pos == 0) {
      nodes[0] = this;
      return 1;
    }

    final NodeLike<E, E> left = nodes[pos - 1];
    if (!(left instanceof PartialLeafNode)) {
      nodes[pos] = this;
      return pos + 1;
    }

    @SuppressWarnings("unchecked") final E[] ls = ((P) left).values;
    final E[] rs = values;
    final int l = ls.length, r = rs.length, n = l + r;
    if (n <= MAX_LEAF) {
      // merge into one node
      final E[] vals = newValuesArray(n);
      Array.copy(ls, l, vals);
      Array.copyFromStart(rs, r, vals, l);
      nodes[pos - 1] = newLeafNode(vals);
      return pos;
    }

    // split into two
    final int ll = n / 2, rl = n - ll, move = r - rl;
    final E[] newLeft = newValuesArray(ll), newRight = newValuesArray(rl);
    Array.copy(ls, l, newLeft);
    Array.copyFromStart(rs, move, newLeft, l);
    Array.copyToStart(rs, move, rl, newRight);
    nodes[pos - 1] = newLeafNode(newLeft);
    nodes[pos] = newLeafNode(newRight);
    return pos + 1;
  }

  @Override
  public NodeLike<E, E> slice(final long off, final long size) {
    final int p = (int) off, n = (int) size;
    final E[] out = newValuesArray(n);
    Array.copyToStart(values, p, n, out);
    return n < MIN_LEAF ? newPartialLeafNode(out) : newLeafNode(out);
  }

  @Override
  public long checkInvariants() {
    if (values.length < MIN_LEAF || values.length > MAX_LEAF)
      throw new AssertionError("Wrong " + Util.className(this) + " size: " + values.length);
    return values.length;
  }
}
