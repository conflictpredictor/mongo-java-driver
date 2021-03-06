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

package com.mongodb.codecs;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.ByteBufNIO;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicInputBuffer;
import org.bson.io.BasicOutputBuffer;
import org.mongodb.Document;

import java.nio.ByteBuffer;

public final class CodecTestUtil {
    static BsonBinaryReader prepareReaderWithObjectToBeDecoded(final Object objectToDecode) {
        //Need to encode it wrapped in a document to conform to the validation
        Document document = new Document("wrapperDocument", objectToDecode);

        BasicOutputBuffer outputBuffer = new BasicOutputBuffer();

        BsonBinaryWriter writer = new BsonBinaryWriter(outputBuffer, true);
        byte[] documentAsByteArrayForReader;
        try {
            new DocumentCodec().encode(writer, document, EncoderContext.builder().build());
            documentAsByteArrayForReader = outputBuffer.toByteArray();
        } finally {
            writer.close();
        }

        BsonBinaryReader reader = new BsonBinaryReader(new BasicInputBuffer(new ByteBufNIO(ByteBuffer.wrap(documentAsByteArrayForReader))),
                                                       false);

        //have to read off the wrapper document so the reader is in the correct position for the test
        reader.readStartDocument();
        reader.readName();
        return reader;
    }

    static <T> BsonBinaryReader prepareReaderWithObjectToBeDecoded(final T objectToDecode, final Codec<T> codec) {
        BasicOutputBuffer outputBuffer = new BasicOutputBuffer();

        BsonBinaryWriter writer = new BsonBinaryWriter(outputBuffer, true);
        byte[] documentAsByteArrayForReader;
        try {
            codec.encode(writer, objectToDecode, EncoderContext.builder().build());
            documentAsByteArrayForReader = outputBuffer.toByteArray();
        } finally {
            writer.close();
        }

        return new BsonBinaryReader(new BasicInputBuffer(new ByteBufNIO(ByteBuffer.wrap(documentAsByteArrayForReader))), false);
    }

    private CodecTestUtil() { }
}
