package edu.umass.cs.gnsclient.client.testing.activecode.schemes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSCommand;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ActiveCode;

/**
 * @author gaozy
 *
 */
public class MyXDNS {
	
	private final static String name = "myxdns";
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String codeFile = "scripts/activeCode/scheme/myxdns.js";
		if(System.getProperty("codeFile")!=null){
			codeFile = System.getProperty("codeFile");
		}
		final String code = new String(Files.readAllBytes(Paths.get(codeFile)));
		
		String recordFile = "conf/activeCode/myxdns";
		if(System.getProperty("recordField")!=null){
			recordFile = System.getProperty("recordFile");
		}
		
		List<String> records = new ArrayList<String>();
		List<Double> weights = new ArrayList<Double>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(recordFile)));
		String line = reader.readLine();
		while(line != null){
			if(!line.startsWith("#")){
				String[] arr = line.split(" ");
				records.add(arr[0]);
				weights.add(Double.parseDouble(arr[1]));
			}
				
            line = reader.readLine();
        } 
		reader.close();
		
		
		String client_ip = args[0];
		try {
			InetAddress.getByName(client_ip);
		} catch (UnknownHostException e) {
			System.out.println("The ip address being entered is invalid, please enter an IPv4 address.");
			System.exit(0);
		}
				
		// create a client
		GNSClientCommands client = null;
		try {
			client = new GNSClientCommands();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		final String ACCOUNT_GUID_SUFFIX = ".activegns.org";
		final String ACCOUNT_GUID = name + ACCOUNT_GUID_SUFFIX;
		final String PASSWORD = "";
		
		// create an account
		final edu.umass.cs.gnsclient.client.util.GuidEntry entry = GuidUtils.lookupOrCreateAccountGuid(client, ACCOUNT_GUID, PASSWORD);

		/**
		 *  create fields used in this test: weight, test_ip, A record
		 *  update code
		 */
		// 1. create A record		
		JSONObject recordObj = new JSONObject();
		recordObj.put("record", records);
		recordObj.put("ttl", 30);		
		client.execute(GNSCommand.fieldUpdate(entry, "A", recordObj));
		
		// 2. update weight
		client.execute(GNSCommand.fieldUpdate(entry, "weight", weights));
		
		// 3. update test ip
		client.execute(GNSCommand.fieldUpdate(entry, "test_ip", client_ip));
		
		// 4. update code
		client.activeCodeSet(entry.getGuid(), ActiveCode.READ_ACTION, code, entry);
		//GNSCommand.activeCodeSet(entry.getGuid(), ActiveCode.READ_ACTION, code, entry);
	}
}
