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

package com.mongodb.acceptancetest.querying;

import com.mongodb.MongoCursor;
import com.mongodb.client.DatabaseTestCase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.QueryBuilder;
import com.mongodb.codecs.CollectibleCodec;
import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.QueryOperators.TYPE;
import static com.mongodb.client.QueryBuilder.query;
import static java.util.Arrays.asList;
import static org.bson.BsonType.INT32;
import static org.bson.BsonType.INT64;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class QueryAcceptanceTest extends DatabaseTestCase {
    @Test
    public void shouldBeAbleToQueryWithDocumentSpecification() {
        collection.insert(new Document("name", "Bob"));

        Document query = new Document("name", "Bob");
        MongoCursor<Document> results = collection.find(query).get();

        assertThat(results.next().get("name").toString(), is("Bob"));
    }

    @Test
    public void shouldBeAbleToQueryWithDocument() {
        collection.insert(new Document("name", "Bob"));

        Document query = new Document("name", "Bob");
        MongoCursor<Document> results = collection.find(query).get();

        assertThat(results.next().get("name").toString(), is("Bob"));
    }

    @Test
    public void shouldBeAbleToQueryTypedCollectionWithDocument() {
        MongoCollection<Person> personCollection = database.getCollection(getCollectionName(), new PersonCodec());
        personCollection.insert(new Person("Bob"));

        MongoCursor<Person> results = personCollection.find(new Document("name", "Bob")).get();

        assertThat(results.next().name, is("Bob"));
    }

    @Test
    public void shouldBeAbleToFilterByType() {
        collection.insert(new Document("product", "Book").append("numTimesOrdered", "some"));
        collection.insert(new Document("product", "CD").append("numTimesOrdered", "6"));
        collection.insert(new Document("product", "DVD").append("numTimesOrdered", 9));
        collection.insert(new Document("product", "SomethingElse").append("numTimesOrdered", 10));

        List<Document> results = new ArrayList<Document>();
        collection.find(new Document("numTimesOrdered", new Document("$type", 16)))
                  .sort(new Document("numTimesOrdered", -1))
                  .into(results);

        assertThat(results.size(), is(2));
        assertThat(results.get(0).get("product").toString(), is("SomethingElse"));
        assertThat(results.get(1).get("product").toString(), is("DVD"));
    }

    @Test
    public void shouldUseFriendlyQueryType() {
        collection.insert(new Document("product", "Book").append("numTimesOrdered", "some"));
        collection.insert(new Document("product", "CD").append("numTimesOrdered", "6"));
        collection.insert(new Document("product", "DVD").append("numTimesOrdered", 9));
        collection.insert(new Document("product", "SomethingElse").append("numTimesOrdered", 10));
        collection.insert(new Document("product", "VeryPopular").append("numTimesOrdered", 7843273657286478L));

        List<Document> results = new ArrayList<Document>();
        //TODO make BSON type serializable
        Document filter = new Document("$or", asList(new Document("numTimesOrdered", new Document("$type", INT32.getValue())),
                                                     new Document("numTimesOrdered", new Document("$type", INT64.getValue()))));
        collection.find(filter)
                  .sort(new Document("numTimesOrdered", -1))
                  .into(results);

        assertThat(results.size(), is(3));
        assertThat(results.get(0).get("product").toString(), is("VeryPopular"));
        assertThat(results.get(1).get("product").toString(), is("SomethingElse"));
        assertThat(results.get(2).get("product").toString(), is("DVD"));
    }

    @Test
    public void shouldBeAbleToSortAscending() {
        collection.insert(new Document("product", "Book"));
        collection.insert(new Document("product", "DVD"));
        collection.insert(new Document("product", "CD"));

        List<Document> results = new ArrayList<Document>();
        collection.find().sort(new Document("product", 1))
                  .into(results);

        assertThat(results.size(), is(3));
        assertThat(results.get(0).get("product").toString(), is("Book"));
        assertThat(results.get(1).get("product").toString(), is("CD"));
        assertThat(results.get(2).get("product").toString(), is("DVD"));
    }

    @Test
    public void shouldBeAbleToUseQueryBuilderForFilter() {
        collection.insert(new Document("product", "Book").append("numTimesOrdered", "some"));
        collection.insert(new Document("product", "CD").append("numTimesOrdered", "6"));
        collection.insert(new Document("product", "DVD").append("numTimesOrdered", 9));
        collection.insert(new Document("product", "SomethingElse").append("numTimesOrdered", 10));
        collection.insert(new Document("product", "VeryPopular").append("numTimesOrdered", 7843273657286478L));

        List<Document> results = new ArrayList<Document>();

        Document filter = new QueryBuilder().or(query("numTimesOrdered").is(query(TYPE).is(INT32.getValue())))
                                            .or(query("numTimesOrdered").is(query(TYPE).is(INT64.getValue())))
                                            .toDocument();
        collection.find(filter)
                  .sort(new Document("numTimesOrdered", -1))
                  .into(results);

        assertThat(results.size(), is(3));
        assertThat(results.get(0).get("product").toString(), is("VeryPopular"));
        assertThat(results.get(1).get("product").toString(), is("SomethingElse"));
        assertThat(results.get(2).get("product").toString(), is("DVD"));
    }


    @Test
    @Ignore("JSON stuff not implemented")
    public void shouldBeAbleToQueryWithJSON() {
    }

    private class PersonCodec implements CollectibleCodec<Person> {
        @Override
        public boolean documentHasId(final Person document) {
            return true;
        }

        @Override
        public BsonObjectId getDocumentId(final Person document) {
            return new BsonObjectId(document.id);
        }

        @Override
        public void generateIdIfAbsentFromDocument(final Person person) {

        }

        @Override
        public void encode(final BsonWriter writer, final Person value, final EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeObjectId("_id", value.id);
            writer.writeString("name", value.name);
            writer.writeEndDocument();
        }

        @Override
        public Person decode(final BsonReader reader, final DecoderContext decoderContext) {
            reader.readStartDocument();
            ObjectId id = reader.readObjectId("_id");
            String name = reader.readString("name");
            reader.readEndDocument();
            return new Person(id, name);
        }

        @Override
        public Class<Person> getEncoderClass() {
            return Person.class;
        }
    }

    private class Person {
        private ObjectId id = new ObjectId();
        private final String name;

        public Person(final String name) {
            this.name = name;
        }

        public Person(final ObjectId id, final String name) {
            this.id = id;
            this.name = name;
        }
    }

}
