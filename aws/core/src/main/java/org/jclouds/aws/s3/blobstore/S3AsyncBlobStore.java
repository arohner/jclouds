/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.aws.s3.blobstore;

import static com.google.common.util.concurrent.Futures.compose;
import static org.jclouds.blobstore.options.ListContainerOptions.Builder.recursive;
import static org.jclouds.concurrent.ConcurrentUtils.convertExceptionToValue;
import static org.jclouds.concurrent.ConcurrentUtils.makeListenable;

import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.jclouds.Constants;
import org.jclouds.aws.domain.Region;
import org.jclouds.aws.s3.S3AsyncClient;
import org.jclouds.aws.s3.S3Client;
import org.jclouds.aws.s3.blobstore.functions.BlobToObject;
import org.jclouds.aws.s3.blobstore.functions.BucketToResourceList;
import org.jclouds.aws.s3.blobstore.functions.BucketToResourceMetadata;
import org.jclouds.aws.s3.blobstore.functions.ContainerToBucketListOptions;
import org.jclouds.aws.s3.blobstore.functions.ObjectToBlob;
import org.jclouds.aws.s3.blobstore.functions.ObjectToBlobMetadata;
import org.jclouds.aws.s3.blobstore.internal.BaseS3BlobStore;
import org.jclouds.aws.s3.domain.BucketMetadata;
import org.jclouds.aws.s3.domain.ListBucketResponse;
import org.jclouds.aws.s3.domain.ObjectMetadata;
import org.jclouds.aws.s3.options.ListBucketOptions;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.KeyNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.ListContainerResponse;
import org.jclouds.blobstore.domain.ListResponse;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.Blob.Factory;
import org.jclouds.blobstore.domain.internal.ListResponseImpl;
import org.jclouds.blobstore.functions.BlobToHttpGetOptions;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.blobstore.strategy.ClearListStrategy;
import org.jclouds.blobstore.strategy.GetDirectoryStrategy;
import org.jclouds.blobstore.strategy.MkdirStrategy;
import org.jclouds.http.options.GetOptions;
import org.jclouds.logging.Logger.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * 
 * @author Adrian Cole
 */
public class S3AsyncBlobStore extends BaseS3BlobStore implements AsyncBlobStore {

   @Inject
   public S3AsyncBlobStore(S3AsyncClient async, S3Client sync, Factory blobFactory,
            LoggerFactory logFactory, ClearListStrategy clearContainerStrategy,
            ObjectToBlobMetadata object2BlobMd, ObjectToBlob object2Blob, BlobToObject blob2Object,
            ContainerToBucketListOptions container2BucketListOptions,
            BlobToHttpGetOptions blob2ObjectGetOptions, GetDirectoryStrategy getDirectoryStrategy,
            MkdirStrategy mkdirStrategy, BucketToResourceMetadata bucket2ResourceMd,
            BucketToResourceList bucket2ResourceList,
            @Named(Constants.PROPERTY_USER_THREADS) ExecutorService service) {
      super(async, sync, blobFactory, logFactory, clearContainerStrategy, object2BlobMd,
               object2Blob, blob2Object, container2BucketListOptions, blob2ObjectGetOptions,
               getDirectoryStrategy, mkdirStrategy, bucket2ResourceMd, bucket2ResourceList, service);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#listOwnedBuckets}
    */
   @Override
   public ListenableFuture<? extends ListResponse<? extends StorageMetadata>> list() {
      return compose(
               async.listOwnedBuckets(),
               new Function<SortedSet<BucketMetadata>, org.jclouds.blobstore.domain.ListResponse<? extends StorageMetadata>>() {
                  public org.jclouds.blobstore.domain.ListResponse<? extends StorageMetadata> apply(
                           SortedSet<BucketMetadata> from) {
                     return new ListResponseImpl<StorageMetadata>(Iterables.transform(from,
                              bucket2ResourceMd), null, null, false);
                  }
               }, service);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#bucketExists}
    * 
    * @param container
    *           bucket name
    */
   @Override
   public ListenableFuture<Boolean> containerExists(String container) {
      return async.bucketExists(container);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#putBucketInRegion}
    * 
    * @param location
    *           corresponds to {@link Region#fromValue}
    * @param container
    *           bucket name
    */
   @Override
   public ListenableFuture<Boolean> createContainerInLocation(String location, String container) {
      return async.putBucketInRegion(Region.fromValue(location), container);
   }

   /**
    * This implementation invokes
    * {@link #list(String,org.jclouds.blobstore.options.ListContainerOptions)}
    * 
    * @param container
    *           bucket name
    */
   @Override
   public ListenableFuture<? extends ListContainerResponse<? extends StorageMetadata>> list(
            String container) {
      return this.list(container, org.jclouds.blobstore.options.ListContainerOptions.NONE);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#listBucket}
    * 
    * @param container
    *           bucket name
    */
   @Override
   public ListenableFuture<? extends ListContainerResponse<? extends StorageMetadata>> list(
            String container, ListContainerOptions options) {
      ListBucketOptions httpOptions = container2BucketListOptions.apply(options);
      ListenableFuture<ListBucketResponse> returnVal = async.listBucket(container, httpOptions);
      return compose(returnVal, bucket2ResourceList, service);
   }

   /**
    * This implementation invokes {@link ClearListStrategy#execute} with the
    * {@link ListContainerOptions#recursive} option.
    * 
    * @param container
    *           bucket name
    */
   @Override
   public ListenableFuture<Void> clearContainer(final String container) {
      return makeListenable(service.submit(new Callable<Void>() {

         public Void call() throws Exception {
            clearContainerStrategy.execute(container, recursive());
            return null;
         }

      }), service);
   }

   /**
    * This implementation invokes {@link ClearListStrategy#execute} with the
    * {@link ListContainerOptions#recursive} option. Then, it invokes
    * {@link S3AsyncClient#deleteBucketIfEmpty}
    * 
    * @param container
    *           bucket name
    */
   @Override
   public ListenableFuture<Void> deleteContainer(final String container) {
      return makeListenable(service.submit(new Callable<Void>() {

         public Void call() throws Exception {
            clearContainerStrategy.execute(container, recursive());
            async.deleteBucketIfEmpty(container).get();
            return null;
         }

      }), service);
   }

   /**
    * This implementation invokes {@link GetDirectoryStrategy#execute}
    * 
    * @param container
    *           bucket name
    * @param directory
    *           virtual path
    */
   @Override
   public ListenableFuture<Boolean> directoryExists(final String container, final String directory) {
      return makeListenable(service.submit(new Callable<Boolean>() {

         public Boolean call() throws Exception {
            try {
               getDirectoryStrategy.execute(container, directory);
               return true;
            } catch (KeyNotFoundException e) {
               return false;
            }
         }

      }), service);
   }

   /**
    * This implementation invokes {@link MkdirStrategy#execute}
    * 
    * @param container
    *           bucket name
    * @param directory
    *           virtual path
    */
   @Override
   public ListenableFuture<Void> createDirectory(final String container, final String directory) {
      return makeListenable(service.submit(new Callable<Void>() {

         public Void call() throws Exception {
            mkdirStrategy.execute(container, directory);
            return null;
         }

      }), service);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#objectExists}
    * 
    * @param container
    *           bucket name
    * @param key
    *           object key
    */
   @Override
   public ListenableFuture<Boolean> blobExists(String container, String key) {
      return async.objectExists(container, key);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#headObject}
    * 
    * @param container
    *           bucket name
    * @param key
    *           object key
    */
   @Override
   public ListenableFuture<BlobMetadata> blobMetadata(String container, String key) {
      return compose(convertExceptionToValue(async.headObject(container, key),
               KeyNotFoundException.class, null), new Function<ObjectMetadata, BlobMetadata>() {

         @Override
         public BlobMetadata apply(ObjectMetadata from) {
            return object2BlobMd.apply(from);
         }

      }, service);
   }

   /**
    * This implementation invokes
    * {@link #getBlob(String,String,org.jclouds.blobstore.options.GetOptions)}
    * 
    * @param container
    *           bucket name
    * @param key
    *           object key
    */
   @Override
   public ListenableFuture<Blob> getBlob(String container, String key) {
      return getBlob(container, key, org.jclouds.blobstore.options.GetOptions.NONE);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#getObject}
    * 
    * @param container
    *           bucket name
    * @param key
    *           object key
    */
   @Override
   public ListenableFuture<Blob> getBlob(String container, String key,
            org.jclouds.blobstore.options.GetOptions options) {
      GetOptions httpOptions = blob2ObjectGetOptions.apply(options);
      return compose(convertExceptionToValue(async.getObject(container, key, httpOptions),
               KeyNotFoundException.class, null), object2Blob, service);
   }

   /**
    * This implementation invokes {@link S3AsyncClient#putObject}
    * 
    * @param container
    *           bucket name
    * @param blob
    *           object
    */
   @Override
   public ListenableFuture<String> putBlob(String container, Blob blob) {
      return async.putObject(container, blob2Object.apply(blob));
   }

   /**
    * This implementation invokes {@link S3AsyncClient#deleteObject}
    * 
    * @param container
    *           bucket name
    * @param key
    *           object key
    */
   @Override
   public ListenableFuture<Void> removeBlob(String container, String key) {
      return async.deleteObject(container, key);
   }

}
