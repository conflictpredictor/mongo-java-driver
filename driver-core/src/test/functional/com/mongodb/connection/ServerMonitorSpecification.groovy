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

package com.mongodb.connection
import com.mongodb.MongoException
import com.mongodb.OperationFunctionalSpecification
import com.mongodb.ServerAddress
import com.mongodb.operation.CommandReadOperation
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.mongodb.CommandResult
import spock.lang.IgnoreIf

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.mongodb.ClusterFixture.getBinding
import static com.mongodb.ClusterFixture.getCredentialList
import static com.mongodb.ClusterFixture.getPrimary
import static com.mongodb.ClusterFixture.getSSLSettings
import static com.mongodb.ClusterFixture.serverVersionAtLeast
import static com.mongodb.connection.ServerMonitor.exceptionHasChanged
import static com.mongodb.connection.ServerMonitor.stateHasChanged
import static java.util.Arrays.asList

class ServerMonitorSpecification extends OperationFunctionalSpecification {
    ServerDescription newDescription
    ServerMonitor serverMonitor
    CountDownLatch latch = new CountDownLatch(1)

    def setup() {
        serverMonitor = new ServerMonitor(getPrimary(), ServerSettings.builder().build(), 'cluster-1',
                                                new ChangeListener<ServerDescription>() {
                                                    @Override
                                                    void stateChanged(final ChangeEvent<ServerDescription> event) {
                                                        newDescription = event.newValue
                                                        latch.countDown()
                                                    }
                                                },
                                                new InternalStreamConnectionFactory('1',
                                                                                    new SocketStreamFactory(SocketSettings.builder()
                                                                                                                          .build(),
                                                                                                            getSSLSettings()),
                                                                                    getCredentialList(),
                                                                                    new NoOpConnectionListener()),
                                                new TestConnectionPool())
        serverMonitor.start()
    }

    def cleanup() {
        serverMonitor.close();
    }

    def 'should have positive round trip time'() {
        when:
        latch.await()

        then:
        newDescription.roundTripTimeNanos > 0
    }

    def 'should return server version'() {
        given:
        CommandResult commandResult = new CommandReadOperation('admin', new BsonDocument('buildinfo', new BsonInt32(1)))
                .execute(getBinding())
        def expectedVersion = new ServerVersion(commandResult.getResponse().getArray('versionArray')
                                                             .subList(0, 3)*.getValue() as List<Integer>)

        when:
        latch.await()

        then:
        newDescription.version == expectedVersion

        cleanup:
        serverMonitor.close()
    }

    @IgnoreIf( { !serverVersionAtLeast(asList(2, 6, 0)) } )
    def 'should set max wire batch size when provided by server'() {
        given:
        CommandResult commandResult = new CommandReadOperation('admin', new BsonDocument('ismaster', new BsonInt32(1)))
                .execute(getBinding())
        def expected = commandResult.response.getInt32('maxWriteBatchSize').getValue()

        when:
        latch.await()

        then:
        newDescription.maxWriteBatchSize == expected
    }

    @IgnoreIf( { serverVersionAtLeast(asList(2, 6, 0)) } )
    def 'should set default max wire batch size when not provided by server'() {
        when:
        latch.await()

        then:
        newDescription.maxWriteBatchSize == ServerDescription.getDefaultMaxWriteBatchSize()
    }

    def 'should report exception has changed when the current and previous are different'() {
        expect:
        exceptionHasChanged(null, new NullPointerException())
        exceptionHasChanged(new NullPointerException(), null)
        exceptionHasChanged(new SocketException(), new SocketException('A message'))
        exceptionHasChanged(new SocketException('A message'), new SocketException())
        exceptionHasChanged(new SocketException('A message'), new MongoException('A message'))
        exceptionHasChanged(new SocketException('A message'), new SocketException('A different message'))
    }

    def 'should report exception has not changed when the current and previous are the same'() {
        expect:
        !exceptionHasChanged(null, null)
        !exceptionHasChanged(new NullPointerException(), new NullPointerException())
        !exceptionHasChanged(new MongoException('A message'), new MongoException('A message'))
    }

    def 'should report state has changed if descriptions are different'() {
        expect:
        stateHasChanged(ServerDescription.builder()
                                         .type(ServerType.UNKNOWN)
                                         .state(ServerConnectionState.CONNECTING)
                                         .address(new ServerAddress())
                                         .build(),
                        ServerDescription.builder()
                                         .type(ServerType.STANDALONE)
                                         .state(ServerConnectionState.CONNECTED)
                                         .address(new ServerAddress())
                                         .roundTripTime(5, TimeUnit.MILLISECONDS)
                                         .build());
    }

    def 'should report state has changed if latencies are different'() {
        expect:
        stateHasChanged(ServerDescription.builder()
                                         .type(ServerType.STANDALONE)
                                         .state(ServerConnectionState.CONNECTED)
                                         .address(new ServerAddress())
                                         .roundTripTime(5, TimeUnit.MILLISECONDS)
                                         .build(),
                        ServerDescription.builder()
                                         .type(ServerType.STANDALONE)
                                         .state(ServerConnectionState.CONNECTED)
                                         .address(new ServerAddress())
                                         .roundTripTime(6, TimeUnit.MILLISECONDS)
                                         .build());
    }

    def 'should report state has not changed if descriptions and latencies are the same'() {
        expect:
        !stateHasChanged(ServerDescription.builder()
                                          .type(ServerType.STANDALONE)
                                          .state(ServerConnectionState.CONNECTED)
                                          .address(new ServerAddress())
                                          .roundTripTime(5, TimeUnit.MILLISECONDS)
                                          .build(),
                         ServerDescription.builder()
                                          .type(ServerType.STANDALONE)
                                          .state(ServerConnectionState.CONNECTED)
                                          .address(new ServerAddress())
                                          .roundTripTime(5, TimeUnit.MILLISECONDS)
                                          .build());
    }
}