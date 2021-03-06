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

package com.mongodb;

import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * A bulk write operation.  A bulk write operation consists of an ordered or unordered collection of write requests,
 * which can be any combination of inserts, updates, replaces, or removes.
 *
 * @see DBCollection#initializeOrderedBulkOperation()
 * @see com.mongodb.DBCollection#initializeUnorderedBulkOperation()
 *
 * @since 2.12
 */
public class BulkWriteOperation {
    private static final String ID_FIELD_NAME = "_id";
    private final boolean ordered;
    private final DBCollection collection;
    private final List<WriteRequest> requests = new ArrayList<WriteRequest>();

    BulkWriteOperation(final boolean ordered, final DBCollection collection) {
        this.ordered = ordered;
        this.collection = collection;
    }

    /**
     * Returns true if this is building an ordered bulk write request.
     *
     * @return whether this is building an ordered bulk write operation
     * @see DBCollection#initializeOrderedBulkOperation()
     * @see DBCollection#initializeUnorderedBulkOperation()
     */
    public boolean isOrdered() {
        return ordered;
    }

    /**
     * Add an insert request to the bulk operation
     *
     * @param document the document to insert
     */
    public void insert(final DBObject document) {
        if (document.get(ID_FIELD_NAME) == null) {
            document.put(ID_FIELD_NAME, new ObjectId());
        }
        addRequest(new InsertRequest(document));
    }

    /**
     * Start building a write request to add to the bulk write operation.  The returned builder can be used to create an update, replace,
     * or remove request with the given query.
     *
     * @param query the query for an update, replace or remove request
     * @return a builder for a single write request
     */
    public BulkWriteRequestBuilder find(final DBObject query) {
        return new BulkWriteRequestBuilder(this, query, collection.getDefaultDBObjectCodec());
    }

    /**
     * Execute the bulk write operation with the default write concern of the collection from which this came.  Note that the
     * continueOnError property of the write concern is ignored.
     *
     * @return the result of the bulk write operation.
     * @throws com.mongodb.BulkWriteException
     * @throws com.mongodb.MongoException
     */
    public BulkWriteResult execute() {
        return collection.executeBulkWriteOperation(ordered, requests);
    }

    /**
     * Execute the bulk write operation with the given write concern.  Note that the continueOnError property of the write concern is
     * ignored.
     *
     * @param writeConcern the write concern to apply to the bulk operation.
     * @return the result of the bulk write operation.
     * @throws com.mongodb.BulkWriteException
     * @throws com.mongodb.MongoException
     */
    public BulkWriteResult execute(final WriteConcern writeConcern) {
        return collection.executeBulkWriteOperation(ordered, requests, writeConcern);
    }

    void addRequest(final WriteRequest request) {
        requests.add(request);
    }
}
