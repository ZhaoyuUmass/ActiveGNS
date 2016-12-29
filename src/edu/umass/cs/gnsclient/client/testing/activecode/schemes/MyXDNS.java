package edu.umass.cs.gnsclient.client.testing.activecode.schemes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSCommand;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnscommon.exceptions.client.EncryptionException;
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
		
		System.out.println("records:"+records+"\nweight:"+weights);
		
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
		
		final String ACCOUNT_GUID_SUFFIX = ".activegns.org.";
		final String ACCOUNT_GUID = name + ACCOUNT_GUID_SUFFIX;
		final String PASSWORD = "";
		System.out.println("ACCOUNT_GUID:"+ACCOUNT_GUID);
		
		
		// create an account
		GuidEntry entry = null;
		String guid_file = "guid";
		if(System.getProperty("guid_file")!=null){
			guid_file = System.getProperty("guid_file");
		}
		try {
			ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(guid_file)));
			entry = new GuidEntry(input);
			input.close();
		} catch (IOException | EncryptionException e) {
			// the file does not exist, create a new guid
		}
		if(entry == null){
			entry = GuidUtils.lookupOrCreateAccountGuid(client, ACCOUNT_GUID, PASSWORD);
			ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(new File("guid")));
			entry.writeObject(output);
			output.flush();
			output.close();
		}

		/**
		 *  create fields used in this test: weight, test_ip, A record
		 *  update code
		 */
		// 1. create A record		
		JSONObject recordObj = new JSONObject();
		JSONArray arr = new JSONArray();
		for(String ip:records){
			arr.put(ip);
		}
		recordObj.put("record", arr);
		recordObj.put("ttl", 30);		
		client.execute(GNSCommand.fieldUpdate(entry, "A", recordObj));
		
		// 2. update weight
		arr = new JSONArray();
		for(Double weight:weights){
			arr.put(weight);
		}
		client.execute(GNSCommand.fieldUpdate(entry, "weight", arr));
		
		// 3. update test ip
		client.execute(GNSCommand.fieldUpdate(entry, "testIp", client_ip));
		
		// 4. update code
		client.activeCodeSet(entry.getGuid(), ActiveCode.READ_ACTION, code, entry);
		
		System.exit(0);
	}
}
