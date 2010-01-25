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
package org.jclouds.rest.internal;

/**
 * Generates RESTful clients from appropriately annotated interfaces.
 * 
 * @author Adrian Cole
 */
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.Constants;
import org.jclouds.concurrent.FutureExceptionParser;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.TransformingHttpCommand;
import org.jclouds.logging.Logger;
import org.jclouds.rest.InvocationContext;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

@Singleton
public class AsyncRestClientProxy<T> implements InvocationHandler {
   private final Injector injector;
   private final RestAnnotationProcessor<T> util;
   private final Class<T> declaring;
   private final Factory commandFactory;

   /**
    * maximum duration of an unwrapped http Request
    */
   @Inject(optional = true)
   @Named(Constants.PROPERTY_HTTP_REQUEST_TIMEOUT)
   protected long requestTimeoutMilliseconds = 30000;

   @Resource
   protected Logger logger = Logger.NULL;

   @SuppressWarnings("unchecked")
   @Inject
   public AsyncRestClientProxy(Injector injector, Factory factory, RestAnnotationProcessor<T> util,
            TypeLiteral<T> typeLiteral) {
      this.injector = injector;
      this.util = util;
      this.declaring = (Class<T>) typeLiteral.getRawType();
      this.commandFactory = factory;
   }

   public Object invoke(Object o, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("equals")) {
         return this.equals(o);
      } else if (method.getName().equals("toString")) {
         return this.toString();
      } else if (method.getName().equals("hashCode")) {
         return this.hashCode();
      } else if (method.getName().startsWith("new")) {
         return injector.getInstance(method.getReturnType());
      } else if (util.getDelegateOrNull(method) != null
               && ListenableFuture.class.isAssignableFrom(method.getReturnType())) {
         return createFuture(method, args);
      } else {
         throw new RuntimeException("method is intended solely to set constants: " + method);
      }
   }

   @SuppressWarnings("unchecked")
   private ListenableFuture<?> createFuture(Method method, Object[] args) throws ExecutionException {
      method = util.getDelegateOrNull(method);
      logger.trace("Converting %s.%s", declaring.getSimpleName(), method.getName());
      Function<Exception, ?> exceptionParser = util
               .createExceptionParserOrThrowResourceNotFoundOn404IfNoAnnotation(method);
      // in case there is an exception creating the request, we should at least pass in args
      if (exceptionParser instanceof InvocationContext) {
         ((InvocationContext) exceptionParser).setContext(null);
      }
      GeneratedHttpRequest<T> request;
      try {
         request = util.createRequest(method, args);
         if (exceptionParser instanceof InvocationContext) {
            ((InvocationContext) exceptionParser).setContext(request);
         }
      } catch (RuntimeException e) {
         if (exceptionParser != null) {
            try {
               return Futures.immediateFuture(exceptionParser.apply(e));
            } catch (Exception ex) {
               return Futures.immediateFailedFuture(ex);
            }
         }
         return Futures.immediateFailedFuture(e);
      }
      logger.trace("Converted %s.%s to %s", declaring.getSimpleName(), method.getName(), request
               .getRequestLine());

      Function<HttpResponse, ?> transformer = util.createResponseParser(method, request);
      logger.trace("Response from %s.%s is parsed by %s", declaring.getSimpleName(), method
               .getName(), transformer.getClass().getSimpleName());

      logger.debug("Invoking %s.%s", declaring.getSimpleName(), method.getName());
      ListenableFuture<?> result = commandFactory.create(request, transformer).execute();

      if (exceptionParser != null) {
         logger.trace("Exceptions from %s.%s are parsed by %s", declaring.getSimpleName(), method
                  .getName(), exceptionParser.getClass().getSimpleName());
         result = new FutureExceptionParser(result, exceptionParser);
      }
      return result;
   }

   public static interface Factory {
      public TransformingHttpCommand<?> create(GeneratedHttpRequest<?> request,
               Function<HttpResponse, ?> transformer);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof AsyncRestClientProxy<?>))
         return false;
      AsyncRestClientProxy<?> other = (AsyncRestClientProxy<?>) obj;
      if (other == this)
         return true;
      if (other.declaring != this.declaring)
         return false;
      return super.equals(obj);
   }

   @Override
   public int hashCode() {
      return declaring.hashCode();
   }

   public String toString() {
      return "Client Proxy for :" + declaring.getName();
   }
}