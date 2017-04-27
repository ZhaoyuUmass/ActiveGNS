package edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import edu.umass.cs.gnscommon.GNSProtocol;
import edu.umass.cs.gnscommon.ResponseCode;
import edu.umass.cs.gnscommon.exceptions.server.FailedDBOperationException;
import edu.umass.cs.gnscommon.exceptions.server.RecordNotFoundException;
import edu.umass.cs.gnscommon.packets.CommandPacket;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.ClientRequestHandlerInterface;
import edu.umass.cs.gnsserver.gnsapp.clientSupport.AclCheckResult;
import edu.umass.cs.gnsserver.gnsapp.clientSupport.ClientSupportConfig;
import edu.umass.cs.gnsserver.gnsapp.clientSupport.NSAccessSupport;
import edu.umass.cs.gnsserver.interfaces.InternalRequestHeader;
import edu.umass.cs.gnsserver.main.GNSConfig;
import edu.umass.cs.utils.Config;
import edu.umass.cs.utils.DelayProfiler;

/**
 * A proof-of-concept implementation to show that the
 * performance can be burst by only touching DB once
 * in the entire transaction.
 * 
 * @author gaozy
 *
 */
public class FieldAccessV2 {
	
	/**
	 * Defines the field name in the guid where guid info is stored.
	 */
	public static final String GUID_INFO = InternalField
	          .makeInternalFieldString("guid_info");
	
	private static ResponseCode signatureAndACLCheck(InternalRequestHeader header, String guid,
	          String field, List<String> fields,
	          String accessorGuid, String signature,
	          String message, MetaDataTypeName metaDataTypeName,
	          JSONObject valuesMap){
		assert(header!=null);
		
		ResponseCode errorCode = ResponseCode.NO_ERROR;
		// If no signature or no accessorGuid, we only check whether the field is accessible by everyone
		if(signature == null || accessorGuid == null){
			if (fieldAccessibleByEveryone(metaDataTypeName, guid, field, valuesMap)) {
		        return ResponseCode.NO_ERROR;
		      } else {
		        ClientSupportConfig.getLogger().log(Level.FINE, "Name {0} key={1} : ACCESS_ERROR",
		                new Object[]{guid, field});
		        return ResponseCode.ACCESS_ERROR;
		      }
		}

		// Next let's check ACL for signed read
		AclCheckResult aclResult = aclCheck(header, guid, field,
		           accessorGuid, metaDataTypeName, valuesMap);
		if(aclResult.getResponseCode().isExceptionOrError()){
			return aclResult.getResponseCode();
		}
		
		// Then check the signature for signed read
		try {
			if(!NSAccessSupport.verifySignature(aclResult.getPublicKey(), signature, message)){
				ClientSupportConfig.getLogger().log(Level.FINE,
			              "Name {0} key={1} : SIGNATURE_ERROR", new Object[]{guid, field});
			    return ResponseCode.SIGNATURE_ERROR;
			}
		} catch (InvalidKeyException | SignatureException | UnsupportedEncodingException | InvalidKeySpecException e) {
			return ResponseCode.SIGNATURE_ERROR;
		}
		return errorCode;
	}
	
	/**
	 * return the first index of item in arr, -1 means not found the item in arr
	 * 
	 * @param arr
	 * @param item
	 * @return index of item in arr
	 */
	private static int indexOf(JSONArray arr, String item){
		int index = -1;
		if(arr == null)
			return index;
		for (int i=0; i<arr.length(); i++){
			try {
				if(arr.getString(i).equals(item)){
					return i;
				}
			} catch (JSONException e) {
				// It doesn't matter if it is not a string
			}
		}
		return index;
	}
	
	
	private static boolean fieldAccessibleByEveryone(MetaDataTypeName metaDataTypeName, 
			String guid, String field, JSONObject valuesMap){
		String prefix = metaDataTypeName.getPrefix();
		String name = metaDataTypeName.name();
		// field is field, we also need entire field
		String entireRecord = GNSProtocol.ENTIRE_RECORD.toString();
		// we need another field called MD
		String md = "MD";
		try {
			JSONArray aclOfField = valuesMap.getJSONObject(prefix)
					.getJSONObject(name).getJSONObject(field).getJSONArray(md);
			if(indexOf(aclOfField, GNSProtocol.EVERYONE.toString()) >= 0){				
				return true;
			}
		} catch (JSONException e) {
			try {
				JSONArray aclOfEntireRecord = valuesMap.getJSONObject(prefix)
						.getJSONObject(name).getJSONObject(entireRecord).getJSONArray(md);
				if(indexOf(aclOfEntireRecord, GNSProtocol.EVERYONE.toString()) >= 0){				
					return true;
				}
			} catch (JSONException e1) {
				// We can not find GNSProtocol.EVERYONE, and return false
				return false;
			}
		}

		return false;
	}
	
	private static AclCheckResult aclCheck(InternalRequestHeader header, String targetGuid, String field,
	          String accessorGuid, MetaDataTypeName access, JSONObject valuesMap){
		ClientSupportConfig.getLogger().log(Level.FINE,
	            "@@@@@@@@@@@@@@@@ACL Check guid={0} key={1} accessor={2} access={3}",
	            new Object[]{targetGuid, field, accessorGuid, access});
		String publicKey = null;
		
		if(accessorGuid.equals(targetGuid)){
			/**
			 * The simple case is that accessor is the same as target, then we fetch the public key to prepare for signature check
			 * This handles the base case where we're accessing our own guid. 
		     * Access to all of our fields is always allowed to our own guid so we just need to get
		     * the public key out of the guid - possibly from the cache.
		     */
		      try {
				publicKey = valuesMap.getJSONObject(GUID_INFO).getString(GNSProtocol.PUBLIC_KEY.toString());
			} catch (JSONException e) {
				e.printStackTrace();
				// The target GUID is mal-formatted, it's a bad guid
				return new AclCheckResult(null, ResponseCode.BAD_GUID_ERROR);
			}		      
		      
		} else {
			// When accessor is different from target, let's do the regular ACL check
			return null;
		}
//		System.out.println("public key:"+publicKey+", valuesMap:"+valuesMap);
		// Return an error immediately here because if we can't find the public key 
	      // the guid must not be local which is a problem.
	      if (publicKey == null) {
	    	  return new AclCheckResult(null, ResponseCode.BAD_GUID_ERROR);
	      } else {
	    	  return new AclCheckResult(publicKey, ResponseCode.NO_ERROR);
	      }
	}
	
	 /**	   
	   * In this implementation, we will fetch the record at the beginning
	   * of the transaction, so that we do not need to access to DB again.
	   * The steps:
	   * 1. Fetch the entire record of the target GUID
	   * 2. Check ACL and signature (if applicable)
	   * 3. Return the value
	   * 
	   * @author gaozy
	   * 
	   * @param header
	   * @param commandPacket
	   * @param guid
	   * @param field - mutually exclusive with fields
	   * @param reader
	   * @param signature
	   * @param message
	   * @param timestamp
	   * @param handler
	   * @return the value of a single field
	   */
	  public static CommandResponse lookupSingleField(InternalRequestHeader header, CommandPacket commandPacket,
	          String guid, String field,
	          String reader, String signature, String message, Date timestamp,
	          ClientRequestHandlerInterface handler) {
	  ClientSupportConfig.getLogger().log(Level.FINE, "guid={0} field={1} accessor={2} ",
                new Object[]{guid, field});  
		JSONObject json = null;
		/**
		 * 1. Fetch the entire record of guid
		 */
		try {
			long startTime = System.nanoTime();
			json = handler.getApp().getDB().lookupEntireRecord(guid);
			DelayProfiler.updateDelayNano("lookupEntireRecord", startTime);
		} catch (FailedDBOperationException | RecordNotFoundException e) {
			return new CommandResponse(ResponseCode.DATABASE_OPERATION_ERROR, GNSProtocol.BAD_RESPONSE.toString()
		              + " " + GNSProtocol.DATABASE_OPERATION_ERROR.toString() + " " + e);
		}
		
		JSONObject valuesMap = null; 
		if(json!=null){
			try {
				valuesMap = json.getJSONObject("nr_valuesMap");
			} catch (JSONException e) {
				return new CommandResponse(ResponseCode.JSON_PARSE_ERROR, GNSProtocol.BAD_RESPONSE.toString()
			              + " " + GNSProtocol.JSON_PARSE_ERROR.toString() + " " + e);
			}
		}
		
		/**
		 * 2. Check ACL and signature
		 * 
		 * Need to check when to check ACL and signature:
		 * 1. A header is verified as internal means the package shares a same INTERNAL_PROOF with this GNS
		 * 2. A command package is mutual auth means the package belongs to an additional category MUTUAL_AUTH.
		 * 	  For a data command, only ReadSecured is a mutual auth command.
		 * 3. A package's reader is the same as INTERNAL_QUERIER means this package is a remote query(active code)
		 */
		ResponseCode errorCode = ResponseCode.NO_ERROR;
		// Only check ACL and signature for non-internal non-mutual request
		if(!header.verifyInternal() && !commandPacket.getCommandType().isMutualAuth()){
			if(field!=null){
				// Check ACL and signature here
				errorCode = signatureAndACLCheck(header, guid, field, null, reader,
		                signature, message, MetaDataTypeName.READ_WHITELIST, valuesMap);
			} else {
				// return a no field error
				return new CommandResponse(ResponseCode.FIELD_NOT_FOUND_EXCEPTION,
		                  GNSProtocol.BAD_RESPONSE.toString() + " "
		                  + GNSProtocol.FIELD_NOT_FOUND.toString() + " " + guid + ":" + field + " ");
			}
		} else {
			/**
			 * There is no need to check ACL and signature for the case that both header 
			 * is internal and command is mutual auth, i.e.,
			 * header.verifyInternal() && commandPacket.getCommandType().isMutualAuth()
			 * 
			 * FIXME: it's unclear whether a package can be non-internal and mutual auth
			 * or internal and non-mutual auth, it seems like a ReadSecured command might
			 * be mutual auth but non-internal, this needs Arun to justify.
			 * 
			 * We assume that the package can be either internal or mutual auth at this point,
			 * and we need to check whether the querier is the same as INTERNAL_QUERIER,
			 * i.e., this is a remote query of active code.
			 */
			if(GNSProtocol.INTERNAL_QUERIER.toString().equals(reader)){
				/*
				errorCode = aclCheck(header, guid, field,
		                  header.getQueryingGUID(),
		                  MetaDataTypeName.READ_WHITELIST, app);
		                  */
			}
			
		}
		
		// time stamp can't be null for the request with signature, to prevent replay attack
		if(timestamp != null ) {
			assert(signature!=null);
			if (timestamp.before(DateUtils.addMinutes(new Date(),
	                -Config.getGlobalInt(GNSConfig.GNSC.STALE_COMMAND_INTERVAL_IN_MINUTES)))) {
	          return new CommandResponse(ResponseCode.STALE_COMMAND_VALUE, 
	        		  GNSProtocol.STALE_COMMMAND.toString()+" timestamp:"+timestamp);
	        }
		}
		
		if(errorCode.isExceptionOrError()){
			return new CommandResponse(errorCode, GNSProtocol.BAD_RESPONSE.toString() + " " + errorCode.getProtocolCode());
		}
		
		if (!header.verifyInternal() && InternalField.isInternalField(field)) {
	        // We are not allowed to return an internal field if the request is not internal
			return new CommandResponse(ResponseCode.ACCESS_ERROR, GNSProtocol.BAD_RESPONSE.toString()
					+" internal field is not acessible!");
	    }
		
		
		/**
		 * 3. Prepare the response
		 */
		JSONObject value = new JSONObject();
		try {
			if(!valuesMap.has(field)){
				return new CommandResponse(ResponseCode.FIELD_NOT_FOUND_EXCEPTION,
		                  GNSProtocol.BAD_RESPONSE.toString() + " "
		                  + GNSProtocol.FIELD_NOT_FOUND.toString() + " " + guid + ":" + field + " ");
			}else{
				value.put(field, valuesMap.get(field));
				return new CommandResponse(ResponseCode.NO_ERROR,
						value.toString());
			}
						
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new CommandResponse(ResponseCode.JSON_PARSE_ERROR, GNSProtocol.BAD_RESPONSE.toString()
		              + " " + GNSProtocol.JSON_PARSE_ERROR.toString() + " " + e);
		}
	  }	
}
