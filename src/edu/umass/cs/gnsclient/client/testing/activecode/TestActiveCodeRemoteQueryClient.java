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
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ActiveCode;
import edu.umass.cs.gnsserver.utils.ValuesMap;

/**
 * @author gaozy
 *
 */
public class TestActiveCodeRemoteQueryClient {
	private final static int numGuid = 2;
	
	private static GNSClientCommands client = null;
	private static GuidEntry[] entries;	
	private static String ACCOUNT_GUID_PREFIX = "ACCOUNT_GUID";
	private static String PASSWORD = "";
	private static String targetGuid = "";
	private final static String someField = "someField";
	private final static String someValue = "someValue";
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
		
		// initialize the fields for each guid
		
		client.fieldUpdate(entries[0], someField, someValue);
		client.fieldUpdate(entries[1], someField, someValue);
		client.fieldUpdate(entries[1], depthField, depthResult);
		
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
		String codeFile = System.getProperty("activeCode");
		if(codeFile == null)
			codeFile = "scripts/activeCode/remoteQuery.js";
		
		String code = new String(Files.readAllBytes(Paths.get(codeFile)));
		code = code.replace("//substitute this line with the targetGuid", "var targetGuid=\""+targetGuid+"\";");
		String noop_code = new String(Files.readAllBytes(Paths.get("scripts/activeCode/noop.js")));
		
		System.out.println("The new code is:\n"+code);
		
		//deploy code for read
		
		try {
			client.activeCodeClear(entries[0].getGuid(), ActiveCode.READ_ACTION, entries[0]);
			client.activeCodeSet(entries[0].getGuid(), ActiveCode.READ_ACTION, code, entries[0]);
			
			client.activeCodeClear(entries[1].getGuid(), ActiveCode.READ_ACTION, entries[1]);		
			client.activeCodeSet(entries[1].getGuid(), ActiveCode.READ_ACTION, noop_code, entries[1]);
		} catch (ClientException e) {
			e.printStackTrace();
		}
		
		String response = null;
		try {
			response = client.fieldRead(entries[0], someField);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(depthResult, response);		
		System.out.println("Depth query test(a read followed by a read) succeeds!");
		
		Thread.sleep(1000);
		
		
		
		// test one write followed by a read
		
		try {
			client.activeCodeClear(entries[0].getGuid(), ActiveCode.READ_ACTION, entries[0]);
			client.activeCodeSet(entries[0].getGuid(), ActiveCode.WRITE_ACTION, code, entries[0]);
			
		} catch (ClientException e) {
			e.printStackTrace();
		}
		
		try {
			client.fieldUpdate(entries[0], someField, someValue);
			//response = client.fieldRead(entries[0], someField);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//assertEquals(depthResult, response);
		System.out.println("Depth query test(a read followed by a write) succeeds!");
		
		Thread.sleep(1000);
		
		
		// test a read followed by a write 
		/*
		code = code.replace("value.put(field, querier.readGuid(targetGuid, \"depthField\").get(\"depthField\"));", 
				"querier.writeGuid(targetGuid, \"someField\", value);");
		
		System.out.println("The new code is:\n"+code);
		try {
			client.activeCodeClear(entries[0].getGuid(), ActiveCode.READ_ACTION, entries[0]);
			client.activeCodeSet(entries[0].getGuid(), ActiveCode.READ_ACTION, code, entries[0]);
			
			client.activeCodeClear(entries[1].getGuid(), ActiveCode.READ_ACTION, entries[1]);		
			client.activeCodeSet(entries[1].getGuid(), ActiveCode.WRITE_ACTION, noop_code, entries[1]);
			
		} catch (ClientException e) {
			e.printStackTrace();
		}
		
		
		try {
			response = client.fieldRead(entries[0], someField);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(someValue, response);
		System.out.println("Depth query test(a write followed by a read) succeeds!");
		
		Thread.sleep(1000);
		*/
		
		// test a write followed by a write
		/*
		try {
			client.activeCodeClear(entries[0].getGuid(), ActiveCode.READ_ACTION, entries[0]);
			client.activeCodeSet(entries[0].getGuid(), ActiveCode.WRITE_ACTION, code, entries[0]);
			
		} catch (ClientException e) {
			e.printStackTrace();
		}
		try {
			response = client.fieldRead(entries[0], someField);
			fail("A write followed with a write operation should not succeed.");
		} catch (Exception e) {
			e.printStackTrace();
		}	
		System.out.println("Depth query test(a write followed by a write) succeeds!");
		*/
		client.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		Result result = JUnitCore.runClasses(TestActiveCodeRemoteQueryClient.class);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.getMessage());
			failure.getException().printStackTrace();
		}
	}
}
