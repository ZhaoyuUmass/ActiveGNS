package edu.umass.cs.gnsserver.gnamed;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import edu.umass.cs.gnsclient.client.GNSClient;
import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.utils.DelayProfiler;

/**
 * @author gaozy
 *
 */
public class GNSLookupTask implements Callable<Message> {
	
	private GNSClientCommands client;
	private GuidEntry entry;
	private Message query;
	
	protected GNSLookupTask(GNSClientCommands client, GuidEntry entry, Message query){
		this.client = client;
		this.entry = entry;
		this.query = query;
	}
	
	@Override
	public Message call() throws Exception {
		
		int type = query.getQuestion().getType();
	    // Was the query legitimate or implemented?
	    if (!Type.isRR(type) && type != Type.ANY) {
	      return NameResolution.errorMessage(query, Rcode.NOTIMP);
	    }

	    // extract the domain (guid) and field from the query
	    final String fieldName = Type.string(query.getQuestion().getType());
	    final Name requestedName = query.getQuestion().getName();
	    final byte[] rawName = requestedName.toWire();
	    final String domainName = NameResolution.querytoStringForGNS(rawName);

	    NameResolution.getLogger().log(Level.FINE, "Trying GNS lookup for domain {0}", domainName);

	    /* Create a response message and add records later */
	    Message response = new Message(query.getHeader().getID());
	    response.getHeader().setFlag(Flags.QR);
	    if (query.getHeader().getFlag(Flags.RD)) {
	      response.getHeader().setFlag(Flags.RA);
	    }
	    response.addRecord(query.getQuestion(), Section.QUESTION);
	    response.getHeader().setFlag(Flags.AA);

	    /* Request DNS fields of an alias and prepare a DNS response message */
	    Boolean nameResolved = false;
	    String nameToResolve = domainName;
	    
	    while (!nameResolved) {
	      JSONObject fieldResponseJson = lookupGuidField(nameToResolve, "A");
	      if (fieldResponseJson == null) {
	        return NameResolution.errorMessage(query, Rcode.NXDOMAIN);
	      }
          if (fieldResponseJson.has("A")) {
        	JSONArray records = fieldResponseJson.getJSONArray("A");
        	for(int i=0; i<records.length(); i++){
        		String ip = records.getString(i);
	            ARecord gnsARecord = new ARecord(new Name(nameToResolve), DClass.IN, 60, InetAddress.getByName(ip));
	            response.addRecord(gnsARecord, Section.ANSWER);
        	}
            nameResolved = true;
          }
	    }
	    NameResolution.getLogger().log(Level.FINER, "Outgoing response from GNS: {0}", response.toString());
		
		return response;
	}
	
	
	private JSONObject lookupGuidField(String domain, String field){		
		JSONObject record = null;
		try {
			if(domain.equals("activegns.org.")){
				String value = client.fieldRead(entry, field);
				record = new JSONObject();
				record.put(field, new JSONArray(value));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return record;
	}
}
