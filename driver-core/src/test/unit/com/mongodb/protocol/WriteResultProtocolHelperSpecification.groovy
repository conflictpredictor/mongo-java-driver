/*
 * Copyright (c) 2008 - 2014 MongoDB Inc. <http://mongodb.com>
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

package com.mongodb.protocol

import com.mongodb.CommandFailureException
import com.mongodb.MongoException
import com.mongodb.ServerAddress
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonObjectId
import org.bson.BsonString
import org.bson.types.ObjectId
import org.mongodb.CommandResult
import spock.lang.Specification

import static java.util.Arrays.asList

class WriteResultProtocolHelperSpecification extends Specification {

    def 'should return a write result for an insert'() {
        given:
        def commandResult = new CommandResult(new ServerAddress(),
                                              new BsonDocument('ok', new BsonInt32(1)).append('n', new BsonInt32(0))
        );
        when:
        def writeResult = ProtocolHelper.getWriteResult(commandResult)

        then:
        writeResult == new AcknowledgedWriteResult(0, false, null)

    }

    def 'should return a write result for an upsert'() {
        given:
        def id = new ObjectId()
        def commandResult = new CommandResult(new ServerAddress(),
                                              new BsonDocument('ok', new BsonInt32(1)).append('n', new BsonInt32(1))
                                                                                      .append('updatedExisting', BsonBoolean.FALSE).
                                                      append('upserted', new BsonObjectId(id))
        );
        when:
        def writeResult = ProtocolHelper.getWriteResult(commandResult)

        then:
        writeResult == new AcknowledgedWriteResult(1, false, new BsonObjectId(id))

    }

    def 'should throw command failure if result is not ok'() {
        given:
        def commandResult = new CommandResult(new ServerAddress(),
                                              new BsonDocument('ok', new BsonInt32(0))
                                                      .append('errmsg', new BsonString('Something is very wrong'))
                                                      .append('code', new BsonInt32(14))
        );
        when:
        ProtocolHelper.getWriteResult(commandResult)

        then:
        def e = thrown(CommandFailureException)
        e.getCode() == 14
    }

    def 'should throw duplicate key when response has a duplicate key error code'() {
        given:
        def commandResult = new CommandResult(new ServerAddress(),
                                              new BsonDocument('ok', new BsonInt32(1))
                                                      .append('err', new BsonString('E11000 duplicate key error index 1'))
                                                      .append('code', new BsonInt32(11000))
        );

        when:
        ProtocolHelper.getWriteResult(commandResult)

        then:
        def e = thrown(MongoException.DuplicateKey)
        e.getCode() == 11000
    }

    def 'should throw duplicate key when errObjects has a duplicate key error code'() {
        given:
        def commandResult =
                new CommandResult(new ServerAddress(),
                                  new BsonDocument('ok', new BsonInt32(1))
                                          .append('err', new BsonString('E11000 duplicate key error index 1'))
                                          .append('errObjects',
                                                  new BsonArray(asList(new BsonDocument('ok', new BsonInt32(1))
                                                                               .append('err', new BsonString('E11000 duplicate key error ' +
                                                                                                             'index 1'))
                                                                               .append('code', new BsonInt32(11000)),
                                                                       new BsonDocument('ok', new BsonInt32(1))
                                                                               .append('err', new BsonString('E11000 duplicate key error ' +
                                                                                                             'index 2'))
                                                                               .append('code', new BsonInt32(11000)))))
                );

        when:
        ProtocolHelper.getWriteResult(commandResult)

        then:
        def e = thrown(MongoException.DuplicateKey)
        e.getCode() == 11000
    }
}
