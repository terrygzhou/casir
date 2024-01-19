package test.edu.rmit.casir.epca.parser;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.pca.PCAUtil;
import edu.rmit.casir.util.FileHandler;
import lts.CompactState;

public class VPCACompilerTest {
	Logger logger = Logger.getLogger(VPCACompilerTest.class);
	VPCACompiler compiler;
	 String epcaFilePath="./casestudy/epca/TravelAgent_3.epca";
//	String epcaFilePath = "./casestudy/epca/bookshop2.epca";
	

	@Before
	public void setUp() throws Exception {
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(this.epcaFilePath).toString();
			logger.info(epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.compiler = new VPCACompiler(epfspStr);
	}

	@Test
	public void testCompiledVPCA() {
		logger.info(compiler.getTemplatePCAMap());
		logger.info(compiler.getVpcaTable());
		Enumeration enu = compiler.getVpcaTable().elements();
		while (enu.hasMoreElements()) {
			VPCA epca = (VPCA) enu.nextElement();
			epca.testEPCA();
			APITest.printPCA(epca.getTemptlatePca().getPcaObj());
			logger.info(epca.unfoldPfsp());
		}
	}

	// @Test
	public void testNormalisation() {
		Enumeration enu = compiler.getVpcaTable().elements();
		while (enu.hasMoreElements()) {
			VPCA epca = (VPCA) enu.nextElement();
			CompactState tempPCA = epca.getTemptlatePca().getPcaObj();
			APITest.printPCA(tempPCA);
			PCAUtil.normalisePCA(tempPCA);
		}
	}

	// @Test
	public void test() {
	}
}
