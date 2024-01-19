package test.edu.rmit.casir.vpca;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import api.APITest;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.ExtTransition;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.pca.CompositePCA;
import edu.rmit.casir.util.FileHandler;
import lts.CompactState;
import lts.LTSException;
import lts.Transition;

public class CompositeVPCATest {
	static Logger logger = Logger.getLogger(CompositeVPCATest.class);
	CompositeVPCA comVpca;
	Map<String, String> abstractPCA;
	Hashtable<String, VPCA> epcaTable;
	Vector<VPCA> epcas = new Vector<>();
	 String name = "TravelAgent_6";
	// String name ="eShop_hs5_overflow";
//	 String name ="eShop1_simple";
//	String name = "trivial_shop";
//	String name = "furniture_simple";
//	String name = "sample";

	

	String epcaFilePath = "./casestudy/epca/" + name + ".epca";
	String unfoldedPcaPath = "./casestudy/epca/output/" + name + ".pca";
	String absPcaPath = "./casestudy/epca/output/" + name + "_abs.pca";
	String compositeDTMCPath = "./casestudy/epca/output/" + name + ".pm";
	String composteDotPath = "./casestudy/epca/output/" + name + ".dot";

	@Before
	public void setUp() throws Exception {
		VPCACompiler vPCACompiler;
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(epcaFilePath).toString();
			// logger.info("reading EPCA-FSP:" + "\n" + epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		vPCACompiler = new VPCACompiler(epfspStr);
		this.abstractPCA = vPCACompiler.getTemplatePCAMap();
		epcaTable = vPCACompiler.getVpcaTable();
		this.unfoldedPCA();
	}

	private void unfoldedPCA() {
		FileHandler.delFile(unfoldedPcaPath);
		try {
			FileHandler.appendPathFile(unfoldedPcaPath, new StringBuffer("pca \n"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		epcaTable.forEach((k, v) -> {
			v.testEPCA();
//			APITest.printPCA(v.getTemptlatePca().getPcaObj());
			logger.info("unfolding EPCA: \n" + v.getProcessName() + "\n " + v.unfoldPfsp());
			try {
				FileHandler.appendPathFile(unfoldedPcaPath,
						new StringBuffer("\n" + v.unfoldPfsp()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.epcas.add(v);
		});
	}

//	 @Test
	public void testUnfoldedLTS() throws Exception {
		for (VPCA vpca : this.epcas) {
			logger.info(vpca.visualizeLTS());
			Map<Integer, ExtState> extStateMap = vpca.getStateIDToExtStateMap();
			logger.info(vpca.getUnfoldedLabeledPCA().getLTS());
			ExtState endState=vpca.getExtStateByStateID(5);
			logger.info(vpca.getExtStates());
			for (int stateID : extStateMap.keySet()) {
				logger.info(stateID);
				logger.info(extStateMap.get(stateID));
			}
		}
		
	}

	// @Test
	public void testUnfoldedPCA() {
		epcaTable.forEach((k, v) -> {
			// v.testEPCA();
			logger.info(
					v.getProcessName() + "template PCA LTS is: " + v.getTemptlatePca().getLTS());
			logger.info("unfolding EPCA: \n" + v.getProcessName() + "\n" + v.unfoldPfsp());
			LabeledPCA localLPCA = v.getUnfoldedLabeledPCA();
			logger.info(localLPCA.getStateLabels());
		});
	}

//	 @Test
	public void testAbstractPCA() {
		FileHandler.delFile(absPcaPath);
		this.abstractPCA.entrySet().stream().forEach(entry -> {
			String pfspStr = entry.getValue();
			pfspStr.replace("pca", "");
			logger.debug(pfspStr);
			try {
				FileHandler.appendPathFile(absPcaPath, new StringBuffer(entry.getValue() + "\n"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("extracting the protocol (template PCA) model for EPCA:" + entry.getKey()
					+ "\n" + entry.getValue());
		});

	}

//	 @Test
	public void testCompose() {

		this.comVpca = new CompositeVPCA(null, this.epcas);

		CompositeLPCA comLpca = comVpca.getComposedLPCA();

		// gloExtStateMap makes sense
		Map<Integer, ExtState[]> gloExtStateMap = comLpca.getGlobStateToExtStatesMap();
		logger.info(comLpca.getGlobalPCA().getStateIDMap());

		for (int i : gloExtStateMap.keySet()) {
			logger.info("globalStateID " + i);
			ExtState[] localExtStates = gloExtStateMap.get(i);
			for (ExtState locExtstate : localExtStates) {
				logger.info("local stateID " + locExtstate.getStateID() + "\tlocal state label "
						+ locExtstate.getExtStateLabel());
			}
			logger.info("\n");
		}

		/**
		 * BUG:the covertEPCAToDot is inconsistent with
		 * comLpca.getGlobStateToExtStatesMap()
		 */

		logger.info(comLpca.getGraphvizDot());

		/**
		 * the following is for testing composition.
		 */

		for (VPCA vpca : this.epcas) {
			LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();
			logger.info(lpca.getStateLabels());
		}

	}

	 @Test
	public void testDotGraph() {
		this.comVpca = new CompositeVPCA(null, this.epcas);
		CompactState pca = comVpca.getComposedPCA().getPcaObj();
		APITest.printPCA(pca);
		// String dotString=pca.convertToGraphviz();
		String dotString = comVpca.getComposedLPCA().getGraphvizDot2();
		logger.info(dotString);
		try {
			FileHandler.outputPathFile(this.composteDotPath, new StringBuffer(dotString));
		} catch (LTSException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	 
	 
//	 @Test
	 public void testGetTraces() {
		 this.comVpca = new CompositeVPCA(null, this.epcas);
		 comVpca.getComposedLPCA().getExtStateTraces(100);
	 }

	// @Test
	public void testPCAStateToExtStateMap() {
		this.comVpca = new CompositeVPCA(null, this.epcas);
		CompositePCA composite = new CompositePCA(comVpca.getComposedPCA(),
				comVpca.getUnfoldedPCAs());
		// logger.info(composite.convertPCAToDTMC());
		/**
		 * globLocStateMap may not be correct by composite.buildGloLocStateMap() Note:
		 * globLocStateMap is the just stateID mapping without labels mapping
		 */
		Map<Integer, Vector<Integer>> globLocStateMap = composite.buildGloLocStateMap();
		logger.info(globLocStateMap);

		/**
		 * mapping from global state to local labels
		 */
		// Map<Integer, ExtState[]> globLocExtStateMap =
		// comEpca.getGlobStateToExtStatesMap();
		Map<Integer, ExtState[]> globLocExtStateMap = comVpca.getComposedLPCA()
				.getGlobStateToExtStatesMap();

		for (int i : globLocExtStateMap.keySet()) {
			logger.info("\n");
			/**
			 * GloState is state ID
			 */
			logger.info("GloState " + i);
			ExtState[] extStateArr = globLocExtStateMap.get(i);

			for (ExtState s : extStateArr) {

				/**
				 * s.getStateID is the abstract stateID which is part of the label
				 * s.getExtStateLabel is a part of the state label
				 */
				logger.info(s.getAbsStateID() + "\t" + s.getExtStateLabel());
			}
		}
	}

//	 @Test
	public void testDTMC() throws Exception {
		// CompositePCA composite = new CompositePCA(comEpca.getCompositePcaRef(),
		// comEpca.getUnfoldedPcaRefs());
		// logger.info(composite.getComposite().convertPCAToDTMC());
		this.comVpca = new CompositeVPCA(null, this.epcas);
		
		logger.info(this.comVpca.getComposedLPCA().getDTMC());
		
		String prismModel = this.comVpca.getComposedLPCA().getDTMC();
		
		logger.info("Generating prism DTMC for composite: " + comVpca.getCompositeName() + "\n"
				+ prismModel);
		FileHandler.outputPathFile(this.compositeDTMCPath, new StringBuffer(prismModel));
	}

//	 @Test
	public void testExplore() throws Exception {
		this.comVpca = new CompositeVPCA(null, this.epcas);

		CompactState gloPca = comVpca.getComposedPCA().getPcaObj();

		APITest.printPCA(gloPca);
		logger.info(gloPca.convertToDTMC());
		logger.info(this.comVpca.getComposedLPCA().getDTMC());
	}

	// @Test
	public void testUnfoldedLocalLabeledPCA() throws Exception {
		this.comVpca = new CompositeVPCA(null, this.epcas);

		for (VPCA epca : this.epcas) {
			LabeledPCA lpca = epca.getUnfoldedLabeledPCA();
			lpca.print();
		}
	}

	// @Test
	public void testMapLocalStateLabels() {
		this.comVpca = new CompositeVPCA(null, this.epcas);
		this.comVpca.getComposedLPCA().getGlobStateToExtStatesMap();
	}

	/**
	 * Given a globalStateID, find all local extStates
	 */
	// @Test
	public void testGetlocalExtStates() {
		this.comVpca = new CompositeVPCA(null, this.epcas);
		int gloStateID = 17;
		CompositeLPCA globalLPCA = this.comVpca.getComposedLPCA();
		Map<Integer, ExtState[]> localExtStates = globalLPCA.getGlobStateToExtStatesMap();
		// Map<Integer, ExtState[]>
		// localExtStates=this.comEpca.getComposedLPCA().getGlobStateToExtStatesMap();
		logger.info("global stateID " + gloStateID);
		ExtState[] eStateArr = localExtStates.get(gloStateID);
		for (ExtState eState : eStateArr) {
			logger.info(eState.getExtStateLabel());
		}
	}

	/**
	 * this tests Local unfolded PCA's <stateID, ExtState> mapping and the global
	 * unfolded PCA's state mapping <gloStateID, Vector<localStateID>> where the
	 * localStateID maps to above local unfolded PCA's state mapping;
	 * <p>
	 * Also the unfolded composite PCA's DTMC and Dot generation align with the
	 * above state mapping
	 */
//	@Test
	public void testStateMapping() {
		/**
		 * Get local unfolded PCA mapping
		 */
		for (VPCA vpca : this.epcas) {
			logger.info(vpca.getStateIDToExtStateMap());
			logger.info(vpca.visualizeLTS());
			LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();
			lpca.print();
		}

		/**
		 * composition of vpca
		 */
		this.comVpca = new CompositeVPCA(null, this.epcas);
		CompositeLPCA gloLpca = this.comVpca.getComposedLPCA();
		CompositePCA comPCA = gloLpca.getGlobalPCA();

		logger.info(comPCA.getStateIDMap());
		Map<Integer, ExtState[]> localExtStates = gloLpca.getGlobStateToExtStatesMap();
		localExtStates.forEach((k, v) -> {
			logger.info("globalStateID " + k);
			for (ExtState st : v) {
				logger.info("locStateID " + st.getStateID());
				logger.info("label " + st.getExtStateLabel());
			}
			logger.info("\n");
		});

		/**
		 * display compositePCA's lts
		 */
		Map<Integer, Set<Transition>> lts = comPCA.getComposite().getLTS();
		for (int stateID : lts.keySet()) {
			logger.info("state " + stateID);
			Set<Transition> outTrans = lts.get(stateID);
			outTrans.forEach(t -> {
				logger.info("label " + t.getLabel() + " toState " + t.getTo());
			});
			logger.info("\n");
		}

		/**
		 * visualise the composed model
		 */
		logger.info(comPCA.getComposite().getPcaObj().convertToGraphviz());
		
		
		/**
		 * testing dot generation with local variables
		 */
		logger.info(gloLpca.getGraphvizDot());
		
		

	}

}
