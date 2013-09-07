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
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.SnapshotApi;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.options.CreateSnapshotOptions;
import org.jclouds.openstack.cinder.v1.predicates.SnapshotPredicates;

import java.io.Closeable;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.*;

/**
 * This example creates a snapshot of a volume.
 *
 * @author Everett Toews
 */
public class CreateSnapshot implements Closeable {
   private final CinderApi cinderApi;
   private final VolumeApi volumeApi;
   private final SnapshotApi snapshotApi;

    /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CreateSnapshot createSnapshot = new CreateSnapshot(args[0], args[1]);

      try {
         Volume volume = createSnapshot.getVolume();
         createSnapshot.createSnapshot(volume);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createSnapshot.close();
      }
   }

   public CreateSnapshot(String username, String apiKey) {
       cinderApi = ContextBuilder.newBuilder(PROVIDER)
               .credentials(username, apiKey)
               .buildApi(CinderApi.class);
       volumeApi = cinderApi.getVolumeApiForZone(ZONE);
       snapshotApi = cinderApi.getSnapshotApiForZone(ZONE);
   }

   /**
    * @return Volume The Volume created in the CreateVolumeAndAttach example
    */
   private Volume getVolume() {
      for (Volume volume: volumeApi.list()) {
         if (volume.getName().startsWith(NAME)) {
            return volume;
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateVolumeAndAttach example first.");
   }

   private void createSnapshot(Volume volume) throws TimeoutException {
      System.out.format("Create Snapshot%n");

      CreateSnapshotOptions options = CreateSnapshotOptions.Builder
            .name(NAME)
            .description("Snapshot of " + volume.getId());

      Snapshot snapshot = snapshotApi.create(volume.getId(), options);

      // Wait for the snapshot to become Available before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!SnapshotPredicates.awaitAvailable(snapshotApi).apply(snapshot)) {
         throw new TimeoutException("Timeout on volume: " + volume);
      }

      System.out.format("  %s%n", snapshot);
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
