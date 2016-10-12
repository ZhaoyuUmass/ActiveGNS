package edu.umass.cs.gnsclient.client.testing.activecode;

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
import edu.umass.cs.gnscommon.AclAccessType;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ActiveCode;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.MetaDataTypeName;

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
	private static String targetGuid = "";
	private final static String someField = "someField";
	private final static int someValue = 1;
	private final static String depthField = "depthField";
	private final static String depthResult = "Depth test succeeds";
	
	
	
	private static void setupClientsAndGuids() throws Exception{
		client = new GNSClientCommands();
		entries = new GuidEntry[2];
		
		// initialize two GUID
		for (int i=0; i<numGuid; i++){
			entries[i] = GuidUtils.lookupOrCreateAccountGuid(
					client, ACCOUNT_GUID_PREFIX + i, PASSWORD);
		}
		System.out.println("Create 2 GUIDs:GUID_1 and GUID_2");
		
		// initialize the fields for each guid
		client.fieldUpdate(entries[0], someField, someValue);
		System.out.println("Update value of field '"+someField+"' for GUID_1 to "+someValue);
		client.fieldUpdate(entries[1], depthField, depthResult);
		System.out.println("Update value of field '"+depthField+"' for GUID_2 to "+depthResult);
		
		// set the target guid to the second one and put it into the code
		targetGuid = entries[numGuid-1].getGuid();
		
		
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
		String codeFile = System.getProperty("activeReadCode");
		if(codeFile == null)
			codeFile = "scripts/activeCode/remoteReadQuery.js";
		
		String code = new String(Files.readAllBytes(Paths.get(codeFile)));
		String read_code = code.replace("//substitute this line with the targetGuid", "var targetGuid=\""+targetGuid+"\";");
		
		System.out.println("The code on READ action of GUID_0 is:\n"+read_code);
		
		
		
		String response = null;
		try {
			//client.aclAdd(AclAccessType.READ_WHITELIST, entries[0], someField, entries[0].getGuid());
			response = client.fieldRead(entries[0].getGuid(), someField, entries[1]); //client.fieldRead(entries[0], someField);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("The response is "+response);
		
		//test whether remote query can bypass the ACL check	
		/*
		try {
			client.activeCodeSet(entries[0].getGuid(), ActiveCode.READ_ACTION, read_code, entries[0]);			
		} catch (ClientException e) {
			e.printStackTrace();
		}
		Thread.sleep(1000);
		
		String response = null;
		try {
			response = client.fieldRead(entries[0], someField);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(depthResult, response);		
		System.out.println("Depth query test(a read followed by a read) succeeds!");
		*/
		
		try {
			cleanup();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private static void cleanup() throws Exception{
		try {
			client.activeCodeClear(entries[0].getGuid(), ActiveCode.READ_ACTION, entries[0]);
			client.activeCodeClear(entries[1].getGuid(), ActiveCode.READ_ACTION, entries[1]);
			client.activeCodeClear(entries[0].getGuid(), ActiveCode.WRITE_ACTION, entries[0]);
			client.activeCodeClear(entries[1].getGuid(), ActiveCode.WRITE_ACTION, entries[1]);
			client.activeCodeClear(entries[0].getGuid(), ActiveCode.ACL_ACTION, entries[0]);
			client.activeCodeClear(entries[1].getGuid(), ActiveCode.ACL_ACTION, entries[1]);
			
		} catch (ClientException e) {
			e.printStackTrace();
		}
				
		client.close();
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
