package test.edu.rmit.casir.vpca;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ EPCATest1.class, EPCATest2.class })
public class EPCATestSuite {
	EPCATest1 t1;
	EPCATest2 t2;
	Logger logger = Logger.getLogger(EPCATestSuite.class);

	@Test
	public void testall() {
		String unfoldingP = t1.getUnfoldPFSP();
		String unfoldingQ = t2.getUnfoldPFSP();
		logger.info(unfoldingP);
	}
}
