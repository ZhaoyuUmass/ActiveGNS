package edu.umass.cs.gnsserver.activecode.scratch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;


import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * This test is used for testing "--no-java" flag in nashorn
 * @author gaozy
 *
 */
public class TestNashornSandbox {
	
	public static void main(String[] args) throws IOException, ScriptException{
		long t=System.currentTimeMillis();
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		ScriptEngine engine = factory.getScriptEngine("-strict", "--no-java", "--no-syntax-extensions");
		long elapsed = System.currentTimeMillis()-t;
		System.out.println("It takes "+elapsed+"ms to initialize a script engine.");
		
		
		String no_java_code = new String(Files.readAllBytes(Paths.get("./scripts/activeCode/testNoJava.js")));
		try {
			engine.eval(no_java_code);
			throw new RuntimeException("Java should not be supported with --no-java option.");
		} catch (ScriptException e) {
			System.out.println("Nashorn with --no-java option test succeeds.");
		}
		
		String no_extension_code = new String(Files.readAllBytes(Paths.get("scripts/activeCode/testNoExtensions.js")));
		try {
			engine.eval(no_extension_code);
			throw new RuntimeException("Java should not be supported with --no-syntax-extensions option.");
		} catch (ScriptException e) {
			System.out.println("Nashorn with --no-syntax-extensions option test succeeds.");
		}
		
		
		String stack_overflow_code = new String(Files.readAllBytes(Paths.get("./scripts/activeCode/testOutOfMemoryError.js")));
		//String stack_overflow_code = new String(Files.readAllBytes(Paths.get("./scripts/activeCode/mal.js")));
		
		try{
			engine.eval(stack_overflow_code);
			throw new RuntimeException("Stack Overflow code should not be executed successfully.");
		}catch(Exception e){
			e.printStackTrace();
		}
		
		throw new RuntimeException("The program should not be here!");
	}
}
