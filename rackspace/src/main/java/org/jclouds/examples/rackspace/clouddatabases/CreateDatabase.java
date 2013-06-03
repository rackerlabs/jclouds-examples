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
package org.jclouds.examples.rackspace.clouddatabases;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.trove.v1.TroveApi;
import org.jclouds.openstack.trove.v1.domain.Instance;
import org.jclouds.openstack.trove.v1.features.DatabaseApi;
import org.jclouds.openstack.trove.v1.features.InstanceApi;

import com.google.common.io.Closeables;

/**
 * This example creates a MySQL database on a Cloud Databases instance.
 * The instance is created in the CreateInstance example.
 * Think of the instance as a type of server. Multiple databases can run on the same instance.
 * 
 * @author Zack Shoylev
 */
public class CreateDatabase implements Closeable {
   private TroveApi api;
   private InstanceApi instanceApi;
   private DatabaseApi databaseApi;

   /**
    * To get a username and API key see 
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument  (args[0]) must be your username.
    * The second argument (args[1]) must be your API key.
    * @throws IOException 
    */
   public static void main(String[] args) throws IOException {
      
      CreateDatabase createDatabase = new CreateDatabase();

      try {
         createDatabase.init(args);
         Instance instance = createDatabase.getInstance();
         createDatabase.createDatabase(instance);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         createDatabase.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds to use the Rackspace Cloud (US).
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-clouddatabases-uk".
      String provider = "rackspace-clouddatabases-us";

      String username = args[0];
      String apiKey = args[1];
      
      api = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildApi(TroveApi.class);
      
      instanceApi = api.getInstanceApiForZone(Constants.ZONE);
      databaseApi = api.getDatabaseApiForInstanceInZone(getInstance().getId(), Constants.ZONE);
   }

   /**
    * @return Instance The Instance created in the CreateInstance example.
    */
   private Instance getInstance() {
      for (Instance instance: instanceApi.list()) {
         if (instance.getName().startsWith(Constants.NAME)) {
            return instance;
         }
      }

      throw new RuntimeException(Constants.NAME + " not found. Run the CreateInstance example first.");
   }

   private void createDatabase(Instance instance) throws TimeoutException {
      System.out.println("Create Database");

      boolean result = databaseApi.create(Constants.NAME);
      System.out.println("  " + result);
   }

   /**
    * Always close your service when you're done with it.
    * @throws IOException 
    */
   public void close() throws IOException {
      Closeables.close(api, true);
   }
}
