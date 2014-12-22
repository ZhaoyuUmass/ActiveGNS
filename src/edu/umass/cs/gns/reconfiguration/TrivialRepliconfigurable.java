package edu.umass.cs.gns.reconfiguration;

import java.util.Set;

import edu.umass.cs.gns.nio.IntegerPacketType;

/**
@author V. Arun
 */
public class TrivialRepliconfigurable implements InterfaceRepliconfigurable {
	
	private final InterfaceApplication app;
	
	public TrivialRepliconfigurable(InterfaceApplication app) {
		this.app = app;
	}

	@Override
	public boolean handleRequest(InterfaceRequest request) {
		return this.app.handleRequest(request);
	}

	@Override
	public InterfaceRequest getRequest(String stringified)
			throws RequestParseException {
		return this.app.getRequest(stringified);
	}

	@Override
	public Set<IntegerPacketType> getRequestTypes() {
		return this.app.getRequestTypes();
	}


	@Override
	public boolean handleRequest(InterfaceRequest request,
			boolean doNotReplyToClient) {
		return (this.app instanceof InterfaceReplicable ? 
				((InterfaceReplicable)this.app).handleRequest(request, doNotReplyToClient): 
					this.app.handleRequest(request));
	}

	@Override
	public InterfaceReconfigurableRequest getStopRequest(String name, int epoch) {
		if(this.app instanceof InterfaceReconfigurable) return ((InterfaceReconfigurable)this.app).getStopRequest(name, epoch);
		throw new RuntimeException("Can not get stop request for a non-reconfigurable app");
	}

	@Override
	public String getFinalState(String name, int epoch) {
		if(this.app instanceof InterfaceReconfigurable) return ((InterfaceReconfigurable)this.app).getFinalState(name, epoch);
		throw new RuntimeException("Can not get stop request for a non-reconfigurable app");
	}

	@Override
	public void putInitialState(String name, int epoch, String state) {
		if(this.app instanceof InterfaceReconfigurable) {
			((InterfaceReconfigurable)this.app).putInitialState(name, epoch, state);
			return;
		}
		throw new RuntimeException("Can not get stop request for a non-reconfigurable app");
	}

	@Override
	public boolean deleteFinalState(String name, int epoch) {
		if(this.app instanceof InterfaceReconfigurable) return ((InterfaceReconfigurable)this.app).deleteFinalState(name, epoch);
		throw new RuntimeException("Can not get stop request for a non-reconfigurable app");
	}

	@Override
	public Integer getEpoch(String name) {
		if(this.app instanceof InterfaceReconfigurable) return ((InterfaceReconfigurable)this.app).getEpoch(name);
		throw new RuntimeException("Can not get stop request for a non-reconfigurable app");
	}

	@Override
	public String getState(String name, int epoch) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateState(String name, String state) {
		// TODO Auto-generated method stub
		return false;
	}
}
