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

package org.bson.codecs.configuration;


import org.bson.codecs.Codec;

// An implementation of CodecRegistry that is used to detect cyclic dependencies between Codecs
class ChildCodecRegistry<T> implements CodecRegistry {

    @SuppressWarnings("rawtypes")
    private final ChildCodecRegistry parent;
    private final RootCodecRegistry registry;
    private final Class<T> codecClass;

    ChildCodecRegistry(final RootCodecRegistry registry, final Class<T> codecClass) {
        this.codecClass = codecClass;
        this.parent = null;
        this.registry = registry;
    }

    @SuppressWarnings("rawtypes")
    private ChildCodecRegistry(final ChildCodecRegistry parent, final Class<T> codecClass) {
        this.parent = parent;
        this.codecClass = codecClass;
        this.registry = parent.registry;
    }

    public Class<T> getCodecClass() {
        return codecClass;
    }

    // Gets a Codec, but if it detects a cyclic dependency, return a LazyCodec which breaks the chain.
    public <U> Codec<U> get(final Class<U> clazz) {
        if (hasCycles(clazz)) {
            return new LazyCodec<U>(registry, clazz);
        } else {
            return registry.get(new ChildCodecRegistry<U>(this, clazz));
        }
    }

    @SuppressWarnings("rawtypes")
    private <U> Boolean hasCycles(final Class<U> theClass) {
        ChildCodecRegistry current = this;
        while (current != null) {
            if (current.codecClass.equals(theClass)) {
                return true;
            }

            current = current.parent;
        }

        return false;
    }
}