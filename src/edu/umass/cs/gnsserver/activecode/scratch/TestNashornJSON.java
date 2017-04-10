package edu.umass.cs.gnsserver.activecode.scratch;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * @author gaozy
 *
 */
public class TestNashornJSON {
	private static ScriptEngine engine;
	private static Invocable invocable;
	private static ScriptObjectMirror json;
	
	static class Person{
		String fname;
		String lname;
		String ssn;
		
		void setFirstName(String fname){
			this.fname = fname;
		}
		
		void setLastName(String lname){
			this.lname = lname;
		}
		
		void setSsn(String ssn){
			this.ssn = ssn;
		}
	}
	
	/**
	 * @param args
	 * @throws NoSuchMethodException
	 * @throws ScriptException
	 * @throws JSONException
	 */
	public static void main(String[] args) throws NoSuchMethodException, ScriptException, JSONException {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		engine = factory.getScriptEngine("-strict", "--no-java", "--no-syntax-extensions");
		invocable = (Invocable) engine;
	    json = (ScriptObjectMirror) engine.eval("JSON");
	    
	    
	    String code = "var run = function(x,y,z){ return {'A':{'ttl':1, 'record':['1.1.1.1', '2.2.2.2', '3.3.3.3']} } };";
	    engine.eval(code);
	    
	    Object obj = invocable.invokeFunction("run", new Object(), new Object());
	    
	    String result = (String) json.callMember("stringify", obj);
	    
	    JSONObject jobj = new JSONObject(result);
	    assert(jobj.toString().equals(result));
	    
	    JSONObject json = new JSONObject();
	    JSONArray arr = new JSONArray();
	    arr.put("1.1.1.1");
	    arr.put("2.2.2.2");
	    arr.put("3.3.3.3");
	    json.put("record", arr);
	    json.put("ttl", 30);
	    obj = invocable.invokeMethod(engine.eval("JSON"), "parse", json.toString());
	    
	    code = "var run = function(x){ var arr=['a','b','c'];\n x['record']=arr;\n return x};";
	    engine.eval(code);
	    Object r = invocable.invokeFunction("run", obj);
	    String str = (String) invocable.invokeMethod(engine.eval("JSON"), "stringify", r);

	    JSONArray newArr = new JSONArray();
	    newArr.put("a");
	    newArr.put("b");
	    newArr.put("c");
	    json.put("record", newArr);
	    assert(str.equals(json.toString()));
	    
	    System.out.println("All tests succeed!");
	}
}
