package edu.umass.cs.gnsserver.activecode.scratch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 */
public class TestNashornPerformance {
	
	 /**
	 * @param args
	 * @throws IOException
	 * @throws ScriptException
	 * @throws NoSuchMethodException
	 * @throws JSONException 
	 */
	public static void main(String[] args) throws IOException, ScriptException, NoSuchMethodException, JSONException {
				
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        Invocable invocable = (Invocable) engine;
        JSONObject value = new JSONObject();
        String field = "someField";
        value.put(field, "someField");
        Object querier = null;
        
        String fileName = "scripts/activeCode/noop.js";
        if(args.length == 1){
        	fileName = args[0];
        }
        
        String code = new String(Files.readAllBytes(Paths.get(fileName)));
        ScriptContext sc = new SimpleScriptContext();
        engine.eval(code, sc);
        engine.setContext(sc);
        invocable.invokeFunction("run", value, field, querier);
        
        int n = 1000000;
        long t = System.currentTimeMillis();
        for(int i=0; i<n; i++) {
        	engine.setContext(sc);
        	invocable.invokeFunction("run", value, field, querier);
    	}
        long elapsed = System.currentTimeMillis() - t;
		System.out.println(String.format("It takes %d ms to execute all requests, avg_latency = %.2f ms", elapsed, elapsed*1000.0/n));
	 }
}

