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
import com.mongodb.protocol.DeleteProtocol;
import org.bson.BsonDocument;
import org.bson.BsonString;
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
import static java.util.Arrays.asList;

/**
 * An operation to remove a user.
 *
 * @since 3.0
 */
public class DropUserOperation implements AsyncWriteOperation<Void>, WriteOperation<Void> {
    private final String database;
    private final String userName;

    public DropUserOperation(final String source, final String userName) {
        this.database = notNull("source", source);
        this.userName = notNull("userName", userName);
    }

    @Override
    public Void execute(final WriteBinding binding) {
        return withConnection(binding, new CallableWithConnection<Void>() {
            @Override
            public Void call(final Connection connection) {
                if (serverIsAtLeastVersionTwoDotSix(connection)) {
                    executeWrappedCommandProtocol(database, getCommand(), connection);
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
                    return executeWrappedCommandProtocolAsync(database, getCommand(), connection, new VoidTransformer<CommandResult>());
                } else {
                    return executeProtocolAsync(getCollectionBasedProtocol(), connection, new VoidTransformer<WriteResult>());
                }
            }
        });
    }

    private DeleteProtocol getCollectionBasedProtocol() {
        MongoNamespace namespace = new MongoNamespace(database, "system.users");
        return new DeleteProtocol(namespace, true, WriteConcern.ACKNOWLEDGED,
                                  asList(new RemoveRequest(new BsonDocument("user", new BsonString(userName))))
        );
    }

    private BsonDocument getCommand() {
        return new BsonDocument("dropUser", new BsonString(userName));
    }
}
