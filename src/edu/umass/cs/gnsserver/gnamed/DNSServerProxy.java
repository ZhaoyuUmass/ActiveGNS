package edu.umass.cs.gnsserver.gnamed;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.json.JSONArray;
import org.xbill.DNS.Message;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSCommand;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.gnscommon.utils.ThreadUtils;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ActiveCode;

/**
 * This class implements a simple proxy between GNS and 
 * DNS. This proxy listens on the port 53 for DNS. 
 * It processes DNS request and translates the request
 * to GNS request and fetch the record from GNS.
 * 
 * @author gaozy
 *
 */
public class DNSServerProxy implements Runnable {
	//private SimpleResolver resolver; 
	private static GNSClientCommands client;
	private static GuidEntry accountGuid;
	private static GuidEntry guid;
	private final DatagramSocket sock;
	private final ExecutorService executor;
	
	private DNSServerProxy(boolean withCode, String codeFile) throws IOException{
		client = new GNSClientCommands();
		sock = new DatagramSocket(53, Inet4Address.getByName("0.0.0.0"));
		executor = Executors.newFixedThreadPool(5);
		try {
			deployDomain();
			System.out.println("The domain has been successfully created!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(withCode){
			final String code = new String(Files.readAllBytes(Paths.get(codeFile)));
			try {
				deployCode(code);
			} catch (ClientException e) {
				e.printStackTrace();
			}
		}
	}
	
	//FIXME: this is just used for a demo, everything is hard-coded
	private static void deployDomain() throws Exception{
		accountGuid = GuidUtils.lookupOrCreateAccountGuid(client, "gaozy@cs.umass.edu",
				"password", true);
		
		String domain = "activegns.org.";
		guid = GuidUtils.lookupOrCreateGuid(client, accountGuid, domain);
		JSONArray records = new JSONArray();
		records.put("1.1.1.1");
		records.put("2.2.2.2");
		
		System.out.println("Create record for "+domain+" with record "+records);
		client.execute(GNSCommand.fieldUpdate(guid, "A", records));
	}
	
	private static void deployCode(String code) throws ClientException, IOException {
		client.activeCodeClear(guid.getGuid(), ActiveCode.READ_ACTION, guid);
		client.activeCodeSet(guid.getGuid(), ActiveCode.READ_ACTION, code, guid);
	}
	
	@Override
	public void run() {
		 System.out.println("Start running TCP proxy ... ");
		 while (true) {
		      try {
		        final short udpLength = 512;
		        while (true) {
		          byte[] incomingData = new byte[udpLength];
		          DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
		          // Read the incoming request
		          incomingPacket.setLength(incomingData.length);
		          try {
		            sock.receive(incomingPacket);
		          } catch (InterruptedIOException e) {
		            continue;
		          }
		          Message query = null;
		          try {
		              query = new Message(incomingData);
		          } catch (IOException e) {
		              // Send out an error response.
		              sendResponse(NameResolution.formErrorMessage(incomingData).toWire(), incomingPacket);
		              continue;
		          }
		          
		          //System.out.println("Query is "+query.toString());
		          
		          Future<Message> task = executor.submit(new GNSLookupTask(client, guid, query));
		          Message resp = task.get(1000, TimeUnit.MILLISECONDS);
		          //System.out.println("The response is "+resp.toString());
		          
		          sendResponse(resp.toWire(), incomingPacket);
		        }
		      } catch (IOException e) {
		        NameResolution.getLogger().log(Level.SEVERE, 
		                "Error in UDP Server (will sleep for 3 seconds and try again): {0}", e);
		        ThreadUtils.sleep(3000);
		      } catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void sendResponse(byte[] responseBytes, DatagramPacket incomingPacket) {
	    DatagramPacket outgoingPacket = new DatagramPacket(responseBytes, responseBytes.length, incomingPacket.getAddress(), incomingPacket.getPort());
	    try {
	      sock.send(outgoingPacket);
	      NameResolution.getLogger().log(Level.FINE,
	              "Response sent to {0} {1}", new Object[]{incomingPacket.getAddress().toString(),
	                incomingPacket.getPort()});
	    } catch (IOException e) {
	      NameResolution.getLogger().log(Level.SEVERE, "Failed to send response{0}", e);
	    }
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{
		boolean withCode = false;
		String codeFile = null;
		if(args.length > 0){
			withCode = true;
			codeFile = args[0];
		}
		DNSServerProxy proxy = new DNSServerProxy(withCode, codeFile);
		new Thread(proxy).start();
	}
}
