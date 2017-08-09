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

import edu.umass.cs.gnsclient.client.GNSClient;
import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnscommon.exceptions.client.EncryptionException;

/**
 * @author gaozy
 *
 */
public class Donar {
	
	private final static String name = "donar";
	
	// The continent information, get initial coordinates from Google
	private final static String[] continents = {"EU", "AS", "NA", "SA", "AF", "OC", "AN" };
	private final static Double[] latitudes = {54.5260, 34.0479, 54.5260, -8.7832, -8.7832, -22.7359, -82.8628 };
	private final static Double[] longitudes = {15.2551, 100.6197, -105.2551, -55.4915, 34.5085, 140.0188, 135.0000 };
	
	private static JSONArray formJsonArray(List<?> l){
		JSONArray arr = new JSONArray();
		for(int i=0; i<l.size(); i++){
			arr.put(l.get(i));
		}
		return arr;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String readFile = "scripts/activeCode/scheme/donar/DONAR-read.js";
		String writeFile = "scripts/activeCode/scheme/donar/DONAR-write.js";
		String libFile = "scripts/activeCode/scheme/donar/numeric-1.2.6.js";
		final String readCode = new String(Files.readAllBytes(Paths.get(readFile)));
		final String writeCode = new String(Files.readAllBytes(Paths.get(writeFile)));
		final String libCode = new String(Files.readAllBytes(Paths.get(libFile)));
		
		String recordFile = "conf/activeCode/donar";
		if(System.getProperty("recordField")!=null){
			recordFile = System.getProperty("recordFile");
		}
				
		// Fields related to replica
		List<String> records = new ArrayList<String>();
		List<Double> w = new ArrayList<Double>();
		List<Double> epsilon = new ArrayList<Double>();
		List<Double> lambda = new ArrayList<Double>();
		List<Double> p = new ArrayList<Double>();
		List<Integer> decision = new ArrayList<Integer>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(recordFile)));
		String line = reader.readLine();
		while(line != null){
			if(!line.startsWith("#")){
				String[] arr = line.split(" ");
				records.add(arr[0]);
				w.add(Double.parseDouble(arr[1]));
				epsilon.add(Double.parseDouble(arr[2]));
				lambda.add(Double.parseDouble(arr[3]));
				p.add(Double.parseDouble(arr[4]));
				decision.add(0);
			}				
            line = reader.readLine();
        } 
		reader.close();
		System.out.println("records:"+records+"\nweight:"+w+"\nepsilon:"+epsilon+"\nlambda:"+lambda);
		
		String regionFile = "conf/activeCode/donar-regions";
		if(System.getProperty("regionField")!=null){
			recordFile = System.getProperty("regionFile");
		}
		
		// Fields related to client regions	
		
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(regionFile)));
		line = reader.readLine();
		while(line != null){
			if(!line.startsWith("#")){
				String[] arr = line.split(" ");
				
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
		GNSClient client = null;
		try {
			client = new GNSClient();
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
		
		
		JSONObject json = new JSONObject();
		
		
		// 1. create A record		
		JSONObject recordObj = new JSONObject();
		JSONArray arr = formJsonArray(records);
		recordObj.put("record", arr);
		recordObj.put("ttl", 30);		
		json.put("A", recordObj);
		
		// 2. add client region information
		JSONObject regionInfo = new JSONObject();
		for(int i=0; i<continents.length; i++){
			JSONObject loc = new JSONObject();
			loc.put("latitude", latitudes[i]);
			loc.put("longitude", longitudes[i]);
			regionInfo.put(continents[i], loc);
		}
		json.put("regionInfo", regionInfo);
		
		/**
		 *  3. add weight, epsilon, lambda, and p defined in conf/activeCode/donar. 
		 *  Also update the decision field to be used by read active code.
		 *  For the meaning of these parameters, see the details in DONAR paper
		 */
		
		json.put("w", formJsonArray(w));
		json.put("epsilon", formJsonArray(epsilon));
		json.put("lambda", formJsonArray(lambda));
		json.put("p", formJsonArray(p));
		json.put("decision", decision);
		
		/**
		 * 4. 
		 */
	}
}
