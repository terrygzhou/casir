package test.edu.rmit.casir.vpca;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.util.FileHandler;

public class CompactStateRefTest {

	Map<String, String> abstractPCA;
	Hashtable<String, VPCA> epcaTable;
	String name = "TravelAgent_6";
//	 String name ="eshop_hs5";

	String epcaFilePath = "./casestudy/epca/" + name + ".epca";
	String unfoldedPcaPath = "./casestudy/epca/output/" + name + ".pca";

	@Before
	public void setUp() throws Exception {
		VPCACompiler compiler;
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(epcaFilePath).toString();
			// logger.info("reading EPCA-FSP:" + "\n" + epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		compiler = new VPCACompiler(epfspStr);
		this.abstractPCA = compiler.getTemplatePCAMap();
		FileHandler.delFile(unfoldedPcaPath);
		FileHandler.appendPathFile(unfoldedPcaPath, new StringBuffer("pca"));
		epcaTable = compiler.getVpcaTable();
	}

	

//	@Test
	public void testOutputLTS() {
		fail("Not yet implemented");
	}

//	@Test
	public void testOutputLTS2() {
		fail("Not yet implemented");
	}
	
	
	@Test
	public void testCompareLTSCorrect() {
		VPCA vpca=epcaTable.get("FC");
		CompactStateRef absPCA=vpca.getTemptlatePca();
		absPCA.compareLTSCorrect();
//		vpca.getUnfoldedLabeledPCA();
		
	}
	

}
