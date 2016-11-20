package edu.umass.cs.gnsserver.activecode.prototype.unblocking;

import java.io.IOException;
import java.util.List;

import org.json.JSONObject;

import edu.umass.cs.gnsserver.activecode.prototype.ActiveException;
import edu.umass.cs.gnsserver.activecode.prototype.ActiveMessage;
import edu.umass.cs.gnsserver.activecode.prototype.ActiveMessage.Type;
import edu.umass.cs.gnsserver.activecode.prototype.interfaces.ACLQuerier;
import edu.umass.cs.gnsserver.activecode.prototype.interfaces.Channel;
import edu.umass.cs.gnsserver.activecode.prototype.interfaces.DNSQuerier;
import edu.umass.cs.gnsserver.activecode.prototype.interfaces.Querier;
import edu.umass.cs.gnsserver.activecode.prototype.utils.Location;

/**
 * This class is an implementation of Querier, Querier only contains
 * readGuid and writeGuid method, so the protected methods will not be
 * exposed to the javascript code.
 * @author gaozy
 *
 */
public class ActiveNonBlockingQuerier implements Querier,ACLQuerier,DNSQuerier {
	private Channel channel;
	private int currentTTL;
	private String currentGuid;
	private long currentID;
	
	private Monitor monitor;
	
	/**
	 * @param channel
	 * @param ttl 
	 * @param guid 
	 * @param id 
	 */
	public ActiveNonBlockingQuerier(Channel channel, int ttl, String guid, long id){
		this.channel = channel;
		this.currentTTL = ttl;
		this.currentGuid = guid;
		this.currentID = id;
		
		monitor = new Monitor();
	}
	
	
	/**
	 * @param queriedGuid
	 * @param field
	 * @return ValuesMap the code trying to read
	 * @throws ActiveException
	 */
	@Override
	public JSONObject readGuid(String queriedGuid, String field) throws ActiveException{
		if(currentTTL <=0)
			throw new ActiveException(); //"Out of query limit"
		if(queriedGuid==null)
			return readValueFromField(currentGuid, currentGuid, field, currentTTL);
		return readValueFromField(currentGuid, queriedGuid, field, currentTTL);
	}
	
	/**
	 * @param queriedGuid
	 * @param field
	 * @param value
	 * @throws ActiveException
	 */
	@Override
	public void writeGuid(String queriedGuid, String field, JSONObject value) throws ActiveException{
		if(currentTTL <=0)
			throw new ActiveException(); //"Out of query limit"
		if(queriedGuid==null)
			writeValueIntoField(currentGuid, currentGuid, field, value, currentTTL);
		else
			writeValueIntoField(currentGuid, queriedGuid, field, value, currentTTL);
	}
	
	@Override
	public JSONObject lookupUsernameForGuid(String targetGuid) throws ActiveException {
		throw new RuntimeException("unimplemented");
	}
	
	private JSONObject readValueFromField(String querierGuid, String queriedGuid, String field, int ttl)
			throws ActiveException {
		JSONObject value = null;
		try{
			ActiveMessage am = new ActiveMessage(ttl, querierGuid, field, queriedGuid, currentID);
			channel.sendMessage(am);
			synchronized(monitor){
				while(!monitor.getDone()){				
					try {						
						monitor.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						return null;
					}
				}
			}
					
			ActiveMessage response = monitor.getResult();
			
			if(response == null){
				throw new ActiveException();
			}
			
			if (response.getError() != null){
				throw new ActiveException();
			}
			value = response.getValue();
		} catch(IOException e) {
			throw new ActiveException();
		}
		return value;
	}

	private void writeValueIntoField(String querierGuid, String queriedGuid, String field, JSONObject value, int ttl)
			throws ActiveException {
		
			ActiveMessage am = new ActiveMessage(ttl, querierGuid, field, queriedGuid, value, currentID);			
			try {
				channel.sendMessage(am);
				synchronized(monitor){
					while(!monitor.getDone()){					
						try {							
							monitor.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							return;
						}
					}
				}
				
				ActiveMessage response = monitor.getResult();
				
				if(response == null){
					throw new ActiveException();
				}
				if (response.getError() != null){
					throw new ActiveException();
				}
			} catch (IOException e) {
				throw new ActiveException();
			}
	}
	
  /**
   *
   * @param response
   * @param isDone
   */
  protected void release(ActiveMessage response, boolean isDone){
		monitor.setResult(response, isDone);
	}
	
	private static class Monitor {
		boolean isDone;
		ActiveMessage response;
		
		Monitor(){
			this.isDone = false;
		}
		
		boolean getDone(){
			return isDone;
		}
		
		synchronized void setResult(ActiveMessage response, boolean isDone){
			assert(response.type == Type.RESPONSE):"This is not a response!";
			this.isDone = isDone;
			this.response = response;				
			notifyAll();
		}
		
		ActiveMessage getResult(){
			return response;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		int n = 1000000;	
		//ActiveNonBlockingQuerier querier = null;
		
		long t = System.currentTimeMillis();
		for(int i=0; i<n; i++){
			
		}
		long elapsed = System.currentTimeMillis() - t;
		System.out.println("It takes "+elapsed+"ms, and the average latency for each operation is "+(elapsed*1000.0/n)+"us");
		
		
	}


	@Override
	public List<Location> getLocations(List<String> ips) {
		// TODO Auto-generated method stub
		return null;
	}
}
