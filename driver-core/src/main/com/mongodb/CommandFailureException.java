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

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonWriter;

import java.io.StringWriter;

import static java.lang.String.format;

/**
 * An exception indicating that a command sent to a MongoDB server returned a failure.
 */
public class CommandFailureException extends MongoServerException {
    private static final long serialVersionUID = -1180715413196161037L;
    private final BsonDocument response;

    /**
     * Construct a new instance with the CommandResult from a failed command
     *
     * @param response the command response
     * @param address the address of the server that generated the response
     */
    public CommandFailureException(final BsonDocument response, final ServerAddress address) {
        super(format("Command failed with error %s: '%s' on server %s. The full response is %s", extractErrorCode(response),
                     extractErrorMessage(response), address, getResponseAsJson(response)), address);
        this.response = response;
    }

    @Override
    public int getErrorCode() {
        return extractErrorCode(response);
    }

    @Override
    public String getErrorMessage() {
        return extractErrorMessage(response);
    }

    /**
     * For internal use only.
     *
     * @return the full response to the command failure.
     */
    public BsonDocument getResponse() {
        return response;
    }

    private static String getResponseAsJson(final BsonDocument commandResponse) {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        new BsonDocumentCodec().encode(jsonWriter, commandResponse, EncoderContext.builder().build());
        return writer.toString();
    }

    private static int extractErrorCode(final BsonDocument response) {
        if (response.containsKey("code")) {
            return ((BsonInt32) response.get("code")).getValue();
        } else {
            return -1;
        }
    }

    private static String extractErrorMessage(final BsonDocument response) {
        if (response.containsKey("errmsg")) {
            return ((BsonString) response.get("errmsg")).getValue();
        } else {
            return null;
        }
    }

}
