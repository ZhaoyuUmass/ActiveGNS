package edu.umass.cs.gnsclient.client.testing.activecode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ActiveCode;

public class SetupChainCode {
	
	private static GNSClientCommands client = null;
	private static GuidEntry[] entries;
	private static String ACCOUNT_GUID_PREFIX = "ACCOUNT_GUID_";
	private static String PASSWORD = "";
	
	private final static String targetGuidField = "someField";
	private final static String successResult = "Depth query succeeds!";
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		int depth = 1;
		if(System.getProperty("depth")!=null){
			depth = Integer.parseInt(System.getProperty("depth"));
		}
		
		String codeFile = "scripts/ativeCode/depth.js";
		if(System.getProperty("codeFile")!=null){
			codeFile = System.getProperty("codeFile");
		}
		
		String code = new String(Files.readAllBytes(Paths.get(codeFile)));
		
		client = new GNSClientCommands();		
		entries = new GuidEntry[depth];
		
		for (int i=0; i<depth; i++){
			entries[i] = GuidUtils.lookupOrCreateAccountGuid(
					client, ACCOUNT_GUID_PREFIX + i, PASSWORD);
		}
		
		String[] nextGuid = new String[depth];
		for(int i=0; i<depth-1; i++){
			nextGuid[i] = entries[i+1].getGuid();
		}
		nextGuid[depth-1] = successResult;
		
		for(int i=0; i<depth; i++){
			client.activeCodeClear(entries[i].getGuid(), ActiveCode.ON_READ, entries[i]);
		}
		
		for(int i=0; i<depth; i++){
			client.fieldUpdate(entries[i], targetGuidField, nextGuid[i]);
		}
		
		for(int i=0; i<depth; i++){
			client.activeCodeSet(entries[i].getGuid(), ActiveCode.ON_READ, code, entries[i]);
		}
		
		String response = client.fieldRead(entries[0], targetGuidField);
		
		assert(response.equals(successResult));
		System.out.println("Response is "+response+", depth query code chain has been successfully set up!");
	}
}
