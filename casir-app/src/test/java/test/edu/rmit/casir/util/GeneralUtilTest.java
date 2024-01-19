package test.edu.rmit.casir.util;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.rmit.casir.util.GeneralUtil;

public class GeneralUtilTest {
	private static Logger logger = Logger.getLogger(GeneralUtilTest.class);


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetTimeStample() {
		for (int i=0;i<30;i++){
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		logger.info(GeneralUtil.getTimeRandomSeqId()+"");
		}
		
		
	}

}
