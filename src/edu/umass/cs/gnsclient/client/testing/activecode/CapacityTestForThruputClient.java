package edu.umass.cs.gnsclient.client.testing.activecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import edu.umass.cs.gigapaxos.testing.TESTPaxosConfig.TC;
import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSClientConfig.GNSCC;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.utils.Config;
import edu.umass.cs.utils.Util;

/**
 * @author gaozy
 *
 */
public class CapacityTestForThruputClient {
	
final static Random random = new Random();
	
	private static int NUM_THREAD = 120;
	
	private static int numClients;
	private static String someField = "someField";
	private static String someValue = "someValue";
	
	private static boolean withMalicious;
	private static boolean withSignature;
	private static boolean isRead;
	
	private static double fraction;
	private static int thres; // After which index to start malicious client
	
	private static GuidEntry entry;
	private static GNSClientCommands[] clients;
	private static GuidEntry malEntry;
	
	private static ExecutorService executor;
	
	
	private static int numFinishedReads = 0;
	private static long lastReadFinishedTime = System.currentTimeMillis();

	synchronized static void incrFinishedReads() {
		numFinishedReads++;
		lastReadFinishedTime = System.currentTimeMillis();
	}
	
	private static int numFailedReads = 0;
	synchronized static void incrFailedReads(){
		numFailedReads++;
		lastReadFinishedTime = System.currentTimeMillis();
	}

	/**
	 * @throws Exception
	 */
	public static void setup() throws Exception {
		numClients = 10;
		if(System.getProperty("numClients") != null){
			numClients = Integer.parseInt(System.getProperty("numClients"));
		}
		
		someField = "someField";
		if(System.getProperty("field")!=null){
			someField = System.getProperty("field");
		}
		
		withMalicious = false;
		if(System.getProperty("withMalicious")!= null){
			withMalicious = Boolean.parseBoolean(System.getProperty("withMalicious"));
		}
		
		withSignature = false;
		if(System.getProperty("withSigniture")!= null){
			withSignature = Boolean.parseBoolean(System.getProperty("withSigniture"));
		}
				
		isRead = true;
		if(System.getProperty("isRead")!=null){
			isRead = Boolean.parseBoolean(System.getProperty("isRead"));
		}
		
		if(System.getProperty("numThread")!=null){
			NUM_THREAD = Integer.parseInt(System.getProperty("numThread"));
		}
		
		String keyFile = "guid";
		if(System.getProperty("keyFile")!= null){
			keyFile = System.getProperty("keyFile");
		}
		ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(keyFile)));
		entry = new GuidEntry(input);
		input.close();
		assert(entry != null);
		
		String malKeyFile = "mal_guid";
		if(System.getProperty("malKeyFile") != null){
			malKeyFile = System.getProperty("malKeyFile");
		}
		if(new File(malKeyFile).exists()){
			input = new ObjectInputStream(new FileInputStream(new File(malKeyFile)));
			malEntry = new GuidEntry(input);
		}
		
		fraction = 0.0;
		if(System.getProperty("fraction")!=null){
			fraction = Double.parseDouble(System.getProperty("fraction"));
		}
		thres = numClients - ((Number) (fraction*numClients)).intValue();
		
		
		
		executor = Executors.newFixedThreadPool(NUM_THREAD);
		
		clients = new GNSClientCommands[numClients];
		for (int i=0; i<numClients; i++){
			clients[i] = new GNSClientCommands();
		}
	}
	
	private static void blockingRead(int clientIndex, GuidEntry guid, boolean signed) {
		executor.submit(new Runnable() {
			public void run() {
				try {
					if (signed)
						clients[clientIndex].fieldRead(guid, someField);
					else
						clients[clientIndex].fieldRead(guid.getGuid(),
								someField, null);					
				} catch (Exception e) {
					//e.printStackTrace();
				}
				incrFinishedReads();
			}
		});
	}
	
	
	private static void blockingWrite(int clientIndex, GuidEntry guid, boolean signed) {
		executor.submit(new Runnable() {
			public void run() {
				try {
					if (signed)
						clients[clientIndex].fieldUpdate(guid, someField, someValue);
					else
						clients[clientIndex].fieldUpdate(guid, someField, someValue);					
				} catch (Exception e) {
					//e.printStackTrace();
				}
				incrFinishedReads();
			}
		});
	}
	
	private static void read(GNSClientCommands client, GuidEntry guid, boolean signed, boolean mal) {		
		try {
			if(signed)
				client.fieldRead(guid, someField);
			else
				client.fieldRead(guid.getGuid(),
						someField, null);
		} catch (Exception e) {
			incrFailedReads();
			return;
		}
		incrFinishedReads();
	}
	
	
	private static void write(GNSClientCommands client, GuidEntry guid, boolean signed, boolean mal){		
		try {
			client.fieldUpdate(guid, someField, someValue);
		} catch (ClientException | IOException | JSONException e) {
			incrFailedReads();
			return;
		}
		incrFinishedReads();
	}
	
	/**
	 * @throws InterruptedException
	 */
	public static void thru_test() throws InterruptedException{
		
		String operation = (isRead?"read":"write");
		String signed = (withSignature?"signed":"unsigned");
		
		int numReads = Math.min(
				Config.getGlobalBoolean(GNSCC.ENABLE_SECRET_KEY) ? Integer.MAX_VALUE : 10000,
				Config.getGlobalInt(TC.NUM_REQUESTS));
		long t = System.currentTimeMillis();
		for (int i = 0; i < numReads; i++) {
			if(isRead){
				blockingRead(i % numClients, entry, withSignature);
			}else{
				blockingWrite(i % numClients, entry, withSignature);
			}
		}
		System.out.print("[total_"+signed+"_"+operation+"=" + numReads+": ");
		int lastCount = 0;
		while (numFinishedReads < numReads) {
			if(numFinishedReads>lastCount)  {
				lastCount = numFinishedReads;
				System.out.print(numFinishedReads + "@" + Util.df(numFinishedReads * 1.0 / (lastReadFinishedTime - t))+"K/s ");
			}
			Thread.sleep(1000);
		}
		System.out.println("] ");
		
		
		System.out.println("parallel_"+signed+"_"+operation+"_rate="
				+ Util.df(numReads * 1.0 / (lastReadFinishedTime - t))
				+ "K/s");
	}
	
	/**
	 * 
	 */
	public static void sequential_thru_test(){
		String signed = withSignature?"signed":"unsigned";
		String operation = isRead?"read":"write";
		
		assert(malEntry != null):"Malicious guid can not be null";
		System.out.println("Start running experiment with "+numClients+" clients, threshold="+thres+"...");
		long t = System.currentTimeMillis();
		Thread thread = new Thread(){
			public void run(){
				
				int lastCount = 0;
				int received = numFinishedReads+numFailedReads;
				while (true) {
					if(received>lastCount)  {
						lastCount = received;
						System.out.print(received + "@" + Util.df(received * 1.0 / (lastReadFinishedTime - t))+"K/s ");
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					received = numFinishedReads+numFailedReads;
				}
			}
		};
		executor.submit(thread);
		for(int i=0; i<numClients; i++){
			executor.submit(new SequentialClient(clients[i], (i>=thres)?malEntry:entry, i>=thres));
		}
		
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println();
		System.out.println("parallel_"+signed+"_"+operation+"_rate="
				+ Util.df(numFinishedReads * 1000.0 / (lastReadFinishedTime - t))
				+ "/s");
	}
	
	static class SequentialClient implements Runnable{
		
		GNSClientCommands client;
		GuidEntry entry;
		boolean mal;
		
		SequentialClient(GNSClientCommands client, GuidEntry entry, boolean mal){
			this.client = client;
			this.entry = entry;
			this.mal = mal;
		}
		
		@Override
		public void run() {
			while(true){
				if(isRead){
					read(client, entry, withSignature, mal);
				}else{
					write(client, entry, withSignature, mal);
				}
			}
		}
		
	}
	
	private static void processArgs(String[] args) throws IOException {
		Config.register(args);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{
		Util.assertAssertionsEnabled();
		processArgs(args);
		
		setup();
		
		if(!withMalicious)
			thru_test();
		else
			sequential_thru_test();
				
		System.exit(0);
	}
}
