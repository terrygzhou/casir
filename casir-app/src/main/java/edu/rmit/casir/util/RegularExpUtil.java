package edu.rmit.casir.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class RegularExpUtil {
	static Logger logger=Logger.getLogger("edu.rmit.casir.util.RegularExpUtil");

	
	public RegularExpUtil() {
	}
	
	public static void testMatcher(){
		Pattern p=Pattern.compile("a*b");
		Matcher m=p.matcher("aaaab");
		boolean b=m.matches();
		logger.info(b);
		
	}

	
	

	    public static void testSample() {

	        String text    =
	                  "John writes about this, and John writes about that," +
	                          " and John writes about everything. "
	                ;

	        String patternString1 = "(John)";

	        Pattern pattern = Pattern.compile(patternString1);
	        Matcher matcher = pattern.matcher(text);

	        while(matcher.find()) {
	            System.out.println("found: " + matcher.group(1)+"\t"+matcher.group(2));
	        }
	    }
	    
	
	public static void testGroup(){
		String pattern="{([.*?])}";
		String text="a{[if(v1<2&v2=1):(v1,v2):=(v1+1, (v2+1)%2)]^" +
				"[if(v1<2&v2=0):(v1,v2):=(v1+1,v2)] " +
				"^[if(v1=2):(v1,v2):=(v1-2,v2)] ^[else:(v1,v2):=(v1,v2)]}";
		Pattern p=Pattern.compile(pattern);
		Matcher m=p.matcher(text);
		while(m.find()){
			logger.info("found: "+m.group(1));
			logger.info(m.group(2));
		}
		
	}
	
}
