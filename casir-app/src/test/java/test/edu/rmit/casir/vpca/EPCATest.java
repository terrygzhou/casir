package test.edu.rmit.casir.vpca;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.util.FileHandler;
import lts.CompactState;
import test.edu.rmit.casir.epca.parser.VPCACompilerTest;

public class EPCATest {
	Logger logger = Logger.getLogger(VPCACompilerTest.class);
	VPCACompiler compiler;
	String epcaFilePath = "./casestudy/epca/TravelAgent_3.epca";
	// String epcaFilePath="./casestudy/epca/TravelAgent_deadlock.epca";
	// String epcaFilePath="./casestudy/epca/hotel_agency.epca";
	Vector<VPCA> epcaVec = new Vector<>();

	@Before
	public void setUp() throws Exception {
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(this.epcaFilePath).toString();
			// logger.info(epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.compiler = new VPCACompiler(epfspStr);

		Enumeration enu = compiler.getVpcaTable().elements();
		while (enu.hasMoreElements()) {
			VPCA epca = (VPCA) enu.nextElement();
			epcaVec.add(epca);
			logger.info(compiler.getTemplatePCAMap());

		}
	}

	@Test
	public void testAbstractPCA() {
		this.epcaVec.forEach(epca -> {
			CompactState absPCA = epca.getTemptlatePca().getPcaObj();
			APITest.printPCA(absPCA);
		});

	}

	@Test
	public void testUnfolding() {
		this.epcaVec.forEach(epca -> {
			try {
				logger.info(epca.generateUnfoldLTS());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info(epca.unfoldPfsp());
			logger.info(epca.normalizeExtLTS());

		});
	}

	@Test
	public void testStateMapping() {
		this.epcaVec.forEach(epca -> {
//			logger.info(epca.getStateIDToExtStateMap());
			logger.info(epca.getStateIDToExtStateMap());
		});
	}

	@Test
	public void testExtState() {

	}

}
