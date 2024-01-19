package test.edu.rmit.casir.lpca;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import api.PFSPCompiler;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.FileHandler;
import lts.CompactState;

/**
 * Testing a global labeled PCA composed by set of composed unfolded PCA. The
 * labeled PCA's states are labeled based on the its abstract PCA's state ID and
 * the VC if any; The global labeled PCA's state's label consists of the local
 * state's labels.
 * 
 * @author terryzhou
 *
 */
public class LabeledPCATest {

	String name = "bookshop2";

	String epcaFilePath = "./casestudy/epca/" + name + ".epca";
	String unfoldedPcaPath = "./casestudy/epca/output/" + name + ".pca";
	String compositeDTMCPath = "./casestudy/epca/output/" + name + ".pm";
	String composteDotPath = "./casestudy/epca/output/" + name + ".dot";

	static Logger logger = Logger.getLogger(LabeledPCATest.class);
	CompositeVPCA comEpca;

	Map<String, String> abstractPCA;

	Map<String, CompactStateRef> labeledPCA;

	Hashtable<String, VPCA> epcaTable;

	Vector<VPCA> epcas = new Vector<>();

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
		Map<String, CompactStateRef> localPCA = this.unfoldedPCA();
	}

	private Map<String, CompactStateRef> unfoldedPCA() {
		Map<String, CompactStateRef> unfoldedPCA = new HashMap<String, CompactStateRef>();
		PFSPCompiler comp = new PFSPCompiler();

		epcaTable.forEach((k, v) -> {
			String pfsp = v.unfoldPfsp();
			CompactState pca = comp.compile(k, pfsp);

			unfoldedPCA.put(k, new CompactStateRef(pca));
			try {
				FileHandler.appendPathFile(unfoldedPcaPath, new StringBuffer("\n" + pfsp));
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.epcas.add(v);
		});
		return unfoldedPCA;

	}

	@Test
	public void testAbsStateMap() {
		this.epcas.forEach(vpca -> {
			try {
				vpca.generateUnfoldLTS();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info("printing abstract pca");
			APITest.printPCA(vpca.getTemptlatePca().getPcaObj());
			logger.info("ending abstract pca");

			// Map<String, ExtState> allExtStates = epca.getExtStates();
			LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();

			logger.info("printing abstract pca");
			APITest.printPCA(lpca.getPcaObj());
			logger.info("ending abstract pca");

			lpca.getStateLabels().forEach((id, label) -> {
				logger.info("___________________________________________________");
				logger.info(id);
				logger.info(label);

				// ExtState v = lpca.getExtStateByStateID(id);
				logger.debug(vpca.getStateMap());
				ExtState v = vpca.getStateMap().get(id);
				/**
				 * Given an abstract stateID, find the extTransition of the lPCA by tracing the
				 * abstract transitions from the abstract state
				 */
				logger.info(v.getAbsStateID());
				logger.info(v.getFSPLabel());
				logger.info(v.getExtStateLabel());
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
