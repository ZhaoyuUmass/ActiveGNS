package edu.umass.cs.gnsserver.activecode.scratch;

import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * This is a tool for cleaning up MongoDB
 * 
 * @author gaozy
 *
 */
public class CleanupMongoDB {
	
	/**
	 * @param args
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) throws UnknownHostException {
		
		MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
		
		List<String> dbs = mongoClient.getDatabaseNames();
		
		for (String name:dbs){
			System.out.println(name);
			if (name.startsWith("UMASS_GNS_DB_GNSApp")){
				DB db = mongoClient.getDB(name);
				db.dropDatabase();				
			}
		}
		
		mongoClient.close();
		
		System.out.println("Nothing wrong!");
	}
}
