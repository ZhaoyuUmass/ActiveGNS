package edu.umass.cs.gnsserver.activecode.prototype.blocking;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import edu.umass.cs.gnsserver.activecode.prototype.ActiveMessage;
import edu.umass.cs.gnsserver.activecode.prototype.ActiveMessage.Type;
import edu.umass.cs.gnsserver.activecode.prototype.ActiveQuerier;
import edu.umass.cs.gnsserver.activecode.prototype.ActiveRunner;
import edu.umass.cs.gnsserver.activecode.prototype.channels.ActiveNamedPipe;
import edu.umass.cs.gnsserver.activecode.prototype.interfaces.Channel;

/**
 * @author gaozy
 *
 */
public class ActiveBlockingWorker {
	
	
	private final ActiveRunner runner;
	
	private final Channel channel;
	private final int id;
	
	private final ThreadPoolExecutor executor;
	private final AtomicInteger counter = new AtomicInteger(0);		
	
	
	/**
	 * Initialize a worker with a named pipe
	 * @param ifile
	 * @param ofile
	 * @param id 
	 * @param numThread 
	 * @param isTest
	 */
	protected ActiveBlockingWorker(String ifile, String ofile, int id, int numThread) {
		this.id = id;
		
		channel = new ActiveNamedPipe(ifile, ofile);
		runner = new ActiveRunner(new ActiveQuerier(channel));
		
		executor = new ThreadPoolExecutor(numThread, numThread, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		executor.prestartAllCoreThreads();	
		
		try {
			runWorker();
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			// close the channel and exit
		}finally{
			channel.close();
		}
		
	}

	
	private void runWorker() throws JSONException, IOException {
		
		
		while(!Thread.currentThread().isInterrupted()){
			ActiveMessage msg = null;
			if((msg = (ActiveMessage) channel.receiveMessage()) != null){

				if(msg.type == Type.REQUEST){
					ActiveMessage response = null;
					Future<ActiveMessage> future = executor.submit(new ActiveWorkerBlockingTask(runner, msg));
					try {
						response = future.get(msg.getBudget(), TimeUnit.MILLISECONDS);
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						e.printStackTrace();
						// construct a response with an error and cancel this task
						future.cancel(true);
						response = new ActiveMessage(msg.getId(), null, e.getMessage());
					}
					// send back response
					channel.sendMessage(response);
					counter.getAndIncrement();
				} else if(msg.type == Type.RESPONSE){
					runner.release(msg);
				}				
			}else{
				// The client is shutdown, let's exit this loop and return
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
			
			new ActiveBlockingWorker(cfile, sfile, id, numThread);
		}
	}
}