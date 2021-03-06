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

import com.mongodb.Function;
import com.mongodb.MongoCursor;
import com.mongodb.MongoNamespace;
import com.mongodb.async.MongoAsyncCursor;
import com.mongodb.async.MongoFuture;
import com.mongodb.binding.AsyncReadBinding;
import com.mongodb.binding.ReadBinding;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.codecs.Decoder;
import org.mongodb.CommandResult;

import static com.mongodb.assertions.Assertions.notNull;
import static com.mongodb.operation.CommandOperationHelper.executeWrappedCommandProtocol;
import static com.mongodb.operation.CommandOperationHelper.executeWrappedCommandProtocolAsync;

/**
 * Groups documents in a collection by the specified key and performs simple aggregation functions, such as computing counts and sums. The
 * command is analogous to a SELECT <...> GROUP BY statement in SQL.
 *
 * @param <T> the type for each document
 *
 * @mongodb.driver.manual reference/command/group Group Command
 * @since 3.0
 */
public class GroupOperation<T> implements AsyncReadOperation<MongoAsyncCursor<T>>, ReadOperation<MongoCursor<T>> {
    private final MongoNamespace namespace;
    private final Group group;
    private final Decoder<T> decoder;

    /**
     * Create an operation that will perform a Group on a given collection.
     *
     * @param namespace the database and collection to run the operation against
     * @param group     contains all the arguments for this group command
     */
    public GroupOperation(final MongoNamespace namespace, final Group group, final Decoder<T> decoder) {
        this.namespace = notNull("namespace", namespace);
        this.group = notNull("group", group);
        this.decoder = notNull("decoder", decoder);
    }

    /**
     * Will return a cursor of Documents containing the results of the group operation.
     *
     * @param binding the binding
     * @return a MongoCursor of T, the results of the group operation in a form to be iterated over
     */
    @Override
    @SuppressWarnings("unchecked")
    public MongoCursor<T> execute(final ReadBinding binding) {
        return executeWrappedCommandProtocol(namespace, getCommand(), CommandResultDocumentCodec.create(decoder, "retval"), binding,
                                             transformer());
    }

    /**
     * Will return a cursor of Documents containing the results of the group operation.
     *
     * @param binding the binding
     * @return a Future MongoCursor of T, the results of the group operation in a form to be iterated over
     */
    @Override
    @SuppressWarnings("unchecked")
    public MongoFuture<MongoAsyncCursor<T>> executeAsync(final AsyncReadBinding binding) {
        return executeWrappedCommandProtocolAsync(namespace, getCommand(), CommandResultDocumentCodec.create(decoder, "retval"), binding,
                                                  asyncTransformer());
    }

    private Function<CommandResult, MongoCursor<T>> transformer() {
        return new Function<CommandResult, MongoCursor<T>>() {
            @SuppressWarnings("unchecked")
            @Override
            public MongoCursor<T> apply(final CommandResult result) {
                return new InlineMongoCursor<T>(result.getAddress(),
                                                BsonDocumentWrapperHelper.<T>toList(result.getResponse().getArray("retval")));
            }
        };
    }

    private Function<CommandResult, MongoAsyncCursor<T>> asyncTransformer() {
        return new Function<CommandResult, MongoAsyncCursor<T>>() {
            @SuppressWarnings("unchecked")
            @Override
            public MongoAsyncCursor<T> apply(final CommandResult result) {
                return new InlineMongoAsyncCursor<T>(BsonDocumentWrapperHelper.<T>toList(result.getResponse().getArray("retval")));
            }
        };
    }


    private BsonDocument getCommand() {

        BsonDocument document = new BsonDocument("ns", new BsonString(namespace.getCollectionName()));

        if (group.getKey() != null) {
            document.put("key", group.getKey());
        } else if (group.getKeyFunction() != null) {
            document.put("keyf", group.getKeyFunction());
        }

        document.put("initial", group.getInitial());
        document.put("$reduce", group.getReduceFunction());

        if (group.getFinalizeFunction() != null) {
            document.put("finalize", group.getFinalizeFunction());
        }

        if (group.getFilter() != null) {
            document.put("cond", group.getFilter());
        }

        return new BsonDocument("group", document);
    }
}
