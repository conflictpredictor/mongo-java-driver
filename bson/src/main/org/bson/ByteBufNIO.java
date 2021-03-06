/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Implementation of {@code ByteBuf} which simply wraps an NIO {@code ByteBuffer} and forwards all calls to it.
 *
 * @since 3.0
 */
public class ByteBufNIO implements ByteBuf {
    private ByteBuffer buf;

    public ByteBufNIO(final ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public void close() {
        buf = null;
    }

    @Override
    public int capacity() {
        return buf.capacity();
    }

    @Override
    public ByteBuf put(final int index, final byte b) {
        buf.put(index, b);
        return this;
    }

    @Override
    public int remaining() {
        return buf.remaining();
    }

    @Override
    public ByteBuf put(final byte[] src, final int offset, final int length) {
        buf.put(src, offset, length);
        return this;
    }

    @Override
    public boolean hasRemaining() {
        return buf.hasRemaining();
    }

    @Override
    public ByteBuf put(final byte b) {
        buf.put(b);
        return this;
    }

    @Override
    public ByteBuf flip() {
        buf.flip();
        return this;
    }

    @Override
    public byte[] array() {
        return buf.array();
    }

    @Override
    public int limit() {
        return buf.limit();
    }

    @Override
    public ByteBuf position(final int newPosition) {
        buf.position(newPosition);
        return this;
    }

    @Override
    public ByteBuf clear() {
        buf.clear();
        return this;
    }

    @Override
    public ByteBuf order(final ByteOrder byteOrder) {
        buf.order(byteOrder);
        return this;
    }

    @Override
    public byte get() {
        return buf.get();
    }

    @Override
    public ByteBuf get(final byte[] bytes) {
        buf.get(bytes);
        return this;
    }

    @Override
    public long getLong() {
        return buf.getLong();
    }

    @Override
    public double getDouble() {
        return buf.getDouble();
    }

    @Override
    public int getInt() {
        return buf.getInt();
    }

    @Override
    public int position() {
        return buf.position();
    }

    @Override
    public ByteBuf limit(final int newLimit) {
        buf.limit(newLimit);
        return this;
    }

    @Override
    public ByteBuf asReadOnly() {
        return new ByteBufNIO(buf.asReadOnlyBuffer());
    }

    @Override
    public ByteBuf duplicate() {
        return new ByteBufNIO(buf.duplicate());
    }

    @Override
    public ByteBuffer asNIO() {
        return buf;
    }
}

