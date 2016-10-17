package edu.umass.cs.gnsserver.gnamed;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSCommand;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;

/**
 * @author gaozy
 *
 */
public class ManagedDNSServiceProxy implements Runnable {
	
	protected static String RECORD_FIELD = "record";
	protected static String TTL_FIELD = "ttl";
	
	private static GNSClientCommands client;
	private static GuidEntry accountGuid;
	
	private final static int default_ttl = 30;
	private final static String DOMAIN = "activegns.org.";
	
	private final static ExecutorService executor = Executors.newFixedThreadPool(10);
	
	private ManagedDNSServiceProxy(){
		/*
		try {
			client = new GNSClientCommands();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			accountGuid = GuidUtils.lookupOrCreateAccountGuid(client, "zhaoyu",
					"password", true);
			deployDomain();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
	private static void deployDomain() throws Exception {
		String domain = DOMAIN;
		int ttl = 0;
		
		GuidEntry guid = GuidUtils.lookupOrCreateGuid(client, accountGuid, domain);
		
		List<String> records = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("conf/activeCode/records")));
		String line = reader.readLine();
		while(line != null){
            records.add(line);
            line = reader.readLine();
        } 
		reader.close();
		
		System.out.println("The record list is "+records);
		updateRecord(guid, records, ttl);
		System.out.println("Create record for "+domain);
	}
	
	private static JSONObject recordToCreate(List<String> ips, int ttl) {
		JSONObject recordObj = new JSONObject();
		JSONArray records = new JSONArray();
		for (String ip:ips){
			records.put(ip);
		}
		
		try {
			recordObj.put(RECORD_FIELD, records);
			recordObj.put(TTL_FIELD, ttl);
		} catch (JSONException e) {
			
		}
		return recordObj;
	}
	
	private static boolean updateRecord(GuidEntry entry, List<String> ips, int ttl){
		System.out.println("Ready to update record for "+entry);
		JSONObject recordObj = recordToCreate(ips, ttl);
		try {
			client.execute(GNSCommand.fieldUpdate(entry, "A", recordObj));
		} catch (ClientException | IOException e) {
			e.printStackTrace();
			// The update failed
			return false;
		}
		return true;
	}
	
	private static GuidEntry getGuidEntryForDomain(String domain){
		return null;
	}
	
	private static void createOrUpdateDomainRecord(){
		
	}
	
	@Override
	public void run() {
		System.out.println("DNS proxy starts running ...");
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(9090);
		} catch (IOException e) {
			
		}
		try{
			while(true){
				try {
					Socket socket = listener.accept();
					executor.execute(new UpdateTask(socket));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}finally{
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	final class UpdateTask implements Runnable{
		Socket sock;
		
		UpdateTask(Socket sock){
			this.sock = sock;
		}
		
		@Override
		public void run() {
			System.out.println("run task...");
			try {
				BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				String queryString = input.readLine();
				JSONObject query = new JSONObject(queryString);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally{
				try {
					sock.close();
				} catch (IOException e) {
					
				}
			}
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		new Thread(new ManagedDNSServiceProxy()).start();
	}
}
