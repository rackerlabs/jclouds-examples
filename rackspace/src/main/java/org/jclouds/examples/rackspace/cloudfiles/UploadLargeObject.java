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

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.REGION;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.blobstore.RegionScopedBlobStoreContext;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Upload a large object in the Cloud Files container from the CreateContainer example.
 */
public class UploadLargeObject implements Closeable {
   private BlobStore blobStore;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UploadLargeObject createContainer = new UploadLargeObject(args[0], args[1]);
      File largeFile = new File("largefile.dat");
      File downloadedFile = new File(largeFile.getName()+".downloaded");

      try {
         // Create a 200MB file for this example
         createContainer.createRandomFile(200000000, largeFile);
         createContainer.uploadLargeObjectFromFile(largeFile);
         createContainer.downloadLargeObjectToFile(largeFile.getName());
         System.out.println("Random     file hash: " + Files.hash(largeFile, Hashing.md5()));
         System.out.println("Downloaded file hash: " + Files.hash(downloadedFile, Hashing.md5()));
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createContainer.cleanup();
         createContainer.close();
         if(largeFile.exists()) largeFile.delete();
         if(downloadedFile.exists()) downloadedFile.delete();
      }
   }

   public UploadLargeObject(String username, String apiKey) {
      Properties overrides = new Properties();
      // This property controls the number of parts being uploaded in parallel, the default is 4
      overrides.setProperty("jclouds.user-threads", "14");
      // This property controls the size (in bytes) of parts being uploaded in parallel, the default is 33554432 bytes = 32 MB
      overrides.setProperty("jclouds.mpu.parts.size", "1100000"); // 1 MB

      RegionScopedBlobStoreContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(RegionScopedBlobStoreContext.class);
      blobStore = context.getBlobStore(REGION);

      blobStore.createContainerInLocation(null, CONTAINER);
   }

   /**
    * Upload a large object from a File using the BlobStore API.
    *
    * @throws ExecutionException
    * @throws InterruptedException
    */
   private void uploadLargeObjectFromFile(File largeFile) throws InterruptedException, ExecutionException {
      System.out.format("Upload Large Object From File%n");

      ByteSource source = Files.asByteSource(largeFile);
      // create the payload and set the content length
      Payload payload = Payloads.newByteSourcePayload(source);
      payload.getContentMetadata().setContentLength(largeFile.length());

      Blob blob = blobStore.blobBuilder(largeFile.getName())
            .payload(payload)
            .build();

      // configure the blobstore to use multipart uploading of the file
      String eTag = blobStore.putBlob(CONTAINER, blob, multipart());

      System.out.format("  Uploaded %s eTag=%s to %s in %s%n", largeFile.getName(), eTag, REGION, CONTAINER);
   }

   /**
    * Download a large object from a File using the BlobStore API.
    *
    * @throws ExecutionException
    * @throws InterruptedException
    */
   private void downloadLargeObjectToFile(String blobName) throws InterruptedException, ExecutionException {
      System.out.format("Download large object to file%n");

      blobStore.downloadBlob(CONTAINER, blobName, new File(blobName + ".downloaded"));
   }

   private void cleanup() {
      System.out.format("Cleaning up...%n");
      blobStore.clearContainer(CONTAINER);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(blobStore.getContext(), true);
   }

   /**
    * Helper method; so that we don't have to add a large file to the repo
    * @param size File size
    * @param file The new random file to generate (will overwrite if it exists)
    * @throws IOException
    * @throws InterruptedException
    */
   private void createRandomFile(long size, File file) throws IOException, InterruptedException {
      RandomAccessFile raf = null;

      // Reserve space for performance reasons
      raf = new RandomAccessFile(file.getAbsoluteFile(), "rw");
      raf.seek(size - 1);
      raf.write(0);

      // Loop through ranges within the file
      long from;
      long to;
      long partSize = 1000000;

      ExecutorService threadPool = Executors.newFixedThreadPool(16);

      for (from = 0; from < size; from = from + partSize) {
         to = (from + partSize >= size) ? size - 1 : from + partSize - 1;
         RandomFileWriter writer = new RandomFileWriter(raf, from, to);
         threadPool.submit(writer);
      }

      threadPool.shutdown();
      threadPool.awaitTermination(1, TimeUnit.DAYS);

      raf.close();
   }

   /**
    * Helper class that runs the random file generation
    */
   private final class RandomFileWriter implements Runnable {
      private final RandomAccessFile raf;
      private final long begin;
      private final long end;

      RandomFileWriter(RandomAccessFile raf, long begin, long end) {
         this.raf = raf;
         this.begin = begin;
         this.end = end;
      }

      @Override
      public void run() {
         try {
            byte[] targetArray = new byte[(int) (end - begin + 1)];
            Random random = new Random();
            random.nextBytes(targetArray);
            // Map file region
            MappedByteBuffer out = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, begin, end - begin + 1);
            out.put(targetArray);
            out.force();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }
   }
}
