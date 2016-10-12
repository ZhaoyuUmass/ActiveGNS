package edu.umass.cs.gnsclient.client.testing.activecode;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;

/**
 * @author gaozy
 *
 */
public class ActiveACLHelloWorldExample {
	
	private final static int numGuid = 2;
	
	private static GNSClientCommands client = null;
	private static GuidEntry[] entries;	
	private static String ACCOUNT_GUID_PREFIX = "GUID";
	private static String PASSWORD = "";
	private final static String someField = "GUID_0_FIELD";
	private final static String someValue = "YOU_SHOULD_NOT_SEE_THIS_FIELD";
	
	
	
	private static void setupClientsAndGuids() throws Exception{
		client = new GNSClientCommands();
		entries = new GuidEntry[2];
		
		// initialize two GUID
		for (int i=0; i<numGuid; i++){
			entries[i] = GuidUtils.lookupOrCreateAccountGuid(
					client, ACCOUNT_GUID_PREFIX + i, PASSWORD);
		}
		System.out.println("Create 2 GUIDs:GUID_0 and GUID_1");
		
		// initialize the fields for each guid
		client.fieldUpdate(entries[0], someField, someValue);
		System.out.println("Update value of field '"+someField+"' for GUID_0 to "+someValue);
		
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
