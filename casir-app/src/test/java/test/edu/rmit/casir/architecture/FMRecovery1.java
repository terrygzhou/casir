package test.edu.rmit.casir.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import api.PFSPCompiler;
import edu.rmit.casir.architecture.Binding;
import edu.rmit.casir.architecture.Configuration;
import edu.rmit.casir.architecture.Framework;
import edu.rmit.casir.architecture.Gate;
import edu.rmit.casir.architecture.GateP;
import edu.rmit.casir.architecture.GateR;
import edu.rmit.casir.architecture.Ken;
import edu.rmit.casir.architecture.KenBasic;
import edu.rmit.casir.architecture.testBed.TestBed;
import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.FileHandler;
import edu.rmit.casir.verification.CounterexampleComicsImpl;
import edu.rmit.casir.verification.PctlProperty;
import lts.CompactState;

/**
 * This the client for the testing
 * 
 * @author terryzhou
 *
 */
public class FMRecovery1 {
	static Logger logger = Logger.getLogger(FMRecovery1.class);
	CompositeVPCA comVpca;
	Map<String, String> abstractPCA;
	Hashtable<String, VPCA> epcaTable;
	Vector<VPCA> epcas = new Vector<>();
	Vector<Ken> kens = new Vector<>();
	Configuration conf;
	Framework framework;
	TestBed tb;

	// folders configuration
	String name = "furniture_simple";
	String rootDir = "./casestudy/thesis/furniture_simple/";
	String rootOutputDir = rootDir + "BeforeRecovery/";

	// for Rec1 recovery
	String rec1Dir = rootDir + "Rec1/";
	String rec1OutputDir = rec1Dir + "output/";
	String rec1OutputDotDir = rec1OutputDir + "dot/";
	String rec1OutputDtmcDir = rec1OutputDir + "dtmc/";
	String rec1OutputSimDir = rec1OutputDir + "simulation/";

	/**
	 * Input file
	 */
	String epcaFilePath = rootDir + name + ".epca";

	/**
	 * output files below
	 */
	// String compositeDTMCPath = outputDtmcDir + name + "_rec1_dtmc.pm";
	// String compositeMRMPath = rec1OutputDotDir + name + "_rec1_";
	// String composteDotPath = outputDotDir + name + "_rec1.dot";
	String counterexampleFile = rootDir + "BeforeRecovery/" + "counter_example.path";
	String stateMapFile = rootOutputDir + name + ".sta";

	/**
	 * To be replaced by automation
	 */
	String rec1Path = rec1Dir + name + "_rec1.pca";

	@Before
	public void setUp() throws Exception {

		try {
			Files.createDirectories(Paths.get(this.rootDir));
			Files.createDirectories(Paths.get(this.rootOutputDir));
			Files.createDirectories(Paths.get(this.rec1Dir));
			Files.createDirectories(Paths.get(this.rec1OutputDir));
			Files.createDirectories(Paths.get(this.rootOutputDir + "unfoledPFSP/"));
			Files.createDirectories(Paths.get(this.rec1OutputDotDir));
			Files.createDirectories(Paths.get(this.rec1OutputDtmcDir));
			Files.createDirectories(Paths.get(this.rec1OutputSimDir));

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		VPCACompiler vPCACompiler;
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(epcaFilePath).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}

		vPCACompiler = new VPCACompiler(epfspStr);
		epcaTable = vPCACompiler.getVpcaTable();
		Set<Ken> kenSet = new HashSet<>();
		Set<Binding> bindings = new HashSet<>();

		Vector<LabeledPCA> localLPCA = new Vector<>();

		for (String k : epcaTable.keySet()) {
			KenBasic ken = new KenBasic(k);
			VPCA vpca = epcaTable.get(k);
			vpca.outputUnfoldedFSP(rootOutputDir + "UnfoledPFSP/" + k + "_unfoled.pca");
			ken.setVpca(vpca);
			// set k's variables
			ken.setVariables(vpca.getVariables());
			kenSet.add(ken);
			this.kens.add(ken);
			LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();
			lpca.outputDotFigure(rootOutputDir + k + "_unfolded.dot");
			lpca.setVpca(vpca);
			lpca.setName(vpca.getProcessName());
			ken.setLpca(lpca);
			localLPCA.add(lpca);
		}

		// set up global model
		Vector<VPCA> vpcaVec = new Vector<>();
		this.epcaTable.keySet().forEach(vpcaName -> {
			VPCA vpca = this.epcaTable.get(vpcaName);
			logger.info(vpca.unfoldPfsp());
			vpcaVec.add(vpca);
		});

		CompositeVPCA compositeVpca = new CompositeVPCA("ALL", vpcaVec);
		this.comVpca = compositeVpca;
		CompositeLPCA gloModel = new CompositeLPCA(localLPCA);
		gloModel.outputDotFile(this.rootOutputDir + this.name + ".dot");
		gloModel.outputDTMCFile(this.rootOutputDir + this.name + ".pm");

		// set Kens and connections
		Map<String, Gate> gateMap = new HashMap<>();
		kenSet.forEach(ken -> {
			logger.debug(ken.getName());
			if (ken.getName().equals("FM")) {
				GateR r = new GateR("RGate_FM");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("RGate_FM", r);
			} else {
				GateP p = new GateP("PGate_WD");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("PGate_WD", p);
			}
		});

		// set bindings
		Binding b = new Binding("order_wood", gateMap.get("RGate_FM"), gateMap.get("PGate_WD"));
		bindings.add(b);

		// set AC
		this.conf = new Configuration(kenSet, bindings);
		this.conf.setGloModel(gloModel);
		logger.debug(this.conf.getGloModel().getGlobalPCA().getGlobalLTS());
		PctlProperty pro = new PctlProperty("IC1", "S=? [wd_owed>0]", this.createVariables());
		pro.setProperty("reliability", "S=? [!\"error\"]");
		conf.setPctlProperty(pro);

		// set the framework by providing the output files
		this.framework = new Framework(this.rec1OutputDtmcDir + this.name + "_rec1.pm",
				this.rec1OutputDtmcDir + this.name + "_rec1",
				this.rec1OutputDotDir + this.name + "_rec1.dot");

		conf.print();
	}

	@Test
	public void testGloRecovery() {

		/**
		 * using COMICS to generate the counterexample and set it into the framework
		 */
		CounterexampleComicsImpl counterExam = new CounterexampleComicsImpl(this.counterexampleFile,
				this.stateMapFile);
		this.framework.setCounterexample(counterExam);

		/**
		 * Some manual work here to generate counterexample and identify the global
		 * state 2
		 */
		// proactive recovery starting from gloState=2
		Map<String, Integer> inconsistency = framework.localiseInconsistency(conf, "IC1", 2);

		Ken connector = null;
		int locState = 0;

		// For each local Ken, generate all recovered configurations
		for (String kenID : inconsistency.keySet()) {
			connector = this.conf.getKenByName(kenID);
			locState = inconsistency.get(kenID);
			Vector<LabeledPCA> candidateLPCAVec = this.getCandidateLpca();
			/**
			 * manually set the transformed Recoveries
			 */
			for (LabeledPCA transformedLpca : candidateLPCAVec) {
				framework.setRecLocLpca(kenID, transformedLpca);
				setCostMap(conf);

				// for Ken connector, from its local state "locState", generating all possible
				// recovered configurations.
				Configuration afterConf = framework.recovery(conf, connector, locState);

				if (afterConf == null)
					continue;
				this.setCostMap(afterConf);

				/**
				 * Evaluate each reconfigured architecture by simulating the runs
				 */
				this.evaluate(afterConf, 100);
			} // for each candidate recovery
		}
	}

	/**
	 * Evaluate the transformed configuration
	 * 
	 * @param afterConf
	 */
	private void evaluate(Configuration afterConf, int bound) {
		Vector<String> outputDataPathVec = new Vector<>();

		// max 99 resources
		for (int i = 0; i < 99; i++) {
			String outputData = rec1OutputSimDir + name + "_rec1_output_" + i + ".txt";
			outputDataPathVec.add(outputData);
		}
		int i = 0;
		Map<String, Map<String, Double>> oneCost = afterConf.getCostMap();

		Set<String> startActions = new HashSet<>();
		startActions.add("avail_1");
		startActions.add("~na_1");

		Set<String> completeActions = new HashSet<>();
		completeActions.add("confirm_amount_1");
		completeActions.add("nowood");

		for (String resource : oneCost.keySet()) {
			Map<String, Double> cost = oneCost.get(resource);
			this.tb = new TestBed(afterConf, outputDataPathVec.get(i), startActions,
					completeActions, cost);
			i++;
			tb.evaluate(afterConf, bound);
		}
	}

	/**
	 ************************ Sample creation **************************
	 */

	/**
	 * Setting both time cost and dollar cost
	 * 
	 * @param conf
	 */
	private void setCostMap(Configuration conf) {
		Map<String, Map<String, Double>> costMaps = new HashMap<>();

		Map<String, Double> timeCost = new HashMap<>();
		timeCost.put("nowood", 1.1);
		timeCost.put("confirm_amount_1", 3.1);
		timeCost.put("wood_check", 0.2);
		timeCost.put("orderWood_orderWood__amount_1", 3.1);
		timeCost.put("order_ack", 1.0);
		costMaps.put("time", timeCost);

		Map<String, Double> dollarCost = new HashMap<>();
		dollarCost.put("nowood", 6.5);
		dollarCost.put("confirm_amount_1", 140.);
		// costMap.put("wood_check", 0.2);
		dollarCost.put("orderWood_orderWood__amount_1", 90.0);
		dollarCost.put("order_ack", 10.0);
		costMaps.put("dollar", dollarCost);

		conf.setCostMap(costMaps);
	}

	/**
	 * manually provide the recovered local model as LPCA
	 * 
	 * @return
	 */
	private Vector<LabeledPCA> getCandidateLpca() {
		Set<VariableType> variables = this.createVariables();
		Vector<LabeledPCA> candLPCAVec = new Vector<>();
		LabeledPCA transformedLpca = null;
		try {
			transformedLpca = this.manualTransformLPCA(rec1Path, "WD_REC1",
					this.getStateLableForRec(), variables);
		} catch (IOException e) {
			e.printStackTrace();
		}
		candLPCAVec.add(transformedLpca);
		return candLPCAVec;
	}

	/**
	 * Manually create the transformed local connector, by supplying the FSP of the
	 * recovered local component and state-label annotation in format of the
	 * Map<Integer, Map<String, Object>>
	 * 
	 * @param pfspPath
	 * @param varLabels
	 * @return
	 * @throws IOException
	 */
	private LabeledPCA manualTransformLPCA(String pfspPath, String processName,
			Map<Integer, Map<String, Object>> varLabels, Set<VariableType> vars)
			throws IOException {
		PFSPCompiler comp = new PFSPCompiler();
		String fspStr = FileHandler.readFileToSB(pfspPath).toString();
		CompactState wdRec1 = comp.compile(processName, fspStr);
		logger.debug(fspStr);
		LabeledPCA afterLpca = new LabeledPCA(wdRec1, varLabels);
		afterLpca.setVariables(vars);

		// pass the afterLpca about the variables inherited from the original vpca
		logger.debug(afterLpca.getLTS());
		return afterLpca;
	}

	/**
	 * Create local variables for the local recovered model
	 * 
	 * @return
	 */
	private Set<VariableType> createVariables() {
		Set<VariableType> vars = new HashSet<>();
		VariableType<Integer> va1 = new VariableType<>();
		va1.setNamespace("WD");
		va1.setVarName("owed");
		Map<Integer, Double> pd = new TreeMap<>();
		pd.put(0, 1.0);
		pd.put(1, 0.0);
		va1.setProbDist(pd);
		va1.setKind(VariableType.LOCAL_KIND);
		vars.add(va1);

		VariableType<Integer> va2 = new VariableType<>();
		va2.setNamespace("WD");
		va2.setVarName("stock");
		Map<Integer, Double> pd2 = new TreeMap<>();
		pd2.put(0, 0.2);
		pd2.put(1, 0.8);
		va2.setProbDist(pd2);
		va2.setKind(VariableType.LOCAL_KIND);
		vars.add(va2);
		return vars;
	}

	/**
	 * Will be replaced by automation
	 * 
	 * @return
	 */
	private Map<Integer, Map<String, Object>> getStateLableForRec() {
		Map<Integer, Map<String, Object>> labelMap = new HashMap<>();
		Map<String, Object> varObj = new HashMap<>();
		labelMap.put(0, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(2, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(12, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(-1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(4, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(5, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(6, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(7, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(8, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(9, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(10, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(11, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(13, varObj);

		return labelMap;
	}

}
