package test.edu.rmit.casir.vpca;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import api.APITest;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.pca.CompositePCA;
import edu.rmit.casir.util.FileHandler;
import lts.CompactState;
import lts.LTSException;
import lts.Transition;

@RunWith(Parameterized.class)
public class ParameterisedCompositeEPCATest {
	
	static Logger logger = Logger.getLogger(CompositeVPCATest.class);

	private String epcaFilePath;
	private String unfoldedPcaPath;
	private String compositeDTMCPath;// =;
	private String composteDotPath;// =;

	CompositeVPCA comEpca;
	Vector<CompactStateRef> upcas = new Vector();
	Vector<VPCA> epcas = new Vector<>();

	public ParameterisedCompositeEPCATest(String epcaFilePath, String unfoldedPcaPath,
			String composteDotPath, String compositeDTMCPath) {
		this.epcaFilePath = epcaFilePath;
		this.unfoldedPcaPath = unfoldedPcaPath;
		this.composteDotPath = composteDotPath;
		this.compositeDTMCPath = compositeDTMCPath;
	}

	/**
	 * Parameterized test cases
	 * <p>
	 * Need more complex template PCA examples!
	 * 
	 * @return
	 */
	@Parameters
	public static Collection<Object[]> generateData() {
		Object[][] objArray = new Object[][] {
				{ "./casestudy/epca/TravelAgent_deadlock.epca",
						"./casestudy/epca/output/TravelAgent_deadlock.pca",
						"./casestudy/epca/output/TravelAgent_deadlock.dot",
						"./casestudy/epca/output/TravelAgent_deadlock_dtmc.nm" },
				{ "./casestudy/epca/eshop_hs8.epca", "./casestudy/epca/output/eshop_hs8.pca",
						"./casestudy/epca/output/eshop_hs8.dot",
						"./casestudy/epca/output/eshop_hs8_dtmc.nm" },
				{ "./casestudy/epca/eshop_hs7.epca", "./casestudy/epca/output/eshop_hs7.pca",
						"./casestudy/epca/output/eshop_hs7.dot",
						"./casestudy/epca/output/eshop_hs7_dtmc.nm" },
				{ "./casestudy/epca/eshop_hs2.epca", "./casestudy/epca/output/eshop_hs2.pca",
						"./casestudy/epca/output/eshop_hs2.dot",
						"./casestudy/epca/output/eshop_hs2_dtmc.nm" },
				{ "./casestudy/epca/eshop_hs3.epca", "./casestudy/epca/output/eshop_hs3.pca",
						"./casestudy/epca/output/eshop_hs3.dot",
						"./casestudy/epca/output/eshop_hs3_dtmc.nm" },
				{ "./casestudy/epca/eshop_hs4.epca", "./casestudy/epca/output/eshop_hs4.pca",
						"./casestudy/epca/output/eshop_hs4.dot",
						"./casestudy/epca/output/eshop_hs4_dtmc.nm" },
				{ "./casestudy/epca/eshop_hs5.epca", "./casestudy/epca/output/eshop_hs5.pca",
						"./casestudy/epca/output/eshop_hs5.dot",
						"./casestudy/epca/output/eshop_hs5_dtmc.nm" },
				{ "./casestudy/epca/eshop.epca", "./casestudy/epca/output/eshop.pca",
						"./casestudy/epca/output/eshop.dot",
						"./casestudy/epca/output/eshop_dtmc.nm" },
				{ "./casestudy/epca/eshop1.epca", "./casestudy/epca/output/eshop1.pca",
						"./casestudy/epca/output/eshop1.dot",
						"./casestudy/epca/output/eshop1_dtmc.nm" },
				{ "./casestudy/epca/eshop2.epca", "./casestudy/epca/output/eshop2.pca",
						"./casestudy/epca/output/eshop2.dot",
						"./casestudy/epca/output/eshop2_dtmc.nm" },
				{ "./casestudy/epca/local.epca", "./casestudy/epca/output/local.pca",
						"./casestudy/epca/output/local.dot",
						"./casestudy/epca/output/local_dtmc.nm" },
				// this one has an issue!
				{ "./casestudy/epca/sample.epca", "./casestudy/epca/output/sample.pca",
						"./casestudy/epca/output/sample.dot",
						"./casestudy/epca/output/sample_dtmc.nm" },
				{ "./casestudy/epca/novar.epca", "./casestudy/epca/output/novar.pca",
						"./casestudy/epca/output/novar.dot",
						"./casestudy/epca/output/novar_dtmc.nm" },
				{ "./casestudy/epca/TravelAgent_1.epca",
						"./casestudy/epca/output/TravelAgent_1.pca",
						"./casestudy/epca/output/TravelAgent_1.dot",
						"./casestudy/epca/output/TravelAgent_1_dtmc.nm" },
				{ "./casestudy/epca/TravelAgent_2.epca",
						"./casestudy/epca/output/TravelAgent_2.pca",
						"./casestudy/epca/output/TravelAgent_2.dot",
						"./casestudy/epca/output/TravelAgent_2_dtmc.nm" },
				{ "./casestudy/epca/TravelAgent_3.epca",
						"./casestudy/epca/output/TravelAgent_3.pca",
						"./casestudy/epca/output/TravelAgent_3.dot",
						"./casestudy/epca/output/TravelAgent_3_dtmc.nm" },
				{ "./casestudy/epca/travelagent_4_work.epca",
						"./casestudy/epca/output/travelagent_4_work.pca",
						"./casestudy/epca/output/travelagent_4_work.dot",
						"./casestudy/epca/output/travelagent_4_work_dtmc.nm" },
				{ "./casestudy/epca/unhandled_error.epca",
						"./casestudy/epca/output/unhandled_error.pca",
						"./casestudy/epca/output/unhandled_error.dot",
						"./casestudy/epca/output/unhandled_error_dtmc.nm" },

		};
		return Arrays.asList(objArray);
	}

	@Before
	public void setUp() throws Exception {
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(epcaFilePath).toString();
			logger.info("reading EPCA-FSP:" + "\n" + epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		VPCACompiler compiler = new VPCACompiler(epfspStr);
		// logger.info(compiler.getEpcaTable());
		for (String pid : compiler.getTemplatePCAMap().keySet()) {
			logger.info("extracting the protocol (template PCA) model for EPCA:" + pid + "\n"
					+ compiler.getTemplatePCAMap().get(pid));
		}
		Enumeration enu = compiler.getVpcaTable().elements();
		FileHandler.delFile(unfoldedPcaPath);
		FileHandler.appendPathFile(unfoldedPcaPath, new StringBuffer("pca"));
		while (enu.hasMoreElements()) {
			VPCA epca = (VPCA) enu.nextElement();
			epca.testEPCA();
			logger.info("template PCA: ");
			APITest.printPCA(epca.getTemptlatePca().getPcaObj());
			// APITest.printPCA(PCAUtil.normalisePCA(epca.getTemptlatePca()));
			logger.info("unfolding EPCA: " + epca.getProcessName() + "\n" + epca.unfoldPfsp());
			FileHandler.appendPathFile(unfoldedPcaPath, new StringBuffer("\n" + epca.unfoldPfsp()));
			this.epcas.add(epca);
		}
	}

	@Test
	public void testCompose() {
		comEpca = new CompositeVPCA("COM", this.epcas);
		// CompactState
		// normalPCA=PCAUtil.normalisePCA(comEpca.getCompositePcaRef().getPca());
		APITest.printPCA(comEpca.getComposedPCA().getPcaObj());
		Set<Transition> outputTransitions = comEpca.getComposedPCA()
				.getOutTransFromState(0);
		logger.info(outputTransitions);
	}

	@Test
	public void testDotGraph() {
		comEpca = new CompositeVPCA("PQ", this.epcas);
		// CompactState pca = comEpca.getCompositePcaRef().getPca();
		// APITest.printPCA(pca);
//		String dotString = comEpca.getComposedLPCA().convertVpcaToDot();
		String dotString = comEpca.getComposedLPCA().getGraphvizDot();
		logger.info(dotString);
		try {
			FileHandler.outputPathFile(this.composteDotPath, new StringBuffer(dotString));
		} catch (LTSException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// CompactState nPca=PCAUtil.normalisePCA(pca);
		// logger.info(nPca.convertToGraphviz());
	}

	@Test
	public void testPCAStateMapping() {
		comEpca = new CompositeVPCA(null, this.epcas);
		// CompositePCA composite = new CompositePCA(comEpca.getComposedPCA(),
		// comEpca.getUnfoldedPCAs());
		CompositePCA composite = comEpca.getComposedLPCA().getGlobalPCA();

		// logger.info(composite.getComposite().getPca().convertToGraphviz());
		Map<Integer, Vector<Integer>> globLocStateMap = composite.buildGloLocStateMap();
		logger.info(globLocStateMap);
	}

	@Test
	public void testPCAStateToExtStateMap() {
		comEpca = new CompositeVPCA("PQ", this.epcas);
		CompositePCA composite = new CompositePCA(comEpca.getComposedPCA(),
				comEpca.getUnfoldedPCAs());
		// logger.info(composite.convertEPCAToDTMC());
		// logger.info(composite.getComposite().getPca().convertToGraphviz());
		logger.info(composite.getComposite().convertPCAToDTMC());
		// logger.info(composite.getComposite().getPca().convertToDTMC());
		Map<Integer, Vector<Integer>> globLocStateMap = composite.buildGloLocStateMap();
		logger.info(globLocStateMap);
		// Map<Integer, ExtState[]> globLocExtStateMap =
		// comEpca.getGlobStateToExtStatesMap();
		Map<Integer, ExtState[]> globLocExtStateMap = comEpca.getComposedLPCA()
				.getGlobStateToExtStatesMap();// .getGlobStateToExtStatesMap();
		for (int i : globLocExtStateMap.keySet()) {
			logger.info("\n");
			logger.info("GloState " + i);
			ExtState[] extStateArr = globLocExtStateMap.get(i);
			for (ExtState s : extStateArr) {
				logger.info(s.getExtStateLabel());
			}
		}
	}

	@Test
	public void testDTMC() throws Exception {
		comEpca = new CompositeVPCA("PQ", this.epcas);
//		String prismModel = comEpca.convertVPCAToDTMC();
		String prismModel = comEpca.getComposedLPCA().getDTMC();
		
		logger.info("generating prism DTMC for composite: " + comEpca.getCompositeName() + "\n"
				+ prismModel);
		FileHandler.outputPathFile(this.compositeDTMCPath, new StringBuffer(prismModel));
	}

	// @Test
	public void testDummy() {

	}

}
