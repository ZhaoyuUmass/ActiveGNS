package edu.umass.cs.reconfiguration.reconfigurationpackets;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.nio.Stringifiable;

/**
@author V. Arun
 * @param <NodeIDType> 
 */
public class RequestEpochFinalState<NodeIDType> extends BasicReconfigurationPacket<NodeIDType> {

	/**
	 * @param initiator
	 * @param name
	 * @param epochNumber
	 */
	public RequestEpochFinalState(NodeIDType initiator, String name, int epochNumber) {
		super(initiator, ReconfigurationPacket.PacketType.REQUEST_EPOCH_FINAL_STATE, name, epochNumber);
	}
	/**
	 * @param json
	 * @param unstringer
	 * @throws JSONException
	 */
	public RequestEpochFinalState(JSONObject json, Stringifiable<NodeIDType> unstringer) throws JSONException {
		super(json, unstringer);
	}
}
