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

import com.mongodb.ConnectionString
import com.mongodb.ServerAddress
import com.mongodb.selector.PrimaryServerSelector
import spock.lang.Specification

class ClusterSettingsSpecification extends Specification {
    def 'should set all properties'() {
        def hosts = [new ServerAddress('localhost'), new ServerAddress('localhost', 30000)]
        def serverSelector = new PrimaryServerSelector()
        when:
        def settings = ClusterSettings.builder()
                                      .hosts(hosts)
                                      .mode(ClusterConnectionMode.MULTIPLE)
                                      .requiredClusterType(ClusterType.REPLICA_SET)
                                      .requiredReplicaSetName('foo')
                                      .serverSelector(serverSelector)
                                      .build();

        then:
        settings.hosts == hosts
        settings.mode == ClusterConnectionMode.MULTIPLE
        settings.requiredClusterType == ClusterType.REPLICA_SET
        settings.requiredReplicaSetName == 'foo'
        settings.serverSelector == serverSelector
    }

    def 'when connection string is applied to builder, all properties should be set'() {
        when:
        def settings = ClusterSettings.builder().applyConnectionString(new ConnectionString('mongodb://example.com:27018'))
                                      .build()

        then:
        settings.mode == ClusterConnectionMode.SINGLE
        settings.hosts == [new ServerAddress('example.com:27018')]
        settings.requiredClusterType == ClusterType.UNKNOWN
        settings.requiredReplicaSetName == null

        when:
        settings = ClusterSettings.builder().applyConnectionString(new ConnectionString('mongodb://example.com:27018,' +
                                                                                        'example.com:27019/?replicaSet=test'))
                                  .build()

        then:
        settings.mode == ClusterConnectionMode.MULTIPLE
        settings.hosts == [new ServerAddress('example.com:27018'), new ServerAddress('example.com:27019')]
        settings.requiredClusterType == ClusterType.REPLICA_SET
        settings.requiredReplicaSetName == 'test'

        when:
        settings = ClusterSettings.builder().applyConnectionString(new ConnectionString('mongodb://example.com:27018,example.com:27019'))
                                  .build()

        then:
        settings.mode == ClusterConnectionMode.MULTIPLE
        settings.hosts == [new ServerAddress('example.com:27018'), new ServerAddress('example.com:27019')]
        settings.requiredClusterType == ClusterType.UNKNOWN
        settings.requiredReplicaSetName == null
    }

    def 'when cluster type is unknown and replica set name is specified, should set cluster type to ReplicaSet'() {
        when:
        def settings = ClusterSettings.builder().hosts([new ServerAddress()]).requiredReplicaSetName('yeah').build()

        then:
        ClusterType.REPLICA_SET == settings.requiredClusterType
    }

    def 'connection mode should default to Multiple regardless of hosts count'() {
        when:
        def settings = ClusterSettings.builder().hosts([new ServerAddress()]).build()

        then:
        settings.mode == ClusterConnectionMode.MULTIPLE
    }

    def 'when mode is Single and hosts size is greater than one, should throw'() {
        when:
        ClusterSettings.builder().hosts([new ServerAddress(), new ServerAddress('other')]).mode(ClusterConnectionMode.SINGLE).build();
        then:
        thrown(IllegalArgumentException)

    }

    def 'when cluster type is Standalone and multiple hosts are specified, should throw'() {
        when:
        ClusterSettings.builder().hosts([new ServerAddress(), new ServerAddress('other')]).requiredClusterType(ClusterType.STANDALONE)
                       .build();
        then:
        thrown(IllegalArgumentException)
    }

    def 'when a replica set name is specified and type is Standalone, should throw'() {
        when:
        ClusterSettings.builder().hosts([new ServerAddress(), new ServerAddress('other')]).requiredReplicaSetName('foo')
                       .requiredClusterType(ClusterType.STANDALONE).build();
        then:
        thrown(IllegalArgumentException)
    }

    def 'when a replica set name is specified and type is Sharded, should throw'() {
        when:
        ClusterSettings.builder().hosts([new ServerAddress(), new ServerAddress('other')]).requiredReplicaSetName('foo')
                       .requiredClusterType(ClusterType.SHARDED).build();
        then:
        thrown(IllegalArgumentException)
    }


    def 'should throws if hosts list is null'() {
        when:
        ClusterSettings.builder().hosts(null).build();

        then:
        thrown(IllegalArgumentException)
    }

    def 'should throws if hosts list is empty'() {
        when:
        ClusterSettings.builder().hosts([]).build();

        then:
        thrown(IllegalArgumentException)
    }

    def 'should remove duplicate hosts'() {
        when:
        def settings = ClusterSettings.builder().hosts([new ServerAddress('server1'),
                                                        new ServerAddress('server2'),
                                                        new ServerAddress('server1')]).build();

        then:
        settings.getHosts() == [new ServerAddress('server1'), new ServerAddress('server2')]
    }
}
