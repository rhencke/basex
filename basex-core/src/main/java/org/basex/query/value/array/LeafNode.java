package org.basex.query.value.array;

import java.util.*;

import org.basex.query.util.fingertree.*;
import org.basex.query.value.*;
import org.basex.util.*;

/**
 * A leaf node containing {@link Value}s.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Leo Woerteler
 */
final class LeafNode extends org.basex.query.value.LeafNode<LeafNode, PartialLeafNode, Value> {

  /**
   * Constructor.
   * @param values the values
   */
  LeafNode(final Value[] values) {
    super(values);
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

  @Override
  public LeafNode reverse() {
    final int n = values.length;
    final Value[] out = new Value[n];
    for(int i = 0; i < n; i++) out[i] = values[n - 1 - i];
    return new LeafNode(out);
  }

  @Override
  public int arity() {
    return values.length;
  }

  @Override
  public Value getSub(final int index) {
    return values[index];
  }

  @Override
  public String toString() {
    return Util.className(this) + '(' + size() + ')' + Arrays.toString(values);
  }

  Value[] values() {
    return values;
  }
}
