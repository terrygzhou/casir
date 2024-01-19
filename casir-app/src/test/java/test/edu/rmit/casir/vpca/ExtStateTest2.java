package test.edu.rmit.casir.vpca;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.FileHandler;

public class ExtStateTest2 {

	Logger logger = Logger.getLogger(ExtStateTest2.class);

	String epcaFilePath = "./casestudy/epca/bookshop2.epca";

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
		VPCACompiler compiler = new VPCACompiler(epfspStr);

		Enumeration enu = compiler.getVpcaTable().elements();
		while (enu.hasMoreElements()) {
			VPCA epca = (VPCA) enu.nextElement();
			epcaVec.add(epca);
			logger.info(compiler.getTemplatePCAMap());
		}

	}

	@Test
	public void testGetAbsStateID() {
		this.epcaVec.forEach(epca -> {
			try {
				epca.generateUnfoldLTS();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info("printing abstract pca");
			APITest.printPCA(epca.getTemptlatePca().getPcaObj());
			logger.info("ending abstract pca");
			
			
//			Map<String, ExtState> allExtStates = epca.getExtStates();
			LabeledPCA lpca = epca.getUnfoldedLabeledPCA();

			logger.info("printing abstract pca");
			APITest.printPCA(lpca.getPcaObj());
			logger.info("ending abstract pca");

			lpca.getStateLabels().forEach((id, label) -> {
				logger.info("___________________________________________________");
				logger.info(id);
				logger.info(label);
//				ExtState v = lpca.getExtStateByStateID(id);
//				logger.info(v.getAbsStateID());
//				logger.info(v.getFSPLabel());
//				logger.info(v.getExtStateLabel());
				logger.info("___________________________________________________");
			});
			/*
			 * allExtStates.forEach((k,v)->{ logger.info(k); logger.info(v.getAbsStateID());
			 * logger.info(v.getFSPLabel()); logger.info(v.getExtStateLabel()); });// for
			 * each extState
			 */
		});// for each epca
	}

}
