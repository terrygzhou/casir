package test.edu.rmit.casir.vpca;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.FileHandler;

public class EPCATest3 {

	static Logger logger = Logger.getLogger(EPCATest3.class);
	
	LabeledPCA unfoldedLPCA=null;

	String name = "bookshop2";

	String epcaFilePath = "./casestudy/epca/" + name + ".epca";

	Hashtable<String, VPCA> epcaTable;

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
		epcaTable = compiler.getVpcaTable();
	}

	@Test
	public void testLabeledPCA() {
		epcaTable.forEach((k, v) -> {
			logger.info(k);
//			v.getUnfoldingPCA();
			this.unfoldedLPCA = v.getUnfoldedLabeledPCA();
			APITest.printPCA(unfoldedLPCA.getPcaObj());
			logger.info(unfoldedLPCA.getPcaObj().states.length);
			logger.info(unfoldedLPCA.getPcaObj().maxStates);
			logger.info(unfoldedLPCA.getPcaObj().hasERROR());
//			logger.info(unfoldedLabelPCA.getPca().computeReliability());
			
			logger.info(unfoldedLPCA.getPcaObj().convertToDTMC());
			
			logger.info(unfoldedLPCA.getStateLabels());
		});
		
	}

}
