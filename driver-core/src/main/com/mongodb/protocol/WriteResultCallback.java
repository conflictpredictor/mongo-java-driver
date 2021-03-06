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

package com.mongodb.protocol;

import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import com.mongodb.async.MongoFuture;
import com.mongodb.connection.Connection;
import com.mongodb.async.SingleResultFuture;
import com.mongodb.operation.SingleResultFutureCallback;
import com.mongodb.protocol.message.RequestMessage;
import org.bson.BsonDocument;
import org.bson.codecs.Decoder;
import org.mongodb.CommandResult;
import org.mongodb.WriteResult;

class WriteResultCallback extends CommandResultBaseCallback {
    private final SingleResultFuture<WriteResult> future;
    private final MongoNamespace namespace;
    private final RequestMessage nextMessage; // only used for batch inserts that need to be split into multiple messages
    private boolean ordered;
    private final WriteConcern writeConcern;
    private final Connection connection;

    // CHECKSTYLE:OFF
    public WriteResultCallback(final SingleResultFuture<WriteResult> future, final Decoder<BsonDocument> decoder,
                               final MongoNamespace namespace, final RequestMessage nextMessage,
                               final boolean ordered, final WriteConcern writeConcern, final long requestId,
                               final Connection connection) {
        // CHECKSTYLE:ON
        super(decoder, requestId, connection.getServerAddress());
        this.future = future;
        this.namespace = namespace;
        this.nextMessage = nextMessage;
        this.ordered = ordered;
        this.writeConcern = writeConcern;
        this.connection = connection;
    }

    @Override
    protected boolean callCallback(final CommandResult commandResult, final MongoException e) {
        boolean done = true;
        if (e != null) {
            future.init(null, e);
        } else {
            try {
                WriteResult writeResult = ProtocolHelper.getWriteResult(commandResult);
                if (nextMessage != null) {
                    MongoFuture<WriteResult> newFuture = new GenericWriteProtocol(namespace, nextMessage, ordered, writeConcern)
                                                         .executeAsync(connection);
                    newFuture.register(new SingleResultFutureCallback<WriteResult>(future));
                    done = false;
                } else {
                    future.init(writeResult, null);
                }
            } catch (MongoException writeException) {
                future.init(null, writeException);
            }
        }
        return done;
    }
}
