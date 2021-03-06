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

package com.mongodb.connection;

import com.mongodb.MongoCredential;
import com.mongodb.event.ClusterListener;
import com.mongodb.event.ConnectionListener;
import com.mongodb.event.ConnectionPoolListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default factory for cluster implementations.
 *
 * @since 3.0
 */
public final class DefaultClusterFactory implements ClusterFactory {
    private static final AtomicInteger NEXT_CLUSTER_ID = new AtomicInteger(1);

    public DefaultClusterFactory() {
    }

    // CHECKSTYLE:OFF
    @Override
    public Cluster create(final ClusterSettings settings, final ServerSettings serverSettings,
                          final ConnectionPoolSettings connectionPoolSettings, final StreamFactory streamFactory,
                          final StreamFactory heartbeatStreamFactory,
                          final List<MongoCredential> credentialList,
                          final ClusterListener clusterListener, final ConnectionPoolListener connectionPoolListener,
                          final ConnectionListener connectionListener) {
        String clusterId = Integer.toString(NEXT_CLUSTER_ID.getAndIncrement());
        ClusterableServerFactory serverFactory = new DefaultClusterableServerFactory(clusterId,
                                                                                     serverSettings,
                                                                                     connectionPoolSettings,
                                                                                     streamFactory,
                                                                                     heartbeatStreamFactory,
                                                                                     credentialList,
                                                                                     connectionListener != null ? connectionListener
                                                                                                             : new NoOpConnectionListener(),
                                                                                     connectionPoolListener != null
                                                                                     ? connectionPoolListener
                                                                                     : new NoOpConnectionPoolListener());

        if (settings.getMode() == ClusterConnectionMode.SINGLE) {
            return new SingleServerCluster(clusterId, settings, serverFactory,
                                           clusterListener != null ? clusterListener : new NoOpClusterListener());
        } else if (settings.getMode() == ClusterConnectionMode.MULTIPLE) {
            return new MultiServerCluster(clusterId, settings, serverFactory,
                                          clusterListener != null ? clusterListener : new NoOpClusterListener());
        } else {
            throw new UnsupportedOperationException("Unsupported cluster mode: " + settings.getMode());
        }
    }
    // CHECKSTYLE:ON
}