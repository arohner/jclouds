/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
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
package org.jclouds.aws.ec2.compute.strategy;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions.Builder.keyPair;
import static org.testng.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Provider;

import org.jclouds.aws.domain.Region;
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions;
import org.jclouds.aws.ec2.domain.PlacementGroup;
import org.jclouds.aws.ec2.domain.RegionNameAndPublicKeyMaterial;
import org.jclouds.aws.ec2.functions.CreatePlacementGroupIfNeeded;
import org.jclouds.aws.ec2.options.AWSRunInstancesOptions;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Credentials;
import org.jclouds.ec2.compute.EC2TemplateBuilderTest;
import org.jclouds.ec2.compute.domain.EC2HardwareBuilder;
import org.jclouds.ec2.compute.domain.RegionAndName;
import org.jclouds.ec2.compute.domain.RegionNameAndIngressRules;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.compute.strategy.CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions;
import org.jclouds.ec2.domain.BlockDeviceMapping;
import org.jclouds.ec2.domain.KeyPair;
import org.jclouds.ec2.options.RunInstancesOptions;
import org.jclouds.encryption.internal.Base64;
import org.jclouds.scriptbuilder.domain.Statements;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * @author Adrian Cole
 */
@Test(groups = "unit", singleThreaded = true, testName = "CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptionsTest")
public class CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptionsTest {
   private static final Provider<RunInstancesOptions> OPTIONS_PROVIDER = new javax.inject.Provider<RunInstancesOptions>() {

      @Override
      public RunInstancesOptions get() {
         return new AWSRunInstancesOptions();
      }

   };

   @Test(enabled = false)
   public void testExecuteWithDefaultOptionsEC2() throws SecurityException, NoSuchMethodException {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      Hardware size = EC2HardwareBuilder.m1_small32().build();
      String systemGeneratedKeyPairName = "systemGeneratedKeyPair";
      String generatedGroup = "group";
      Set<String> generatedGroups = ImmutableSet.of(generatedGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = createMock(
               CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class, new Method[] {
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getOptionsProvider"),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewKeyPairUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewPlacementGroupUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getSecurityGroupsForTagAndOptions", String.class, String.class,
                                          TemplateOptions.class) });

      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      Template template = createMock(Template.class);

      // setup expectations
      expect(strategy.getOptionsProvider()).andReturn(OPTIONS_PROVIDER);
      expect(template.getHardware()).andReturn(size).atLeastOnce();
      expect(template.getOptions()).andReturn(options).atLeastOnce();
      expect(options.getBlockDeviceMappings()).andReturn(ImmutableSet.<BlockDeviceMapping> of()).atLeastOnce();
      expect(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options)).andReturn(
               systemGeneratedKeyPairName);
      expect(strategy.getSecurityGroupsForTagAndOptions(region, group, options)).andReturn(generatedGroups);
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getSubnetId()).andReturn(null);
      expect(options.getUserData()).andReturn(null);
      expect(options.isMonitoringEnabled()).andReturn(false);

      // replay mocks
      replay(options);
      replay(template);
      replay(strategy);

      // run
      RunInstancesOptions customize = strategy.execute(region, group, template);
      assertEquals(customize.buildQueryParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildFormParameters().entries(), ImmutableMultimap.<String, String> of("InstanceType",
               size.getProviderId(), "SecurityGroup.1", generatedGroup, "KeyName", systemGeneratedKeyPairName)
               .entries());
      assertEquals(customize.buildMatrixParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildRequestHeaders(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildStringPayload(), null);

      // verify mocks
      verify(options);
      verify(template);
      verify(strategy);
   }

   @Test(enabled = false)
   public void testExecuteForCCAutomatic() throws SecurityException, NoSuchMethodException {
      // setup constants
      String region = Region.US_EAST_1;
      String group = "group";
      Hardware size = EC2TemplateBuilderTest.CC1_4XLARGE;
      String systemGeneratedKeyPairName = "systemGeneratedKeyPair";
      String generatedGroup = "group";
      Set<String> generatedGroups = ImmutableSet.of(generatedGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = createMock(
               CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class, new Method[] {
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getOptionsProvider"),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewKeyPairUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewPlacementGroupUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getSecurityGroupsForTagAndOptions", String.class, String.class,
                                          TemplateOptions.class) });

      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      Template template = createMock(Template.class);

      // setup expectations
      expect(strategy.getOptionsProvider()).andReturn(OPTIONS_PROVIDER);
      expect(template.getHardware()).andReturn(size).atLeastOnce();
      expect(template.getOptions()).andReturn(options).atLeastOnce();
      expect(options.getBlockDeviceMappings()).andReturn(ImmutableSet.<BlockDeviceMapping> of()).atLeastOnce();
      expect(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options)).andReturn(
               systemGeneratedKeyPairName);
      expect(strategy.createNewPlacementGroupUnlessUserSpecifiedOtherwise(region, group, options)).andReturn(
               generatedGroup);
      expect(strategy.getSecurityGroupsForTagAndOptions(region, group, options)).andReturn(generatedGroups);
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getSubnetId()).andReturn(null);
      expect(options.getUserData()).andReturn(null);
      expect(options.isMonitoringEnabled()).andReturn(false);

      // replay mocks
      replay(options);
      replay(template);
      replay(strategy);

      // run
      RunInstancesOptions customize = strategy.execute(region, group, template);
      assertEquals(customize.buildQueryParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildFormParameters().entries(), ImmutableMultimap.<String, String> of("InstanceType",
               size.getProviderId(), "SecurityGroup.1", generatedGroup, "KeyName", systemGeneratedKeyPairName,
               "Placement.GroupName", generatedGroup).entries());
      assertEquals(customize.buildMatrixParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildRequestHeaders(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildStringPayload(), null);

      // verify mocks
      verify(options);
      verify(template);
      verify(strategy);
   }

   @Test(enabled = false)
   public void testExecuteForCCUserSpecified() throws SecurityException, NoSuchMethodException {
      // setup constants
      String region = Region.US_EAST_1;
      String group = "group";
      Hardware size = EC2TemplateBuilderTest.CC1_4XLARGE;
      String systemGeneratedKeyPairName = "systemGeneratedKeyPair";
      String generatedGroup = "group";
      Set<String> generatedGroups = ImmutableSet.of(generatedGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = createMock(
               CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class, new Method[] {
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getOptionsProvider"),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewKeyPairUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewPlacementGroupUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getSecurityGroupsForTagAndOptions", String.class, String.class,
                                          TemplateOptions.class) });

      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      Template template = createMock(Template.class);

      // setup expectations
      expect(strategy.getOptionsProvider()).andReturn(OPTIONS_PROVIDER);
      expect(template.getHardware()).andReturn(size).atLeastOnce();
      expect(template.getOptions()).andReturn(options).atLeastOnce();
      expect(options.getBlockDeviceMappings()).andReturn(ImmutableSet.<BlockDeviceMapping> of()).atLeastOnce();
      expect(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options)).andReturn(
               systemGeneratedKeyPairName);
      expect(strategy.createNewPlacementGroupUnlessUserSpecifiedOtherwise(region, group, options)).andReturn(
               generatedGroup);
      expect(strategy.getSecurityGroupsForTagAndOptions(region, group, options)).andReturn(generatedGroups);
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getSubnetId()).andReturn(null);
      expect(options.getUserData()).andReturn(null);
      expect(options.isMonitoringEnabled()).andReturn(false);

      // replay mocks
      replay(options);
      replay(template);
      replay(strategy);

      // run
      RunInstancesOptions customize = strategy.execute(region, group, template);
      assertEquals(customize.buildQueryParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildFormParameters().entries(), ImmutableMultimap.<String, String> of("InstanceType",
               size.getProviderId(), "SecurityGroup.1", generatedGroup, "KeyName", systemGeneratedKeyPairName,
               "Placement.GroupName", generatedGroup).entries());
      assertEquals(customize.buildMatrixParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildRequestHeaders(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildStringPayload(), null);

      // verify mocks
      verify(options);
      verify(template);
      verify(strategy);
   }

   @Test(enabled = false)
   public void testExecuteWithSubnet() throws SecurityException, NoSuchMethodException {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      Hardware size = EC2HardwareBuilder.m1_small32().build();
      String systemGeneratedKeyPairName = "systemGeneratedKeyPair";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = createMock(
               CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class, new Method[] {
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getOptionsProvider"),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewKeyPairUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewPlacementGroupUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getSecurityGroupsForTagAndOptions", String.class, String.class,
                                          TemplateOptions.class) });

      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      Template template = createMock(Template.class);

      // setup expectations
      expect(strategy.getOptionsProvider()).andReturn(OPTIONS_PROVIDER);
      expect(template.getHardware()).andReturn(size).atLeastOnce();
      expect(template.getOptions()).andReturn(options).atLeastOnce();
      expect(options.getBlockDeviceMappings()).andReturn(ImmutableSet.<BlockDeviceMapping> of()).atLeastOnce();
      expect(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options)).andReturn(
               systemGeneratedKeyPairName);
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getSubnetId()).andReturn("1");
      expect(options.getUserData()).andReturn(null);
      expect(options.isMonitoringEnabled()).andReturn(false);

      // replay mocks
      replay(options);
      replay(template);
      replay(strategy);

      // run
      RunInstancesOptions customize = strategy.execute(region, group, template);
      assertEquals(customize.buildQueryParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildFormParameters().entries(), ImmutableMultimap.<String, String> of("InstanceType",
               size.getProviderId(), "SubnetId", "1", "KeyName", systemGeneratedKeyPairName).entries());
      assertEquals(customize.buildMatrixParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildRequestHeaders(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildStringPayload(), null);

      // verify mocks
      verify(options);
      verify(template);
      verify(strategy);
   }

   @Test(enabled = false)
   public void testExecuteWithUserData() throws SecurityException, NoSuchMethodException {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      Hardware size = EC2HardwareBuilder.m1_small32().build();
      String systemGeneratedKeyPairName = "systemGeneratedKeyPair";
      String generatedGroup = "group";
      Set<String> generatedGroups = ImmutableSet.of(generatedGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = createMock(
               CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class, new Method[] {
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getOptionsProvider"),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewKeyPairUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.class.getDeclaredMethod(
                                 "createNewPlacementGroupUnlessUserSpecifiedOtherwise", String.class, String.class,
                                 TemplateOptions.class),
                        CreateKeyPairAndSecurityGroupsAsNeededAndReturnRunOptions.class
                                 .getDeclaredMethod("getSecurityGroupsForTagAndOptions", String.class, String.class,
                                          TemplateOptions.class) });

      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      Template template = createMock(Template.class);

      // setup expectations
      expect(strategy.getOptionsProvider()).andReturn(OPTIONS_PROVIDER);
      expect(template.getHardware()).andReturn(size).atLeastOnce();
      expect(template.getOptions()).andReturn(options).atLeastOnce();
      expect(options.getBlockDeviceMappings()).andReturn(ImmutableSet.<BlockDeviceMapping> of()).atLeastOnce();
      expect(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options)).andReturn(
               systemGeneratedKeyPairName);
      expect(strategy.getSecurityGroupsForTagAndOptions(region, group, options)).andReturn(generatedGroups);
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getSubnetId()).andReturn(null);
      expect(options.getUserData()).andReturn("hello".getBytes());
      expect(options.isMonitoringEnabled()).andReturn(false);

      // replay mocks
      replay(options);
      replay(template);
      replay(strategy);

      // run
      RunInstancesOptions customize = strategy.execute(region, group, template);
      assertEquals(customize.buildQueryParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildFormParameters().entries(), ImmutableMultimap.<String, String> of("InstanceType",
               size.getProviderId(), "SecurityGroup.1", "group", "KeyName", systemGeneratedKeyPairName, "UserData",
               Base64.encodeBytes("hello".getBytes())).entries());
      assertEquals(customize.buildMatrixParameters(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildRequestHeaders(), ImmutableMultimap.<String, String> of());
      assertEquals(customize.buildStringPayload(), null);

      // verify mocks
      verify(options);
      verify(template);
      verify(strategy);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_reusesKeyWhenToldToWithRunScriptButNoCredentials() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedKeyPair = "myKeyPair";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      EC2TemplateOptions options = createMock(EC2TemplateOptions.class);
      KeyPair keyPair = createMock(KeyPair.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(Maps.<RegionAndName, KeyPair> newConcurrentMap());
      expect(options.getKeyPair()).andReturn(userSuppliedKeyPair);
      expect(options.getPublicKey()).andReturn(null).times(2);
      expect(options.getOverridingCredentials()).andReturn(null);
      expect(options.getRunScript()).andReturn(Statements.exec("echo foo"));
      expect(strategy.credentialsMap.getUnchecked(new RegionAndName(region, userSuppliedKeyPair))).andThrow(
               new NullPointerException());

      // replay mocks
      replay(options);
      replay(keyPair);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options), userSuppliedKeyPair);

      // verify mocks
      verify(options);
      verify(keyPair);
      verifyStrategy(strategy);
   }

   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_reusesKeyWhenToldToWithRunScriptAndCredentialsAlreadyInMap() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedKeyPair = "myKeyPair";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      EC2TemplateOptions options = createMock(EC2TemplateOptions.class);
      KeyPair keyPair = createMock(KeyPair.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(Maps.<RegionAndName, KeyPair> newConcurrentMap());
      expect(options.getPublicKey()).andReturn(null).times(2);
      expect(options.getKeyPair()).andReturn(userSuppliedKeyPair);
      expect(options.getOverridingCredentials()).andReturn(null);
      expect(options.getRunScript()).andReturn(Statements.exec("echo foo"));
      expect(strategy.credentialsMap.getUnchecked(new RegionAndName(region, userSuppliedKeyPair))).andReturn(keyPair);

      // replay mocks
      replay(options);
      replay(keyPair);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options), userSuppliedKeyPair);

      // verify mocks
      verify(options);
      verify(keyPair);
      verifyStrategy(strategy);
   }

   @SuppressWarnings("unchecked")
   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_reusesKeyWhenToldToWithRunScriptAndCredentialsSpecified() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedKeyPair = "myKeyPair";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      EC2TemplateOptions options = createMock(EC2TemplateOptions.class);
      KeyPair keyPair = createMock(KeyPair.class);

      ConcurrentMap<RegionAndName, KeyPair> backing = createMock(ConcurrentMap.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(backing).atLeastOnce();
      expect(backing.containsKey(new RegionAndName(region, group))).andReturn(false);
      expect(options.getKeyPair()).andReturn(userSuppliedKeyPair);
      expect(options.getPublicKey()).andReturn(null).times(2);
      expect(options.getOverridingCredentials()).andReturn(new Credentials(null, "MyRsa")).atLeastOnce();
      expect(
               backing.put(new RegionAndName(region, userSuppliedKeyPair), KeyPair.builder().region(region).keyName(
                        userSuppliedKeyPair).keyFingerprint("//TODO").keyMaterial("MyRsa").build())).andReturn(null);
      expect(options.getRunScript()).andReturn(Statements.exec("echo foo"));
      expect(strategy.credentialsMap.getUnchecked(new RegionAndName(region, userSuppliedKeyPair))).andReturn(keyPair);

      // replay mocks
      replay(options);
      replay(keyPair);
      replay(backing);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options), userSuppliedKeyPair);

      // verify mocks
      verify(options);
      verify(keyPair);
      verify(backing);
      verifyStrategy(strategy);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_importsKeyPairAndUnsetsTemplateInstructionWhenPublicKeySuppliedAndAddsCredentialToMapWhenOverridingCredsAreSet() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = keyPair(group).authorizePublicKey("ssh-rsa").overrideCredentialsWith(
               new Credentials("foo", "private"));
      KeyPair keyPair = new KeyPair(region, group, "//TODO", null);
      ConcurrentMap<RegionAndName, KeyPair> backing = createMock(ConcurrentMap.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(backing).atLeastOnce();
      expect(backing.containsKey(new RegionAndName(region, group))).andReturn(false);
      expect(strategy.importExistingKeyPair.apply(new RegionNameAndPublicKeyMaterial(region, group, "private")))
               .andReturn(keyPair);
      expect(backing.put(new RegionAndName(region, group), keyPair.toBuilder().keyMaterial("private").build()))
               .andReturn(null);

      // replay mocks
      replay(backing);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options), group);

      // verify mocks
      verify(backing);
      verifyStrategy(strategy);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_importsKeyPairAndUnsetsTemplateInstructionWhenPublicKeySupplied() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      AWSEC2TemplateOptions options = keyPair(group).authorizePublicKey("ssh-rsa");

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();

      KeyPair keyPair = new KeyPair(region, "jclouds#" + group, "fingerprint", null);

      ConcurrentMap<RegionAndName, KeyPair> backing = createMock(ConcurrentMap.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(backing).atLeastOnce();
      expect(backing.containsKey(new RegionAndName(region, group))).andReturn(false);
      expect(strategy.importExistingKeyPair.apply(new RegionNameAndPublicKeyMaterial(region, group, "ssh-rsa")))
               .andReturn(keyPair);
      expect(backing.put(new RegionAndName(region, group), keyPair)).andReturn(null);

      // replay mocks
      replay(backing);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options), "jclouds#" + group);

      // verify mocks
      verify(backing);
      verifyStrategy(strategy);
   }

   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_createsNewKeyPairAndReturnsItsNameWhenNoPublicKeySupplied() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedKeyPair = null;
      boolean shouldAutomaticallyCreateKeyPair = true;
      String systemGeneratedKeyPairName = "systemGeneratedKeyPair";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      KeyPair keyPair = createMock(KeyPair.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(Maps.<RegionAndName, KeyPair> newConcurrentMap());
      expect(options.getKeyPair()).andReturn(userSuppliedKeyPair);
      expect(options.shouldAutomaticallyCreateKeyPair()).andReturn(shouldAutomaticallyCreateKeyPair);
      expect(strategy.credentialsMap.getUnchecked(new RegionAndName(region, group))).andReturn(keyPair);
      expect(options.getPublicKey()).andReturn(null).times(2);
      expect(keyPair.getKeyName()).andReturn(systemGeneratedKeyPairName).atLeastOnce();
      expect(options.getRunScript()).andReturn(null);

      // replay mocks
      replay(options);
      replay(keyPair);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options),
               systemGeneratedKeyPairName);

      // verify mocks
      verify(options);
      verify(keyPair);
      verifyStrategy(strategy);
   }

   @SuppressWarnings("unchecked")
   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_returnsExistingKeyIfAlreadyPresent() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String systemGeneratedKeyPairName = "systemGeneratedKeyPair";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      KeyPair keyPair = createMock(KeyPair.class);
      ConcurrentMap<RegionAndName, KeyPair> backing = createMock(ConcurrentMap.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(backing);
      expect(backing.containsKey(new RegionAndName(region, group))).andReturn(true);
      expect(strategy.credentialsMap.getUnchecked(new RegionAndName(region, group))).andReturn(keyPair);
      expect(keyPair.getKeyName()).andReturn(systemGeneratedKeyPairName).atLeastOnce();

      // replay mocks
      replay(options);
      replay(keyPair);
      replay(backing);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options),
               systemGeneratedKeyPairName);

      // verify mocks
      verify(options);
      verify(keyPair);
      verify(backing);
      verifyStrategy(strategy);
   }

   public void testCreateNewKeyPairUnlessUserSpecifiedOtherwise_doesntCreateAKeyPairAndReturnsNullWhenToldNotTo() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedKeyPair = null;
      boolean shouldAutomaticallyCreateKeyPair = false; // here's the important
      // part!

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      KeyPair keyPair = createMock(KeyPair.class);

      // setup expectations
      expect(strategy.credentialsMap.asMap()).andReturn(Maps.<RegionAndName, KeyPair> newConcurrentMap());
      expect(options.getPublicKey()).andReturn(null).times(2);
      expect(options.getKeyPair()).andReturn(userSuppliedKeyPair);
      expect(options.getRunScript()).andReturn(null);
      expect(options.shouldAutomaticallyCreateKeyPair()).andReturn(shouldAutomaticallyCreateKeyPair);

      // replay mocks
      replay(options);
      replay(keyPair);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewKeyPairUnlessUserSpecifiedOtherwise(region, group, options), null);

      // verify mocks
      verify(options);
      verify(keyPair);
      verifyStrategy(strategy);
   }

   public void testGetSecurityGroupsForTagAndOptions_createsNewGroupByDefaultWhenNoPortsAreSpecifiedWhenDoesntExist() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String generatedMarkerGroup = "jclouds#group#" + Region.AP_SOUTHEAST_1;
      Set<String> groupNames = ImmutableSet.<String> of();
      int[] ports = new int[] {};
      boolean shouldAuthorizeSelf = true;
      Set<String> returnVal = ImmutableSet.<String> of(generatedMarkerGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);

      // setup expectations
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getGroups()).andReturn(groupNames).atLeastOnce();
      expect(options.getInboundPorts()).andReturn(ports).atLeastOnce();
      RegionNameAndIngressRules regionNameAndIngressRules = new RegionNameAndIngressRules(region, generatedMarkerGroup,
               ports, shouldAuthorizeSelf);
      expect(strategy.securityGroupMap.getUnchecked(regionNameAndIngressRules)).andReturn(group);

      // replay mocks
      replay(options);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.getSecurityGroupsForTagAndOptions(region, group, options), returnVal);

      // verify mocks
      verify(options);
      verifyStrategy(strategy);
   }

   public void testGetSecurityGroupsForTagAndOptions_createsNewGroupByDefaultWhenPortsAreSpecifiedWhenDoesntExist() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String generatedMarkerGroup = "jclouds#group#" + Region.AP_SOUTHEAST_1;
      Set<String> groupNames = ImmutableSet.<String> of();
      int[] ports = new int[] { 22, 80 };
      boolean shouldAuthorizeSelf = true;
      Set<String> returnVal = ImmutableSet.<String> of(generatedMarkerGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);

      // setup expectations
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getGroups()).andReturn(groupNames).atLeastOnce();
      expect(options.getInboundPorts()).andReturn(ports).atLeastOnce();
      RegionNameAndIngressRules regionNameAndIngressRules = new RegionNameAndIngressRules(region, generatedMarkerGroup,
               ports, shouldAuthorizeSelf);
      expect(strategy.securityGroupMap.getUnchecked(regionNameAndIngressRules)).andReturn(generatedMarkerGroup);

      // replay mocks
      replay(options);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.getSecurityGroupsForTagAndOptions(region, group, options), returnVal);

      // verify mocks
      verify(options);
      verifyStrategy(strategy);
   }

   public void testGetSecurityGroupsForTagAndOptions_reusesGroupByDefaultWhenNoPortsAreSpecifiedWhenDoesExist() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String generatedMarkerGroup = "jclouds#group#" + Region.AP_SOUTHEAST_1;
      Set<String> groupNames = ImmutableSet.<String> of();
      int[] ports = new int[] {};
      boolean shouldAuthorizeSelf = true;
      Set<String> returnVal = ImmutableSet.<String> of(generatedMarkerGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);

      // setup expectations
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getGroups()).andReturn(groupNames).atLeastOnce();
      expect(options.getInboundPorts()).andReturn(ports).atLeastOnce();
      RegionNameAndIngressRules regionNameAndIngressRules = new RegionNameAndIngressRules(region, generatedMarkerGroup,
               ports, shouldAuthorizeSelf);
      expect(strategy.securityGroupMap.getUnchecked(regionNameAndIngressRules)).andReturn(generatedMarkerGroup);

      // replay mocks
      replay(options);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.getSecurityGroupsForTagAndOptions(region, group, options), returnVal);

      // verify mocks
      verify(options);
      verifyStrategy(strategy);
   }

   public void testGetSecurityGroupsForTagAndOptions_reusesGroupByDefaultWhenNoPortsAreSpecifiedWhenDoesExistAndAcceptsUserSuppliedGroups() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String generatedMarkerGroup = "jclouds#group#" + Region.AP_SOUTHEAST_1;
      Set<String> groupNames = ImmutableSet.<String> of("group1", "group2");
      int[] ports = new int[] {};
      boolean shouldAuthorizeSelf = true;
      boolean groupExisted = true;
      Set<String> returnVal = ImmutableSet.<String> of(generatedMarkerGroup, "group1", "group2");

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);

      // setup expectations
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of());
      expect(options.getGroups()).andReturn(groupNames).atLeastOnce();
      RegionNameAndIngressRules regionNameAndIngressRules = new RegionNameAndIngressRules(region, generatedMarkerGroup,
               ports, shouldAuthorizeSelf);

      expect(strategy.securityGroupMap.getUnchecked(regionNameAndIngressRules))
               .andReturn(groupExisted ? "group" : null);

      // replay mocks
      replay(options);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.getSecurityGroupsForTagAndOptions(region, group, options), returnVal);

      // verify mocks
      verify(options);
      verifyStrategy(strategy);
   }

   public void testGetSecurityGroupsForTagAndOptions_reusesGroupByDefaultWhenNoPortsAreSpecifiedWhenDoesExistAndAcceptsUserSuppliedGroupIds() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String generatedMarkerGroup = "jclouds#group#" + Region.AP_SOUTHEAST_1;
      Set<String> groupNames = ImmutableSet.<String> of();
      int[] ports = new int[] {};
      boolean shouldAuthorizeSelf = true;
      boolean groupExisted = true;
      Set<String> returnVal = ImmutableSet.<String> of(generatedMarkerGroup);

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);

      // setup expectations
      expect(options.getGroupIds()).andReturn(ImmutableSet.<String> of("group1", "group2"));
      expect(options.getGroups()).andReturn(groupNames).atLeastOnce();
      RegionNameAndIngressRules regionNameAndIngressRules = new RegionNameAndIngressRules(region, generatedMarkerGroup,
               ports, shouldAuthorizeSelf);

      expect(strategy.securityGroupMap.getUnchecked(regionNameAndIngressRules))
               .andReturn(groupExisted ? "group" : null);

      // replay mocks
      replay(options);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.getSecurityGroupsForTagAndOptions(region, group, options), returnVal);

      // verify mocks
      verify(options);
      verifyStrategy(strategy);
   }

   public void testCreateNewPlacementGroupUnlessUserSpecifiedOtherwise_reusesKeyWhenToldTo() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedPlacementGroup = "myPlacementGroup";

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      PlacementGroup placementGroup = createMock(PlacementGroup.class);

      // setup expectations
      expect(options.getPlacementGroup()).andReturn(userSuppliedPlacementGroup);

      // replay mocks
      replay(options);
      replay(placementGroup);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewPlacementGroupUnlessUserSpecifiedOtherwise(region, group, options),
               userSuppliedPlacementGroup);

      // verify mocks
      verify(options);
      verify(placementGroup);
      verifyStrategy(strategy);
   }

   public void testCreateNewPlacementGroupUnlessUserSpecifiedOtherwise_createsNewPlacementGroupAndReturnsItsNameByDefault() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedPlacementGroup = null;
      boolean shouldAutomaticallyCreatePlacementGroup = true;
      String generatedMarkerGroup = "jclouds#group#" + Region.AP_SOUTHEAST_1;

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);

      // setup expectations
      expect(options.getPlacementGroup()).andReturn(userSuppliedPlacementGroup);
      expect(options.shouldAutomaticallyCreatePlacementGroup()).andReturn(shouldAutomaticallyCreatePlacementGroup);
      expect(strategy.placementGroupMap.getUnchecked(new RegionAndName(region, generatedMarkerGroup))).andReturn(
               generatedMarkerGroup);

      // replay mocks
      replay(options);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewPlacementGroupUnlessUserSpecifiedOtherwise(region, group, options),
               generatedMarkerGroup);

      // verify mocks
      verify(options);
      verifyStrategy(strategy);
   }

   public void testCreateNewPlacementGroupUnlessUserSpecifiedOtherwise_doesntCreateAPlacementGroupAndReturnsNullWhenToldNotTo() {
      // setup constants
      String region = Region.AP_SOUTHEAST_1;
      String group = "group";
      String userSuppliedPlacementGroup = null;
      boolean shouldAutomaticallyCreatePlacementGroup = false; // here's the important
      // part!

      // create mocks
      CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy = setupStrategy();
      AWSEC2TemplateOptions options = createMock(AWSEC2TemplateOptions.class);
      PlacementGroup placementGroup = createMock(PlacementGroup.class);

      // setup expectations
      expect(options.getPlacementGroup()).andReturn(userSuppliedPlacementGroup);
      expect(options.shouldAutomaticallyCreatePlacementGroup()).andReturn(shouldAutomaticallyCreatePlacementGroup);

      // replay mocks
      replay(options);
      replay(placementGroup);
      replayStrategy(strategy);

      // run
      assertEquals(strategy.createNewPlacementGroupUnlessUserSpecifiedOtherwise(region, group, options), null);

      // verify mocks
      verify(options);
      verify(placementGroup);
      verifyStrategy(strategy);
   }

   private void verifyStrategy(CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy) {
      verify(strategy.credentialsMap);
      verify(strategy.securityGroupMap);
      verify(strategy.placementGroupMap);
      verify(strategy.importExistingKeyPair);
      verify(strategy.createPlacementGroupIfNeeded);
   }

   @SuppressWarnings("unchecked")
   private CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions setupStrategy() {
      Cache<RegionAndName, KeyPair> credentialsMap = createMock(Cache.class);
      Cache<RegionAndName, String> securityGroupMap = createMock(Cache.class);
      Cache<RegionAndName, String> placementGroupMap = createMock(Cache.class);
      Function<RegionNameAndPublicKeyMaterial, KeyPair> importExistingKeyPair = createMock(Function.class);
      CreatePlacementGroupIfNeeded createPlacementGroupIfNeeded = createMock(CreatePlacementGroupIfNeeded.class);

      return new CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions(credentialsMap, securityGroupMap,
               OPTIONS_PROVIDER, placementGroupMap, createPlacementGroupIfNeeded, importExistingKeyPair);
   }

   private void replayStrategy(CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions strategy) {
      replay(strategy.credentialsMap);
      replay(strategy.securityGroupMap);
      replay(strategy.placementGroupMap);
      replay(strategy.importExistingKeyPair);
      replay(strategy.createPlacementGroupIfNeeded);
   }

}
