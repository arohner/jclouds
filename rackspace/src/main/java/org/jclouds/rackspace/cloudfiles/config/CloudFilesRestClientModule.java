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
package org.jclouds.rackspace.cloudfiles.config;

import javax.inject.Singleton;

import org.jclouds.concurrent.internal.SyncProxy;
import org.jclouds.http.HttpErrorHandler;
import org.jclouds.http.RequiresHttp;
import org.jclouds.http.annotation.ClientError;
import org.jclouds.http.annotation.Redirection;
import org.jclouds.http.annotation.ServerError;
import org.jclouds.rackspace.cloudfiles.CloudFilesAsyncClient;
import org.jclouds.rackspace.cloudfiles.CloudFilesClient;
import org.jclouds.rackspace.cloudfiles.handlers.ParseCloudFilesErrorFromHttpResponse;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.RestClientFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * 
 * @author Adrian Cole
 */
@ConfiguresRestClient
@RequiresHttp
public class CloudFilesRestClientModule extends AbstractModule {
   @Override
   protected void configure() {
      bindErrorHandlers();
   }

   @Provides
   @Singleton
   protected CloudFilesAsyncClient provideAsyncClient(RestClientFactory factory) {
      return factory.create(CloudFilesAsyncClient.class);
   }

   @Provides
   @Singleton
   public CloudFilesClient provideClient(CloudFilesAsyncClient client)
            throws IllegalArgumentException, SecurityException, NoSuchMethodException {
      return SyncProxy.create(CloudFilesClient.class, client);
   }

   protected void bindErrorHandlers() {
      bind(HttpErrorHandler.class).annotatedWith(Redirection.class).to(
               ParseCloudFilesErrorFromHttpResponse.class);
      bind(HttpErrorHandler.class).annotatedWith(ClientError.class).to(
               ParseCloudFilesErrorFromHttpResponse.class);
      bind(HttpErrorHandler.class).annotatedWith(ServerError.class).to(
               ParseCloudFilesErrorFromHttpResponse.class);
   }
}