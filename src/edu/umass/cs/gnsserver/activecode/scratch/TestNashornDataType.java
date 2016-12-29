package edu.umass.cs.gnsserver.activecode.scratch;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runners.MethodSorters;

import edu.umass.cs.gnsclient.client.util.Util;
import edu.umass.cs.utils.DefaultTest;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;

/**
 * @author gaozy
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNashornDataType extends DefaultTest {
	
	private final static String someField = "someField";
	private final static String someValue = "someValue";
	
	private static ScriptEngine engine;
	private static Invocable invocable;
	
	@SuppressWarnings("restriction")
	private static ScriptObjectMirror json;
	private static String code;
	
	@SuppressWarnings("restriction")
	private static ScriptObjectMirror string2JS(String str){
		return (ScriptObjectMirror) json.callMember("parse", str);
	}
	
	@SuppressWarnings("restriction")
	private static String js2String(ScriptObjectMirror obj){
		return (String) json.callMember("stringify", obj);
	}
	
	/**
	 * @throws Exception
	 */
	@SuppressWarnings("restriction")
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		engine = factory.getScriptEngine("-strict", "--no-java", "--no-syntax-extensions");
		invocable = (Invocable) engine;
		json = (ScriptObjectMirror) engine.eval("JSON");
		code = new String(Files.readAllBytes(Paths.get("./scripts/activeCode/noop.js")));
		engine.eval(code);
	}
	
	/**
	 * @throws JSONException 
	 * 
	 */
	@Test
	public void test_00_JSArray() throws JSONException{
		JSONArray arr = new JSONArray();
		arr.put(4);
		arr.put(1);		
		JSONObject obj = new JSONObject();
		JSONObject aobj = new JSONObject();
		aobj.put("", arr);
		aobj.put("ttl", 30);
		obj.put("A", aobj);
		
		String new_code = null;
		try {
			new_code = new String(Files.readAllBytes(Paths.get("./scripts/activeCode/test.js")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
		try {
			engine.eval(new_code);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		
		try {
			/*
			JSONObject result = new JSONObject(
					js2String( (ScriptObjectMirror) invocable.invokeFunction("run", string2JS(arr.toString()), someField, null) )
					);
			*/
			ScriptObjectMirror result = (ScriptObjectMirror) invocable.invokeFunction("run", string2JS(obj.toString()), someField, null);
			//String[] iarr = (String[])ScriptUtils.convert(result, String[].class);
			String str = js2String(result);
			System.out.println("converted:"+str);
		} catch (NoSuchMethodException | ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	//@Test
	public void test_01_Java2JS(){
		JSONObject obj = new JSONObject();
		try {
			obj.put(someField, someValue);
			engine.eval(code);
		} catch (JSONException | ScriptException e) {
			fail(e.getMessage());
		}
		String str = obj.toString();
		
		int n =1000000;
		long t1 = System.nanoTime();
		for(int i=0;i<n; i++){
			try {
				JSONObject result = new JSONObject( 
						js2String( (ScriptObjectMirror) invocable.invokeFunction("run", string2JS(new JSONObject(str).toString()), someField, null) ) 
						);
			} catch (NoSuchMethodException | JSONException | ScriptException e) {
				e.printStackTrace();
			}
		}
		long elapsed = System.nanoTime() - t1;
		System.out.println("Serialization: String->JSONObject->JS JSON->JSONObject\nEach call takes: "+Util.df(elapsed/n)+"ns");
	}
	
	/**
	 * This method test the serialization order: String->JSONObject
	 * <p> This order has already been implemented, but it is hard for people to understand and use as Java objects, 
	 * i.e., JSONObject and JSONArray, are nested with Javascript collections, i.e., JSON and Array
	 */
	//@Test
	public void test_02_String2Java(){
		JSONObject obj = new JSONObject();
		try {
			obj.put(someField, someValue);
			engine.eval(code);
		} catch (JSONException | ScriptException e) {
			fail(e.getMessage());
		}
		String str = obj.toString();
		
		int n =1000000;
		long t1 = System.nanoTime();
		for(int i=0;i<n; i++){
			try {
				JSONObject result = (JSONObject) invocable.invokeFunction("run", new JSONObject(str), someField, null);
			} catch (NoSuchMethodException | ScriptException | JSONException e) {
				e.printStackTrace();
			}
		}
		long elapsed = System.nanoTime() - t1;
		System.out.println("Serialization: String->JSONObject\nEach call takes: "+Util.df(elapsed/n)+"ns");
	}
	
	/**
	 * This method test the serialization order: String->JSON
	 * <p> This order is supposed to be easily understood by JS programmer who writes JS active code.
	 * All parameters are used as native JS collections instead of Java objects.
	 */
	//@Test
	public void test_03_String2JS(){
		
		JSONObject obj = new JSONObject();
		try {
			obj.put(someField, someValue);
			engine.eval(code);
		} catch (JSONException | ScriptException e) {
			fail(e.getMessage());
		}
		String str = obj.toString();
		
		int n =1000000;
		long t1 = System.nanoTime();
		for(int i=0;i<n; i++){
			try {
				String result = js2String( (ScriptObjectMirror) invocable.invokeFunction("run", string2JS(str), someField, null) );
			} catch (NoSuchMethodException | ScriptException e) {
				e.printStackTrace();
			}
		}
		long elapsed = System.nanoTime() - t1;
		System.out.println("Serialization: String->JS JSON\nEach call takes: "+Util.df(elapsed/n)+"ns");
	}
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws ScriptException 
	 * @throws NoSuchMethodException 
	 * @throws JSONException 
	 */
	public static void main(String[] args) throws IOException, ScriptException, NoSuchMethodException, JSONException{
		
		Result result = JUnitCore.runClasses(TestNashornDataType.class);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString() + "\n");
	    }
	}
}
