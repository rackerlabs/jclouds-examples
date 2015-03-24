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
package org.jclouds.examples.rackspace.cloudblockstorage;

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.VolumeType;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.PROVIDER;

/**
 * This example lists all volume types.
 */
public class ListVolumeTypes implements Closeable {
   private final CinderApi cinderApi;
   private final Set<String> regions;

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ListVolumeTypes listVolumeTypes = new ListVolumeTypes(args[0], args[1]);

      try {
         listVolumeTypes.listVolumeTypes();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listVolumeTypes.close();
      }
   }

   public ListVolumeTypes(String username, String apiKey) {
      cinderApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CinderApi.class);
      regions = cinderApi.getConfiguredRegions();
   }

   private void listVolumeTypes() {
      System.out.format("List Volumes Types%n");

      for (String region: regions) {
         System.out.format("  %s%n", region);

         for (VolumeType volumeType: cinderApi.getVolumeTypeApi(region).list()) {
            System.out.format("    %s%n", volumeType);
         }
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(cinderApi, true);
   }
}
