package org.basex.query.value.array;

import java.util.*;

import org.basex.query.value.*;
import org.basex.query.value.item.Item;
import org.basex.util.*;

/**
 * A partial leaf node containing fewer elements than required in a node.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Leo Woerteler
 */
final class PartialLeafNode extends org.basex.query.value.PartialLeafNode<LeafNode, PartialLeafNode, Value> {
  /** The single element. */
  final Value[] elems;

  /**
   * Constructor.
   * @param elems the elements
   */
  PartialLeafNode(final Value[] elems) {
    super(elems);
    this.elems = elems;
  }

  @Override
  protected Value[] newValuesArray(int size) {
    return new Value[size];
  }

  @Override
  protected PartialLeafNode newPartialLeafNode(Value[] values) {
    return new PartialLeafNode(values);
  }

  @Override
  protected LeafNode newLeafNode(Value[] values) {
    return new LeafNode(values);
  }

}
