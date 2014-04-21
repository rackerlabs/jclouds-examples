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
package org.jclouds.examples.rackspace.cloudfiles;

import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.REGION;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.blobstore.RegionScopedBlobStoreContext;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Upload objects in the Cloud Files container from the CreateContainer example.
 *  
 * @author Everett Toews
 * @author Jeremy Daggett
 */
public class UploadObjects implements Closeable {
   private final BlobStore blobStore;
   private final CloudFilesApi cloudFiles;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UploadObjects uploadContainer = new UploadObjects(args[0], args[1]);

      try {
         uploadContainer.uploadObjectFromFile();
         uploadContainer.uploadObjectFromString();
         uploadContainer.uploadObjectFromStringWithMetadata();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      finally {
         uploadContainer.close();
      }
   }

   public UploadObjects(String username, String apiKey) {
      ContextBuilder builder = ContextBuilder.newBuilder(PROVIDER)
                                  .credentials(username, apiKey);
      blobStore = builder.buildView(RegionScopedBlobStoreContext.class).blobStoreInRegion(REGION);
      cloudFiles = blobStore.getContext().unwrapApi(CloudFilesApi.class);
   }

   /**
    * Upload an object from a File using the Swift API. 
    */
   private void uploadObjectFromFile() throws IOException {
      System.out.format("Upload Object From File%n");

      String filename = "uploadObjectFromFile";
      String suffix = ".txt";

      File tempFile = File.createTempFile(filename, suffix);
      tempFile.deleteOnExit();

      BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
      out.write("uploadObjectFromFile");
      out.close();

      ByteSource byteSource = Files.asByteSource(tempFile);
      Payload payload = Payloads.newByteSourcePayload(byteSource); 
      
      cloudFiles.objectApiInRegionForContainer(REGION, CONTAINER)
         .replace(filename + suffix, payload, ImmutableMap.<String, String> of());

      System.out.format("  %s%s%n", filename, suffix);
   }

   /**
    * Upload an object from a String using the Swift API. 
    */
   private void uploadObjectFromString() {
      System.out.format("Upload Object From String%n");

      String filename = "uploadObjectFromString.txt";

      ByteSource source = ByteSource.wrap("uploadObjectFromString".getBytes());
      Payload payload = Payloads.newByteSourcePayload(source);

      cloudFiles.objectApiInRegionForContainer(REGION, CONTAINER)
         .replace(filename, payload, ImmutableMap.<String, String>of());

      System.out.format("  %s%n", filename);
   }

   /**
    * Upload an object from a String with metadata using the BlobStore API. 
    */
   private void uploadObjectFromStringWithMetadata() {
      System.out.format("Upload Object From String With Metadata%n");

      String filename = "uploadObjectFromStringWithMetadata.txt";

      Map<String, String> userMetadata = new HashMap<String, String>();
      userMetadata.put("key1", "value1");

      ByteSource source = ByteSource.wrap("uploadObjectFromString".getBytes());

      Blob blob = blobStore.blobBuilder(filename)
            .payload(Payloads.newByteSourcePayload(source))
            .userMetadata(userMetadata)
            .build();

      blobStore.putBlob(CONTAINER, blob);

      System.out.format("  %s%n", filename);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(blobStore.getContext(), true);
   }
}
