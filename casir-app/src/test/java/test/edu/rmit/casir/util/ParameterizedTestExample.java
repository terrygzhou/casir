package test.edu.rmit.casir.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class ParameterizedTestExample {
	static Logger logger=Logger.getLogger(ParameterizedTestExample.class);
	private String datum;
	private String expectedResult;
	
	
	public ParameterizedTestExample(String datum, String expected){
		this.datum=datum;
		this.expectedResult=expected;
	}
	
	@Parameters
	public static Collection<Object[]> generateData(){
		Object[][] objArray=new Object[][]{{"AGCCG", "AGTTA"},{"AGTTA", "GATCA"}};
		return Arrays.asList(objArray);
	}
	
	

	@Test
	public void testSomething() {
		logger.info(this.datum);
		logger.info(this.expectedResult);
	}

}
