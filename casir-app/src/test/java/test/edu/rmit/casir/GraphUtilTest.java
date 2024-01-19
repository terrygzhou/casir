package test.edu.rmit.casir;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.rmit.casir.util.GraphUtil;

import pipe.dataLayer.DataLayer;

public class GraphUtilTest {
	DataLayer pn;
	String pnFilePath="/Users/terryzhou//PhD/Thesis/case_study/pipe2/compensation_tasks/put_order.xml";
	String dotFilePath="/Users/terryzhou//PhD/Thesis/case_study/pipe2/compensation_tasks/put_order.dot";

	@Before
	public void setUp() throws Exception {
		pn = new DataLayer(pnFilePath);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testConvertPNToDot() {
		GraphUtil.convertPNToDot(pn, dotFilePath);
		
//		fail("Not yet implemented");
	}

}
