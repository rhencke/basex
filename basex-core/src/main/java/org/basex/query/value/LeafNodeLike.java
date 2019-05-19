package org.basex.query.value;

import org.basex.query.util.fingertree.NodeLike;
import org.basex.util.Util;

import java.util.Arrays;

public abstract class LeafNodeLike<
    N extends LeafNode<N, P, E>,
    P extends PartialLeafNode<N, P, E>,
    E
> implements NodeLike<E, E> {
  protected final E[] values;

  LeafNodeLike(E[] values) {
    this.values = values;
  }

  protected abstract E[] newValuesArray(int size);

  protected abstract P newPartialLeafNode(E[] values);

  protected abstract N newLeafNode(E[] values);

  public long size() {
    return values.length;
  }

  // TODO consolidate append?

  @Override
  public String toString() {
    return Util.className(this) + '(' + size() + ')' + Arrays.toString(values);
  }
}
