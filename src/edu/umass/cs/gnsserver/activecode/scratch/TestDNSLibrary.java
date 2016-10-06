package edu.umass.cs.gnsserver.activecode.scratch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;

import edu.umass.cs.gnsserver.gnamed.NameResolution;
import edu.umass.cs.utils.Util;

/**
 * This class is used for testing org.xbill.dns library's performance
 * @author gaozy
 *
 */
public class TestDNSLibrary {
	
	
	/**
	 * This test is used for testing the performance of malformed query
	 */ 
	private static void test_01_malformat(){
		byte[] b = new byte[512];
		new Random().nextBytes(b);
		
		long t = System.currentTimeMillis();
		int n = 1000000;
		for (int i=0; i<n; i++){
			try {
				new Message(b);
			} catch (IOException e) {
				NameResolution.formErrorMessage(b).toWire();
			}
		}
		long elapsed = System.currentTimeMillis() - t;
		
		System.out.println("It takes "+elapsed+"ms for "+n+" requests, the average latency for each operation is "+Util.df(elapsed*1000.0/n)+"us");
		System.out.println("The throughput of this operation is "+Util.df(n/elapsed)+" K/s");
	}
	
	private static void test_02_serialization() throws TextParseException, UnknownHostException {
		
		long t = System.currentTimeMillis();
		int n = 1000000;
		for (int i=0; i<n; i++){
			try {
				Message query = generateDNSMessage();
				byte[] b = query.toWire();
				new Message(b);
			} catch (IOException e) {
				
			}
		}
		long elapsed = System.currentTimeMillis() - t;
		
		System.out.println("It takes "+elapsed+"ms for "+n+" requests, the average latency for each operation is "+Util.df(elapsed*1000.0/n)+"us");
		System.out.println("The throughput of this operation is "+Util.df(n/elapsed)+" K/s");
	}
	
	private static Message generateDNSMessage() throws TextParseException, UnknownHostException{
		String domain = "activegns.org.";
		String ip = "1.1.1.1";
		Message response = new Message(0);
		response.getHeader().setFlag(Flags.QR);
	    
	    response.getHeader().setFlag(Flags.RA);

	    response.getHeader().setFlag(Flags.AA);
	    ARecord gnsARecord = new ARecord(new Name(domain), DClass.IN, 60, InetAddress.getByName(ip));
        response.addRecord(gnsARecord, Section.ANSWER);
		
		return response;
		
	}
	
	/**
	 * @param args
	 * @throws UnknownHostException 
	 * @throws TextParseException 
	 */
	public static void main(String[] args) throws TextParseException, UnknownHostException{
		System.out.println("####################### TEST 01 ##########################");
		System.out.println("Malformatted query: Randomly generate meaningless data and use the library to deserialize the message. \n"
				+ "If the message can not be deserialized, then generate a response with a format error message(FORMERR).");
		test_01_malformat();
		System.out.println("######################## DONE ############################");
		
		System.out.println("####################### TEST 02 ##########################");
		System.out.println("DNS request serialization: test for A record \n");
		test_02_serialization();
		System.out.println("######################## DONE ############################");
		
	}
}
