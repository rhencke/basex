package org.basex.query.value.seq.tree;

import org.basex.query.util.fingertree.Node;
import org.basex.query.util.fingertree.NodeLike;
import org.basex.query.value.item.Item;
import org.basex.util.Array;

/**
 * A leaf node containing {@link Item}s.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Leo Woerteler
 */
final class LeafNode extends org.basex.query.value.LeafNode<LeafNode, PartialLeafNode, Item> {

  /**
   * Constructor.
   *
   * @param values the values
   */
  LeafNode(final Item[] values) {
    super(values);
  }

  @Override
  protected Item[] newValuesArray(int size) {
    return new Item[size];
  }

  @Override
  protected PartialLeafNode newPartialLeafNode(Item[] values) {
    return new PartialLeafNode(values);
  }

  @Override
  protected LeafNode newLeafNode(Item[] values) {
    return new LeafNode(values);
  }

  @Override
  public NodeLike<Item, Item>[] remove(final Node<Item, Item> left,
                                       final Node<Item, Item> right, final long pos) {
    final int p = (int) pos, n = values.length;
    @SuppressWarnings("unchecked") final NodeLike<Item, Item>[] out = new NodeLike[]{left, null, right};
    if (n > TreeSeq.MIN_LEAF) {
      // we do not have to split
      final Item[] vals = new Item[n - 1];
      Array.copy(values, p, vals);
      Array.copy(values, p + 1, n - 1 - p, vals, p);
      out[1] = new LeafNode(vals);
      return out;
    }

    final LeafNode leftLeaf = (LeafNode) left;
    if (leftLeaf != null && leftLeaf.arity() > TreeSeq.MIN_LEAF) {
      // steal from the left neighbor
      final Item[] lvals = leftLeaf.values;
      final int l = lvals.length, diff = l - TreeSeq.MIN_LEAF, move = (diff + 1) / 2;
      final int ll = l - move, rl = n - 1 + move;
      final Item[] newLeft = new Item[ll], newRight = new Item[rl];

      Array.copy(lvals, ll, newLeft);
      Array.copyToStart(lvals, ll, move, newRight);
      Array.copyFromStart(values, p, newRight, move);
      Array.copy(values, p + 1, n - 1 - p, newRight, move + p);
      out[0] = new LeafNode(newLeft);
      out[1] = new LeafNode(newRight);
      return out;
    }

    final LeafNode rightLeaf = (LeafNode) right;
    if (rightLeaf != null && rightLeaf.arity() > TreeSeq.MIN_LEAF) {
      // steal from the right neighbor
      final Item[] rvals = rightLeaf.values;
      final int r = rvals.length, diff = r - TreeSeq.MIN_LEAF, move = (diff + 1) / 2;
      final int ll = n - 1 + move, rl = r - move;
      final Item[] newLeft = new Item[ll], newRight = new Item[rl];

      Array.copy(values, p, newLeft);
      Array.copy(values, p + 1, n - 1 - p, newLeft, p);
      Array.copyFromStart(rvals, move, newLeft, n - 1);
      Array.copyToStart(rvals, move, rl, newRight);
      out[1] = new LeafNode(newLeft);
      out[2] = new LeafNode(newRight);
      return out;
    }

    if (left != null) {
      // merge with left neighbor
      final Item[] lvals = ((LeafNode) left).values;
      final int l = lvals.length, r = values.length;
      final Item[] vals = new Item[l + r - 1];
      Array.copy(lvals, l, vals);
      Array.copyFromStart(values, p, vals, l);
      Array.copy(values, p + 1, r - 1 - p, vals, l + p);
      out[0] = new LeafNode(vals);
      return out;
    }

    if (right != null) {
      // merge with right neighbor
      final Item[] rvals = ((LeafNode) right).values;
      final int l = values.length, r = rvals.length;
      final Item[] vals = new Item[l - 1 + r];
      Array.copy(values, p, vals);
      Array.copy(values, p + 1, l - 1 - p, vals, p);
      Array.copyFromStart(rvals, r, vals, l - 1);
      out[2] = new LeafNode(vals);
      return out;
    }

    // underflow
    final Item[] vals = new Item[n - 1];
    Array.copy(values, p, vals);
    Array.copy(values, p + 1, n - 1 - p, vals, p);
    out[1] = new PartialLeafNode(vals);
    return out;
  }

  @Override
  public int arity() {
    return values.length;
  }

  @Override
  public Item getSub(final int index) {
    return values[index];
  }

  Item[] values(){
    return values;
  }
}
