/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.examples.rackspace.cloudloadbalancers;

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.UpdateLoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.cloudloadbalancers.Constants.*;
import static org.jclouds.rackspace.cloudloadbalancers.v1.domain.internal.BaseLoadBalancer.Algorithm.RANDOM;

/**
 * This example updates a Load Balancer.
 *
 */
public class UpdateLoadBalancers implements Closeable {
   private final CloudLoadBalancersApi clbApi;
   private final LoadBalancerApi lbApi;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UpdateLoadBalancers updateLoadBalancers = new UpdateLoadBalancers(args[0], args[1]);

      try {
         LoadBalancer loadBalancer = updateLoadBalancers.getLoadBalancer();
         updateLoadBalancers.updateLoadBalancer(loadBalancer);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         updateLoadBalancers.close();
      }
   }

   public UpdateLoadBalancers(String username, String apiKey) {
      clbApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudLoadBalancersApi.class);
      lbApi = clbApi.getLoadBalancerApi(REGION);
   }

   private LoadBalancer getLoadBalancer() {
      for (LoadBalancer loadBalancer: lbApi.list().concat()) {
         if (loadBalancer.getName().startsWith(NAME)) {
            return loadBalancer;
         }
      }

      throw new RuntimeException(NAME + " not found. Run a CreateLoadBalancer* example first.");
   }

   private void updateLoadBalancer(LoadBalancer loadBalancer) throws TimeoutException {
      System.out.format("Update Load Balancer%n");

      UpdateLoadBalancer updateLB = UpdateLoadBalancer.builder()
            .name(NAME + "-update")
            .protocol("HTTPS")
            .port(443)
            .algorithm(RANDOM)
            .build();

      lbApi.update(loadBalancer.getId(), updateLB);

      // Wait for the Load Balancer to become Active before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!LoadBalancerPredicates.awaitAvailable(lbApi).apply(loadBalancer)) {
         throw new TimeoutException("Timeout on loadBalancer: " + loadBalancer);
      }

      System.out.format("  %s%n", true);
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(clbApi, true);
   }
}
