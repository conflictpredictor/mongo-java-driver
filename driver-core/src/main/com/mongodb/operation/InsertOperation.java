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

package com.mongodb.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import com.mongodb.codecs.CollectibleCodec;
import com.mongodb.protocol.InsertCommandProtocol;
import com.mongodb.protocol.InsertProtocol;
import com.mongodb.protocol.WriteCommandProtocol;
import com.mongodb.protocol.WriteProtocol;
import org.bson.codecs.Encoder;
import org.mongodb.BulkWriteResult;

import java.util.List;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * An operation that inserts one or more documents into a collection.
 *
 * @param <T> the document type
 *
 * @since 3.0
 */
public class InsertOperation<T> extends BaseWriteOperation {
    private final List<InsertRequest<T>> insertRequestList;
    private final Encoder<T> encoder;

    public InsertOperation(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                           final List<InsertRequest<T>> insertRequestList, final Encoder<T> encoder) {
        super(namespace, ordered, writeConcern);
        this.insertRequestList = notNull("insertList", insertRequestList);
        this.encoder = notNull("encoder", encoder);
        if (encoder instanceof CollectibleCodec) {
            for (InsertRequest<T> cur : insertRequestList) {
                ((CollectibleCodec<T>) encoder).generateIdIfAbsentFromDocument(cur.getDocument());
            }
        }
    }

    @Override
    protected WriteProtocol getWriteProtocol() {
        return new InsertProtocol<T>(getNamespace(), isOrdered(), getWriteConcern(), insertRequestList, encoder);
    }

    @Override
    protected WriteCommandProtocol getCommandProtocol() {
        return new InsertCommandProtocol<T>(getNamespace(), isOrdered(), getWriteConcern(), insertRequestList, encoder);
    }

    @Override
    protected WriteRequest.Type getType() {
        return WriteRequest.Type.INSERT;
    }

    @Override
    protected int getCount(final BulkWriteResult bulkWriteResult) {
        return 0;
    }
}
