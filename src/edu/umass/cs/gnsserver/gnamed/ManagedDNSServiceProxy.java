package edu.umass.cs.gnsserver.gnamed;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ActiveCode;

/**
 * @author gaozy
 *
 */
public class ManagedDNSServiceProxy implements Runnable {
	
	protected final static String RECORD_FIELD = "record";
	protected final static String TTL_FIELD = "ttl";
	
	private final static String ACTION_FIELD = "action";
	private final static String GUID_FIELD = "guid";
	private final static String CODE_FIELD = "code";
	private final static String USERNAME_FIELD = "username";
	
	private final static String A_RECORD_FIELD = "A";
	
	private enum Actions {
	    CREATE("create"),
	    UPDATE("update"),
	    // remove the code
	    REMOVE("remove"),
	    // delete the record
	    DELETE("delete")
	    ;

	    private final String text;

	    /**
	     * @param text
	     */
	    private Actions(String text) {
	        this.text = text;
	    }

	    /**
	     * @see java.lang.Enum#toString()
	     */
	    @Override
	    public String toString() {
	        return text;
	    }
	}
	
	private static GNSClientCommands client;
	private static GuidEntry accountGuid;
	
	private final static int default_ttl = 30;
	private final static String DOMAIN = "activegns.org.";
	
	private final static ExecutorService executor = Executors.newFixedThreadPool(10);
	
	private ManagedDNSServiceProxy(){
		try {
			client = new GNSClientCommands();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			accountGuid = GuidUtils.lookupOrCreateAccountGuid(client, "gaozy",
					"password", true);
			deployDomain();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	private static void updateRecord(GuidEntry entry, List<String> ips, int ttl){
		System.out.println("Ready to update record for "+entry);
		JSONObject recordObj = recordToCreate(ips, ttl);
		try {
			client.execute(GNSCommand.fieldUpdate(entry, A_RECORD_FIELD, recordObj));
		} catch (ClientException | IOException e) {
			e.printStackTrace();
			// The update failed			
		}	
	}
	
	private static void updateCode(GuidEntry entry, String code){
		try {
			client.activeCodeSet(entry.getGuid(), ActiveCode.READ_ACTION, code, entry);
		} catch (ClientException | IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static GuidEntry createGuidEntryForDomain(String domain) throws Exception{		
		return GuidUtils.lookupOrCreateGuid(client, accountGuid, domain);
	}
	
	private static void updateRecordAndCode(GuidEntry entry, String record, String code){
		assert(!record.equals("")):"record can't be enpty when update";
		// Update record
		List<String> ips = Arrays.asList(record.split("\\n"));
		updateRecord(entry, ips, default_ttl);
		
		// Update code
		if(!code.equals("")){
			updateCode(entry, code);
		}else{
			removeCode(entry);
		}
	}
	
	private static void deleteRecord(GuidEntry entry){
		// clear the code
		removeCode(entry);
		
		try {
			client.execute(GNSCommand.fieldRemove(entry, A_RECORD_FIELD));
		} catch (ClientException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void removeCode(GuidEntry entry){
		try {
			client.activeCodeClear(entry.getGuid(), ActiveCode.READ_ACTION, entry);
		} catch (ClientException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String serializeGuid(GuidEntry entry){
		try {
		     ByteArrayOutputStream bo = new ByteArrayOutputStream();
		     ObjectOutputStream so = new ObjectOutputStream(bo);
		     entry.writeObject(so);
		     so.flush();
		     so.close();
		     return Base64.getEncoder().encodeToString(bo.toByteArray());
		 } catch (Exception e) {
			 e.printStackTrace();
		     return null;
		 }
	}
	
	private static GuidEntry deserializeGuid(String key) {
		try {
		     byte b[] = Base64.getDecoder().decode(key); 
		     ObjectInputStream si = new ObjectInputStream(new ByteArrayInputStream(b));
		     GuidEntry guid = new GuidEntry(si);
		     si.close();
		     return guid;
		 } catch (Exception e) {
		     e.printStackTrace();
		     return null;
		 }
	}
	
	private static JSONObject handleRequest(JSONObject req){
		JSONObject result = new JSONObject();
		try {
			Actions action = Actions.valueOf(req.getString(ACTION_FIELD).toUpperCase());
			switch(action){
				case CREATE:{
						String username = req.getString(USERNAME_FIELD);
						String subdomain = username+"."+DOMAIN;
						GuidEntry entry = createGuidEntryForDomain(subdomain);
						String guid = serializeGuid(entry);
						result.put(GUID_FIELD, guid);
				}
				break;
				case UPDATE:{
					String record = req.getString(RECORD_FIELD);
					GuidEntry guid = deserializeGuid(req.getString(GUID_FIELD));
					String code = req.getString(CODE_FIELD);
					updateRecordAndCode(guid, record, code);
				}
				break;
				case REMOVE:{
					GuidEntry guid = deserializeGuid(req.getString(GUID_FIELD));
					removeCode(guid);
				}
				break;
				case DELETE:{
					GuidEntry guid = deserializeGuid(req.getString(GUID_FIELD));
					deleteRecord(guid);
				}
				break;
				default:
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return result;
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
			try {
				BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				String queryString = input.readLine();
				JSONObject request = new JSONObject(queryString);
				System.out.println("Recived query from frontend:"+request.toString());
				JSONObject response = handleRequest(request);
				if(response != null){
					PrintWriter out = new PrintWriter(sock.getOutputStream());
					out.println(response.toString());
					out.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally{
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
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
