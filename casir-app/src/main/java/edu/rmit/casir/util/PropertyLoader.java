package edu.rmit.casir.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;


public class PropertyLoader {

	public static final String HMSCSpecFile ="hmsc.case";
	public static final String CASENAME="casename";
	public static final String COSTSpec="sample.cost";
	public static final String LTSDir="lts.dir";
	public static final String LTSFsp="fsp";	// the filename of input FSP file 
	public static final String LTSAssert="assert";  // the path of fluent assert file
	public static final String RECOVMscSpecDir="recovery.dir"; 
	public static final String COSTDir="cost.dir";
	public static final String MARKOVDir="markov.dir"; 
	public static final String LTSOutputDir="lts.output.dir"; // the directory of the output FSP file
	public static final String WEIGHTED_TRACE_Dir="trace.dir"; // generated dependence graphy dot dir
	public static final String RECOVERY_TRACE_Dir="recovery.trace.dir"; //generated trace language dot
	
	
	private static Logger logger = Logger.getLogger(PropertyLoader.class);

	
	public static HashMap<String, String> cachedProperty;
	
	
	public static HashMap<String, String> getCachedProperty(){
		return loadProperties();
	}
	

	private static Properties prop = new Properties();
	private static String configFilePath = "./build.properties";

	
	/**
	 * Get all raw key-value from the configuration property file
	 * @return
	 */
	public static HashMap<String, String> loadProperties() {
		if (cachedProperty != null)
			return cachedProperty;
		else {
			cachedProperty = new HashMap<String, String>();
			try {
				prop.load(new FileInputStream(configFilePath));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Set<Object> keys=prop.keySet();
			for (Object key:keys){
				cachedProperty.put(key.toString(), prop.getProperty((String)key));
			}
			return cachedProperty;
		}
	}
	
	
	/**
	 * parse the variable by the concrete value
	 * @return
	 */
	private static String parseValue(String rawValue){
		String[] parts=rawValue.split("/");
		LinkedList<String> newValue=new LinkedList<String>();
		StringBuffer sb=new StringBuffer();
		for (int i=0; i<parts.length;i++){
			String part=parts[i];
			newValue.addLast(refinePartValue(part));
			sb.append(refinePartValue(part));
			if(i<parts.length-1) sb.append("/");
		}
		return sb.toString();
	}
	
	/**
	 * 
	 * @param part
	 * @return
	 */
	private static String refinePartValue(String part){
		if(!part.contains("$")) return part;
		String parsedPart=null;
		if(part.contains("$")){
			String key=part.substring(2,part.indexOf("}"));
			String rest=part.substring(part.indexOf("}")+1, part.length());
//			Debug.demoln(key);
			part=getCachedProperty().get(key);
			 parsedPart=parseValue(part)+rest;
		}
		return parsedPart;
		
	}
	

	public static String getValue(String name) {
		HashMap<String, String> map=loadProperties();
		String v=map.get(name);
		return parseValue(v);
	}
	
	
	

	public static void main(String[] arg) {
		String v = PropertyLoader.getValue(PropertyLoader.LTSOutputDir);
		logger.info(parseValue(v));
	}
}
