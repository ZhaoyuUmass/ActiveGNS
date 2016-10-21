package edu.umass.cs.gnsserver.activecode.prototype.unblocking;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import edu.umass.cs.gnsserver.activecode.prototype.ActiveMessage;
import edu.umass.cs.gnsserver.activecode.prototype.ActiveMessage.Type;
import edu.umass.cs.gnsserver.activecode.prototype.channels.ActiveNamedPipe;
import edu.umass.cs.gnsserver.activecode.prototype.interfaces.Channel;

/**
 * @author gaozy
 *
 */
public class ActiveNonBlockingWorker {
	
	
	private final ActiveNonBlockingRunner runner;
	
	private final Channel channel;
	private final int id;
	
	private final ThreadPoolExecutor executor;
	private final ThreadPoolExecutor taskExecutor;	
	
	
	
	/**
	 * Initialize a worker with a named pipe
	 * @param ifile
	 * @param ofile
	 * @param id 
	 * @param numThread 
	 * @param isTest
	 */
	protected ActiveNonBlockingWorker(String ifile, String ofile, int id, int numThread) {
		this.id = id;
		
		executor = new ThreadPoolExecutor(numThread, numThread, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		executor.prestartAllCoreThreads();
		taskExecutor = new ThreadPoolExecutor(numThread, numThread, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		taskExecutor.prestartAllCoreThreads();
		
		channel = new ActiveNamedPipe(ifile, ofile);
		runner = new ActiveNonBlockingRunner(channel);
		
		try {
			runWorker();
		} catch (JSONException | IOException e) {
			//e.printStackTrace();
			// close the channel and exit
		}finally{
			channel.close();
		}
		
	}

	
	private void runWorker() throws JSONException, IOException {
		
		ActiveMessage msg = null;
		while(!Thread.currentThread().isInterrupted()){
			if((msg = (ActiveMessage) channel.receiveMessage()) != null){
				System.out.println("Worker receives message:"+msg);
				if(msg.type == Type.REQUEST){
					taskExecutor.submit(new ActiveWorkerSubmittedTask(executor, runner, msg, channel));					
				} else if (msg.type == Type.RESPONSE ){
					runner.release(msg);					
				} 
			}else{
				// The client is shutdown
				break;
			}
		}
	}
	
	
	public String toString(){
		return this.getClass().getSimpleName()+id;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		boolean pipeEnable = Boolean.parseBoolean(args[4]);
		if(pipeEnable){
			String cfile = args[0];
			String sfile = args[1];
			int id = Integer.parseInt(args[2]);
			int numThread = Integer.parseInt(args[3]);
			
			new ActiveNonBlockingWorker(cfile, sfile, id, numThread);
		}
	}
}
