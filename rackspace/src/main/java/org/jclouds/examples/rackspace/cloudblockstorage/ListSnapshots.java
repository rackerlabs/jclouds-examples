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

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Snapshot;

import java.io.Closeable;
import java.util.Set;

import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.PROVIDER;

/**
 * This example lists all of your snapshots.
 * 
 * @author Everett Toews
 */
public class ListSnapshots implements Closeable {
   private final CinderApi cinderApi;
   private final Set<String> zones;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListSnapshots listSnapshots = new ListSnapshots(args[0], args[1]);

      try {
         listSnapshots.listSnapshots();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listSnapshots.close();
      }
   }

   public ListSnapshots(String username, String apiKey) {
      cinderApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CinderApi.class);
      zones = cinderApi.getConfiguredZones();
   }

   private void listSnapshots() {
      System.out.format("List Snapshots%n");

      for (String zone: zones) {
         System.out.format("  %s%n", zone);

         for (Snapshot snapshot: cinderApi.getSnapshotApiForZone(zone).listInDetail()) {
            System.out.format("    %s%n", snapshot);
         }
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    * When jclouds switches to Java 7 the try/catch block below can be removed.
    */
   public void close() {
      if (cinderApi != null) {
         try {
            cinderApi.close();
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
}
