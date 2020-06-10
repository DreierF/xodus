/**
 * Copyright 2010 - 2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus.tree.patricia;

import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ByteIterator;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.log.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ImmutableNode extends NodeBase {

    @NotNull
    private final RandomAccessLoggable loggable;
    @NotNull
    private final ByteIterableWithAddress data;
    private final int dataOffset;
    private final short childrenCount;
    private final byte childAddressLength;

    ImmutableNode(@NotNull final RandomAccessLoggable loggable, @NotNull final ByteIterableWithAddress data) {
        this(loggable.getType(), loggable, data, data.iterator());
    }

    private ImmutableNode(final byte type,
                          @NotNull final RandomAccessLoggable loggable,
                          @NotNull final ByteIterableWithAddress data,
                          @NotNull final ByteIteratorWithAddress it) {
        super(type, data, it);
        this.loggable = loggable;
        this.data = data;
        if (PatriciaTreeBase.nodeHasChildren(type)) {
            final int i = CompressedUnsignedLongByteIterable.getInt(it);
            childrenCount = (short) (i >> 3);
            checkAddressLength(childAddressLength = (byte) ((i & 7) + 1));
        } else {
            childrenCount = (short) 0;
            childAddressLength = (byte) 0;
        }
        dataOffset = (int) (it.getAddress() - data.getDataAddress());
    }

    /**
     * Creates empty node for an empty tree.
     */
    ImmutableNode() {
        super(ByteIterable.EMPTY, null);
        loggable = NullLoggable.create();
        data = ByteIterableWithAddress.EMPTY;
        dataOffset = 0;
        childrenCount = (short) 0;
        childAddressLength = (byte) 0;
    }

    @NotNull
    public RandomAccessLoggable getLoggable() {
        return loggable;
    }

    @Override
    long getAddress() {
        return loggable.getAddress();
    }

    @Override
    boolean isMutable() {
        return false;
    }

    @Override
    MutableNode getMutableCopy(@NotNull final PatriciaTreeMutable mutableTree) {
        return mutableTree.mutateNode(this);
    }

    @Override
    @Nullable
    NodeBase getChild(@NotNull final PatriciaTreeBase tree, final byte b) {
        final int key = b & 0xff;
        int low = 0;
        int high = childrenCount - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final int offset = dataOffset + mid * (childAddressLength + 1);
            final int cmp = (data.byteAt(offset) & 0xff) - key;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return tree.loadNode(data.nextLong(offset + 1, childAddressLength));
            }
        }
        return null;
    }

    @Override
    @NotNull
    NodeChildren getChildren() {
        return () -> childrenCount == (short) 0 ?
            new EmptyNodeChildrenIterator() :
            new ImmutableNodeChildrenIterator(getDataIterator(0), -1, null);
    }

    @Override
    @NotNull
    NodeChildrenIterator getChildren(final byte b) {
        final int key = b & 0xff;
        int low = 0;
        int high = childrenCount - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final int offset = dataOffset + mid * (childAddressLength + 1);
            final int cmp = (data.byteAt(offset) & 0xff) - key;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                final long suffixAddress = data.nextLong(offset + 1, childAddressLength);
                return new ImmutableNodeChildrenIterator(
                    data.iterator(offset + childAddressLength + 1), mid, new ChildReference(b, suffixAddress));
            }
        }
        return new EmptyNodeChildrenIterator();
    }

    @Override
    @NotNull
    NodeChildrenIterator getChildrenRange(final byte b) {
        final int key = b & 0xff;
        int low = -1;
        int high = childrenCount;
        int resultOffset = -1;
        byte resultByte = (byte) 0;
        while (high - low > 1) {
            final int mid = (low + high + 1) >>> 1;
            final int offset = dataOffset + mid * (childAddressLength + 1);
            final byte actual = data.byteAt(offset);
            if ((actual & 0xff) > key) {
                resultOffset = offset;
                resultByte = actual;
                high = mid;
            } else {
                low = mid;
            }
        }
        if (resultOffset >= 0) {
            final ByteIteratorWithAddress it = data.iterator(resultOffset + 1);
            final long suffixAddress = it.nextLong(childAddressLength);
            return new ImmutableNodeChildrenIterator(it, high, new ChildReference(resultByte, suffixAddress));
        }
        return new EmptyNodeChildrenIterator();
    }


    @Override
    int getChildrenCount() {
        return childrenCount;
    }

    @NotNull
    @Override
    NodeChildrenIterator getChildrenLast() {
        return new ImmutableNodeChildrenIterator(null, childrenCount, null);
    }

    private ByteIterator getDataIterator(final int offset) {
        return getAddress() == Loggable.NULL_ADDRESS ? ByteIterable.EMPTY_ITERATOR : data.iterator(dataOffset + offset);
    }


    private static void checkAddressLength(long addressLen) {
        if (addressLen < 0 || addressLen > 8) {
            throw new ExodusException("Invalid length of address: " + addressLen);
        }
    }

    private final class ImmutableNodeChildrenIterator implements NodeChildrenIterator {

        private ByteIterator itr;
        private int index;
        private ChildReference node;

        private ImmutableNodeChildrenIterator(ByteIterator itr, int index, ChildReference node) {
            this.itr = itr;
            this.index = index;
            this.node = node;
        }

        @Override
        public boolean hasNext() {
            return index < childrenCount - 1;
        }

        @Override
        public ChildReference next() {
            ++index;
            return node = new ChildReference(itr.next(), itr.nextLong(childAddressLength));
        }

        @Override
        public boolean hasPrev() {
            return index > 0;
        }

        @Override
        public ChildReference prev() {
            --index;
            itr = getDataIterator(index * (childAddressLength + 1));
            return node = new ChildReference(itr.next(), itr.nextLong(childAddressLength));
        }

        @Override
        public boolean isMutable() {
            return false;
        }

        @Override
        public void nextInPlace() {
            ++index;
            final ChildReference node = this.node;
            node.firstByte = itr.next();
            node.suffixAddress = itr.nextLong(childAddressLength);
        }

        @Override
        public void prevInPlace() {
            --index;
            final ChildReference node = this.node;
            itr = getDataIterator(index * (childAddressLength + 1));
            node.firstByte = itr.next();
            node.suffixAddress = itr.nextLong(childAddressLength);
        }

        @Override
        public ChildReference getNode() {
            return node;
        }

        @Override
        public void remove() {
            throw new ExodusException("Can't remove manually Patricia node child, use Store.delete() instead");
        }

        @Override
        public NodeBase getParentNode() {
            return ImmutableNode.this;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public ByteIterable getKey() {
            return keySequence;
        }
    }
}