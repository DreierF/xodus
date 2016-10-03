/**
 * Copyright 2010 - 2016 JetBrains s.r.o.
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
package jetbrains.exodus;

import jetbrains.exodus.bindings.LongBinding;

public abstract class ByteIterator {

    public abstract boolean hasNext();

    public abstract byte next();

    /**
     * @param length items to skip
     *               result is undefined for non-positive length
     * @return how many items were skipped
     * zero if no elements left
     */
    public abstract long skip(long length);

    public long nextLong(final int length) {
        return LongBinding.entryToUnsignedLong(this, length);
    }
}
