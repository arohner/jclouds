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
package org.jclouds.mezeo.pcs2.config;

import static com.google.common.util.concurrent.Executors.sameThreadExecutor;
import static org.testng.Assert.assertEquals;

import org.jclouds.Constants;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.http.functions.config.ParserModule;
import org.jclouds.http.handlers.CloseContentAndSetExceptionErrorHandler;
import org.jclouds.http.handlers.DelegatingErrorHandler;
import org.jclouds.http.handlers.DelegatingRetryHandler;
import org.jclouds.http.handlers.RedirectionRetryHandler;
import org.jclouds.logging.Logger;
import org.jclouds.logging.Logger.LoggerFactory;
import org.jclouds.mezeo.pcs2.handlers.PCSClientErrorRetryHandler;
import org.jclouds.mezeo.pcs2.reference.PCSConstants;
import org.jclouds.util.Jsr330;
import org.testng.annotations.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "pcs2.RestContext<PCSClient>ModuleTest")
public class PCSContextModuleTest {

   Injector createInjector() {
      return Guice.createInjector(new PCSRestClientModule(), new PCSContextModule() {
         @Override
         protected void configure() {
            bindConstant().annotatedWith(Jsr330.named(PCSConstants.PROPERTY_PCS2_USER)).to("user");
            bindConstant().annotatedWith(Jsr330.named(PCSConstants.PROPERTY_PCS2_PASSWORD)).to(
                     "key");
            bindConstant().annotatedWith(Jsr330.named(PCSConstants.PROPERTY_PCS2_ENDPOINT)).to(
                     "http://localhost");
            bindConstant().annotatedWith(Jsr330.named(Constants.PROPERTY_IO_WORKER_THREADS))
                     .to("1");
            bindConstant().annotatedWith(Jsr330.named(Constants.PROPERTY_USER_THREADS)).to("1");
            bindConstant().annotatedWith(
                     Jsr330.named(Constants.PROPERTY_MAX_CONNECTIONS_PER_CONTEXT)).to("0");
            bindConstant().annotatedWith(Jsr330.named(Constants.PROPERTY_MAX_CONNECTIONS_PER_HOST))
                     .to("1");
            bind(Logger.LoggerFactory.class).toInstance(new LoggerFactory() {
               public Logger getLogger(String category) {
                  return Logger.NULL;
               }
            });
            super.configure();
         }
      }, new ParserModule(), new JavaUrlHttpCommandExecutorServiceModule(),
               new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()));
   }

   @Test
   void testServerErrorHandler() {
      DelegatingErrorHandler handler = createInjector().getInstance(DelegatingErrorHandler.class);
      assertEquals(handler.getServerErrorHandler().getClass(),
               CloseContentAndSetExceptionErrorHandler.class);
   }

   @Test
   void testClientErrorHandler() {
      DelegatingErrorHandler handler = createInjector().getInstance(DelegatingErrorHandler.class);
      assertEquals(handler.getClientErrorHandler().getClass(),
               CloseContentAndSetExceptionErrorHandler.class);
   }

   @Test
   void testClientRetryHandler() {
      DelegatingRetryHandler handler = createInjector().getInstance(DelegatingRetryHandler.class);
      assertEquals(handler.getClientErrorRetryHandler().getClass(),
               PCSClientErrorRetryHandler.class);
   }

   @Test
   void testRedirectionRetryHandler() {
      DelegatingRetryHandler handler = createInjector().getInstance(DelegatingRetryHandler.class);
      assertEquals(handler.getRedirectionRetryHandler().getClass(), RedirectionRetryHandler.class);
   }

}