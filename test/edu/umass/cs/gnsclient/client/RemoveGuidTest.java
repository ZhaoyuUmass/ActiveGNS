/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Westy
 *
 */
package edu.umass.cs.gnsclient.client;

import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnscommon.utils.RandomString;
import edu.umass.cs.gnscommon.exceptions.client.GnsClientException;
import java.io.IOException;
import java.net.InetSocketAddress;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Comprehensive functionality test for the GNS.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoveGuidTest {

  private static String ACCOUNT_ALIAS = "support@gns.name"; // REPLACE THIS WITH YOUR ACCOUNT ALIAS
  private static final String PASSWORD = "password";
  private static GnsClient client;
  /**
   * The address of the GNS server we will contact
   */
  private static InetSocketAddress address = null;
  private static GuidEntry masterGuid;

  public RemoveGuidTest() {
    if (client == null) {
      if (System.getProperty("host") != null
              && !System.getProperty("host").isEmpty()
              && System.getProperty("port") != null
              && !System.getProperty("port").isEmpty()) {
        address = new InetSocketAddress(System.getProperty("host"),
                Integer.parseInt(System.getProperty("port")));
      } else {
        address = new InetSocketAddress("127.0.0.1", GNSClientConfig.LNS_PORT);
      }
       try {
        client = new GnsClient(//address, 
                System.getProperty("disableSSL").equals("true"));
      } catch (IOException e) {
        fail("Exception creating client: " + e);
      }
      try {
        masterGuid = GuidUtils.lookupOrCreateAccountGuid(client, ACCOUNT_ALIAS, PASSWORD, true);
      } catch (Exception e) {
        fail("Exception when we were not expecting it: " + e);
      }
    }
  }

  @Test
  public void test_01_RemoveGuidUsingAccount() {
    String testGuidName = "testGUID" + RandomString.randomString(6);
    GuidEntry testGuid = null;
    try {
      testGuid = GuidUtils.registerGuidWithTestTag(client, masterGuid, testGuidName);
    } catch (Exception e) {
      fail("Exception while creating testGuid: " + e);
    }

    System.out.println("testGuid is " + testGuid.toString());

    try {
      client.guidRemove(masterGuid, testGuid.getGuid());
    } catch (Exception e) {
      fail("Exception while removing testGuid (" + testGuid.toString() + "): " + e);
    }
    try {
      client.lookupGuidRecord(testGuid.getGuid());
      fail("Lookup testGuid should have throw an exception.");
    } catch (GnsClientException e) {

    } catch (IOException e) {
      fail("Exception while doing Lookup testGuid: " + e);
    }
  }

  @Test
  public void test_02_RemoveGuid() {
    String testGuidName = "testGUID" + RandomString.randomString(6);
    GuidEntry testGuid = null;
    try {
      testGuid = GuidUtils.registerGuidWithTestTag(client, masterGuid, testGuidName);
    } catch (Exception e) {
      fail("Exception while creating testGuid: " + e);
    }
    try {
      client.guidRemove(testGuid);
    } catch (Exception e) {
      fail("Exception while removing testGuid: " + e);
    }
    try {
      client.lookupGuidRecord(testGuid.getGuid());
      fail("Lookup testGuid should have throw an exception.");
    } catch (GnsClientException e) {

    } catch (IOException e) {
      fail("Exception while doing Lookup testGuid: " + e);
    }
  }

  @Test
  public void test_03_RemoveAccount() {
    try {
      client.accountGuidRemove(masterGuid);
    } catch (Exception e) {
      fail("Exception while removing testGuid: " + e);
    }
    try {
      // this should be using the guid
      client.lookupAccountRecord(ACCOUNT_ALIAS);
      fail("lookupAccountRecord for " + ACCOUNT_ALIAS + " should have throw an exception.");
    } catch (GnsClientException e) {

    } catch (IOException e) {
      fail("Exception while lookupAccountRecord for " + ACCOUNT_ALIAS + " :" + e);
    }
  }
}
