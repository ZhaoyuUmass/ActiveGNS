package edu.umass.cs.gnsserver.activecode.prototype.blocking;

import java.util.concurrent.Callable;

import javax.script.ScriptException;

import edu.umass.cs.gnsserver.activecode.prototype.ActiveMessage;
import edu.umass.cs.gnsserver.activecode.prototype.unblocking.ActiveNonBlockingRunner;

/**
 * @author gaozy
 *
 */
public class ActiveWorkerBlockingTask implements Callable<ActiveMessage> {
	
	final ActiveBlockingRunner runner;
	final ActiveMessage request;
	
	ActiveWorkerBlockingTask(ActiveBlockingRunner runner, ActiveMessage request){
		this.runner = runner;
		this.request = request;
	}
		
	@Override
	public ActiveMessage call() {
		ActiveMessage response = null;
		try {
			response = new ActiveMessage(request.getId(), 
					runner.runCode(request.getGuid(), request.getField(), request.getCode(), request.getValue(), request.getTtl(), request.getId()),
					null);
		} catch (NoSuchMethodException | ScriptException e) {
			//e.printStackTrace();
			response = new ActiveMessage(request.getId(), null, e.getMessage());
		}

		return response;
	}

}
