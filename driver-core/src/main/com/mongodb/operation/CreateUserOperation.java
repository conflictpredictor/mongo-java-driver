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
import com.mongodb.async.MongoFuture;
import com.mongodb.binding.AsyncWriteBinding;
import com.mongodb.binding.WriteBinding;
import com.mongodb.connection.Connection;
import com.mongodb.protocol.InsertProtocol;
import com.mongodb.protocol.Protocol;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.mongodb.CommandResult;
import org.mongodb.WriteResult;

import static com.mongodb.assertions.Assertions.notNull;
import static com.mongodb.operation.CommandOperationHelper.executeWrappedCommandProtocol;
import static com.mongodb.operation.CommandOperationHelper.executeWrappedCommandProtocolAsync;
import static com.mongodb.operation.OperationHelper.AsyncCallableWithConnection;
import static com.mongodb.operation.OperationHelper.CallableWithConnection;
import static com.mongodb.operation.OperationHelper.VoidTransformer;
import static com.mongodb.operation.OperationHelper.executeProtocolAsync;
import static com.mongodb.operation.OperationHelper.serverIsAtLeastVersionTwoDotSix;
import static com.mongodb.operation.OperationHelper.withConnection;
import static com.mongodb.operation.UserOperationHelper.asCollectionDocument;
import static com.mongodb.operation.UserOperationHelper.asCommandDocument;
import static java.util.Arrays.asList;

/**
 * An operation to create a user.
 *
 * @since 3.0
 */
public class CreateUserOperation implements AsyncWriteOperation<Void>, WriteOperation<Void> {
    private final User user;

    public CreateUserOperation(final User user) {
        this.user = notNull("user", user);
    }

    @Override
    public Void execute(final WriteBinding binding) {
        return withConnection(binding, new CallableWithConnection<Void>() {
            @Override
            public Void call(final Connection connection) {
                if (serverIsAtLeastVersionTwoDotSix(connection)) {
                    executeWrappedCommandProtocol(user.getCredential().getSource(), getCommand(), connection);
                } else {
                    getCollectionBasedProtocol().execute(connection);
                }
                return null;
            }
        });
    }

    @Override
    public MongoFuture<Void> executeAsync(final AsyncWriteBinding binding) {
        return withConnection(binding, new AsyncCallableWithConnection<Void>() {
            @Override
            public MongoFuture<Void> call(final Connection connection) {
                if (serverIsAtLeastVersionTwoDotSix(connection)) {
                    return executeWrappedCommandProtocolAsync(user.getCredential().getSource(), getCommand(), connection,
                                                              new VoidTransformer<CommandResult>());
                } else {
                    return executeProtocolAsync(getCollectionBasedProtocol(), connection, new VoidTransformer<WriteResult>());
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Protocol<WriteResult> getCollectionBasedProtocol() {
        MongoNamespace namespace = new MongoNamespace(user.getCredential().getSource(), "system.users");
        return new InsertProtocol<BsonDocument>(namespace, true, WriteConcern.ACKNOWLEDGED,
                                                asList(new InsertRequest<BsonDocument>(asCollectionDocument(user))),
                                                new BsonDocumentCodec());
    }

    private BsonDocument getCommand() {
        return asCommandDocument(user, "createUser");
    }
}
