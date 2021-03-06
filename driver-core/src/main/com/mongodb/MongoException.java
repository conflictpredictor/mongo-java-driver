/*
 * Copyright (c) 2008 - 2014 MongoDB, Inc.
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


import org.bson.BsonDocument;

/**
 * Top level Exception for all Exceptions, server-side or client-side, that come from the driver.
 */
public class MongoException extends RuntimeException {
    private static final long serialVersionUID = -4415279469780082174L;

    private final int code;

    /**
     * @param msg the message
     */
    public MongoException(final String msg) {
        super(msg);
        code = -3;
    }

    /**
     * @param code the error code
     * @param msg  the message
     */
    public MongoException(final int code, final String msg) {
        super(msg);
        this.code = code;
    }

    /**
     * @param msg the message
     * @param t   the throwable cause
     */
    public MongoException(final String msg, final Throwable t) {
        super(msg, t);
        code = -4;
    }

    /**
     * @param code the error code
     * @param msg  the message
     * @param t    the throwable cause
     */
    public MongoException(final int code, final String msg, final Throwable t) {
        super(msg, t);
        this.code = code;
    }

    /**
     * Gets the exception code
     *
     * @return the error code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Subclass of WriteConcernException representing a duplicate key error.
     *
     * @deprecated This class will be dropped in 3.x versions. Please catch {@link DuplicateKeyException} instead.
     */
    @Deprecated
    public static class DuplicateKey extends DuplicateKeyException {

        private static final long serialVersionUID = 6557680785576001838L;

        public DuplicateKey(final BsonDocument response, final ServerAddress address, final WriteResult writeResult) {
            super(response, address, writeResult);
        }
    }
}
