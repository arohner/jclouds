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
package org.jclouds.aws.s3.blobstore.integration;

import java.io.IOException;
import java.util.Properties;

import org.jclouds.aws.s3.S3ContextFactory;
import org.jclouds.aws.s3.config.S3StubClientModule;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.integration.internal.BaseBlobStoreIntegrationTest;
import org.jclouds.blobstore.integration.internal.BaseTestInitializer;
import org.jclouds.logging.config.ConsoleLoggingModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * 
 * @author Adrian Cole
 */
public class S3TestInitializer extends BaseTestInitializer {

   @Override
   protected BlobStoreContext createLiveContext(Module configurationModule, String url, String app,
            String account, String key) throws IOException {
      BaseBlobStoreIntegrationTest.SANITY_CHECK_RETURNED_BUCKET_NAME = true;
      return new BlobStoreContextFactory().createContext("s3", account, key, ImmutableSet.of(
               configurationModule, new ConsoleLoggingModule()), new Properties());
   }

   @Override
   protected BlobStoreContext createStubContext() {
      return S3ContextFactory.createContext("user", "pass", new S3StubClientModule());
   }

}