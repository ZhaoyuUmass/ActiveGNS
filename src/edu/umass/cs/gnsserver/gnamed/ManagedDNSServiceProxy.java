package edu.umass.cs.gnsserver.gnamed;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSCommand;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;

/**
 * @author gaozy
 *
 */
public class ManagedDNSServiceProxy implements Runnable {
	
	protected static String RECORD_FIELD = "record";
	protected static String TTL_FIELD = "ttl";
	
	private static GNSClientCommands client;
	private static GuidEntry accountGuid;
	
	private ManagedDNSServiceProxy(){
		try {
			client = new GNSClientCommands();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			accountGuid = GuidUtils.lookupOrCreateAccountGuid(client, "gaozy@cs.umass.edu",
					"password", true);
			deployDomain();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void deployDomain() throws Exception {
		// hardcode this domain
		String domain = "activegns.org.";
		int ttl = 0;
		
		GuidEntry guid = GuidUtils.lookupOrCreateGuid(client, accountGuid, domain);
		
		JSONArray records = new JSONArray();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("conf/activeCode/records")));
		String line = reader.readLine();
		while(line != null){
            records.put(line);
            line = reader.readLine();
        } 
		reader.close();
		
		JSONObject recordObj = new JSONObject();
		recordObj.put(RECORD_FIELD, records);
		recordObj.put(TTL_FIELD, ttl);
				
		client.execute(GNSCommand.fieldUpdate(guid, "A", recordObj));
		System.out.println("Create record for "+domain+" with record "+recordObj);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Domain activegns.org has been deployed");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		new Thread(new ManagedDNSServiceProxy()).start();
	}
}
