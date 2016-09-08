package edu.umass.cs.gnsclient.client.testing.activecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.FixMethodOrder;

import edu.umass.cs.gigapaxos.testing.TESTPaxosConfig.TC;
import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSClientConfig.GNSCC;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.utils.Config;
import edu.umass.cs.utils.DefaultTest;
import edu.umass.cs.utils.Util;

/**
 * @author gaozy
 *
 */
public class CapacityTestForThruputClient {
	
final static Random random = new Random();
	
	private static int NUM_THREAD = 100;
	
	private static int numClients;
	private static String someField = "someField";
	private static String someValue = "someValue";
	private static boolean withSignature;

	private static boolean isRead;

	private static GuidEntry entry;
	private static GNSClientCommands[] clients;
	
	private static ExecutorService executor;
	
	
	private static int numFinishedReads = 0;
	private static long lastReadFinishedTime = System.currentTimeMillis();

	synchronized static void incrFinishedReads() {
		numFinishedReads++;
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
		System.out.println("There are "+numClients+" clients.");
		
		someField = "someField";
		if(System.getProperty("field")!=null){
			someField = System.getProperty("field");
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
		assert(entry != null);
		
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
						assert(clients[clientIndex].fieldRead(guid, someField).equals(someValue));
					else
						assert(clients[clientIndex].fieldRead(guid.getGuid(),
								someField, null).equals(someValue));
					incrFinishedReads();
				} catch (Exception e) {
					e.printStackTrace();
				}
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
					incrFinishedReads();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * @throws InterruptedException
	 */
	public static void thru_test() throws InterruptedException{
		
		int numReads = Math.min(
				Config.getGlobalBoolean(GNSCC.ENABLE_SECRET_KEY) ? Integer.MAX_VALUE : 10000,
				Config.getGlobalInt(TC.NUM_REQUESTS));
		long t = System.currentTimeMillis();
		for (int i = 0; i < numReads; i++) {
			if(isRead){
				blockingRead(numReads % numClients, entry, withSignature);
			}else{
				blockingWrite(numReads % numClients, entry, withSignature);
			}
		}
		System.out.print("[total_reads=" + numReads+": ");
		int lastCount = 0;
		while (numFinishedReads < numReads) {
			if(numFinishedReads>lastCount)  {
				lastCount = numFinishedReads;
				System.out.print(numFinishedReads + "@" + Util.df(numFinishedReads * 1.0 / (lastReadFinishedTime - t))+"K/s ");
			}
			Thread.sleep(1000);
		}
		System.out.print("] ");

		System.out.print("parallel_signed_read_rate="
				+ Util.df(numReads * 1.0 / (lastReadFinishedTime - t))
				+ "K/s");
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
		thru_test();
				
		System.exit(0);
	}
}
