package org.basex.query.value.seq.tree;

import org.basex.query.value.item.Item;

/**
 * A partial leaf node containing fewer elements than required in a node.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Leo Woerteler
 */
final class PartialLeafNode extends org.basex.query.value.PartialLeafNode<LeafNode, PartialLeafNode, Item> {

  /**
   * Constructor.
   * @param elems the elements
   */
  PartialLeafNode(final Item[] elems) {
    super(elems);
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

  Item[] values() {
    return values;
  }
}
