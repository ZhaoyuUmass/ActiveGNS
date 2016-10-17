package edu.umass.cs.gnsclient.client.testing.activecode;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnscommon.AclAccessType;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;

/**
 * This test checks when a GNS user does not remove his ALL_FIELD ACL,
 * what will happen.
 * 
 * <p>The invariant of ACL is:
 * <p>When the whitelist of a field Y.F exists, then a GUID X can read Y.F if and only if X belongs to the whitelist of Y.F
 * <p>When the whitelist of a field Y.F does not exist, then any GUID X can read Y.F 
 * 
 * @author gaozy
 *
 */
public class ActiveACLHelloWorldExample {
	
	private final static int numGuid = 3;
	
	private static GNSClientCommands client = null;
	private static GuidEntry[] entries;	
	private static String ACCOUNT_GUID_PREFIX = "GUID";
	private static String PASSWORD = "";
	private final static String someField = "GUID_0_FIELD";
	private final static String someValue = "YOU_SHOULD_NOT_SEE_THIS_FIELD";
	
	
	
	private static void setupClientsAndGuids() throws Exception {
		client = new GNSClientCommands();
		entries = new GuidEntry[numGuid];
		
		// initialize three GUID
		for (int i=0; i<numGuid; i++){
			try {
				entries[i] = GuidUtils.lookupOrCreateAccountGuid(
						client, ACCOUNT_GUID_PREFIX + i, PASSWORD);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Create 3 GUIDs:GUID_0, GUID_1 and GUID_2");
		
		// initialize the fields for each guid
		client.fieldUpdate(entries[0], someField, someValue);
		client.aclAdd(AclAccessType.READ_WHITELIST, entries[0], someField, entries[2].getGuid());
		
		System.out.println("Update value of field '"+someField+"' for GUID_0 to "
				+someValue+", and add GUID_2 into "+someField+"'s ACL.");
		
	}
	
	
	/**
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	@Test
	public void test_01_RemoteQuery() throws IOException, InterruptedException{	
		
		try {
			setupClientsAndGuids();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(">>>>>>>>>> Testing >>>>>>>>>>");
		
		String response = null;
		try {
			response = client.fieldRead(entries[0].getGuid(), someField, entries[2]);
		} catch (Exception e1) {
			
		}
		assertEquals(response, someValue);
		
		System.out.println("GUID_1 reads the field GUID_0_FIELD of GUID_0");
		try {
			response = client.fieldRead(entries[0].getGuid(), someField, entries[1]);
			fail("GUID_1 should not be able to access to the field GUID_0_FIELD and see the response :\""+response+"\"");
		} catch (Exception e) {
			
		}
		
		System.out.println("Test passed!");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		Result result = JUnitCore.runClasses(ActiveACLHelloWorldExample.class);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.getMessage());
			failure.getException().printStackTrace();
		}
	}
}
