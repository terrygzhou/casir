package test.edu.rmit.casir.casestudy.fm;

import java.io.IOException;
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
import lts.CompactState;
import lts.Transition;

/**
 * This the client for the testing
 * 
 * @author terryzhou
 *
 */
public class FMComplex {
	static Logger logger = Logger.getLogger(FMComplex.class);
	CompositeVPCA comVpca;
	Map<String, String> abstractPCA;
	Hashtable<String, VPCA> epcaTable;
	Vector<VPCA> epcas = new Vector<>();
	String workDir = "./casestudy/thesis/furniture_maker/";

	// to be specified
	String currentDir = workDir + "scenario_2/";
	// String currentDir = workDir + "scenario_3/";
	// String name = "fm_complex__reliablewood_err_free_2";
	// String name = "fm_complex__reliablewood_err_free";
	// String name = "fm_complex__reliablewood_with_err";
	String name = "fm_complex__reliablewood_with_err";
	String epcaFilePath = currentDir + name + ".epca";

	/**
	 * output files below
	 */
	String outputDir = currentDir + "output/";
	// String compositeDTMCPath = outputDir + name + "_rec1_dtmc.pm";
	// String compositeMRMPath = outputDir + name + "_rec1_";
	// String composteDotPath = outputDir + name + "_rec1.dot";
	String dtmcFilePath = outputDir + name + ".pm";

	String counterexampleFile = "./casestudy/thesis/furniture_maker/scenario_2/output/counter_example.path";
	String stateMapFile = "./casestudy/thesis/furniture_maker/scenario_2/output/fm_complex__reliablewood_with_err.sta";

	Vector<Ken> kens = new Vector<>();
	Configuration conf;
	CompositeLPCA gloModel;
	Framework framework = new Framework();

	TestBed tb;

	@Before
	public void setUp() throws Exception {
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
		Vector<LabeledPCA> localLPCA = new Vector<>();

		// reading local models and kens
		for (String name : epcaTable.keySet()) {
			if (name.equals("WD"))
				logger.debug("debug");
			VPCA vpca = epcaTable.get(name);
			logger.info(vpca.unfoldPfsp());
			KenBasic ken = new KenBasic(name);
			ken.setVpca(vpca);
			kenSet.add(ken);
			this.kens.add(ken);
			Vector<LabeledPCA> localLPCAVec = new Vector<>();
			LabeledPCA locallpca = vpca.getUnfoldedLabeledPCA();
			locallpca.print();
			locallpca.outputDotFigure(outputDir);
			// localLPCAVec.add(locallpca);
			// CompositeLPCA complpca = new CompositeLPCA(localLPCAVec);
			ken.setLpca(locallpca);
			localLPCA.add(locallpca);
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
		this.gloModel = new CompositeLPCA(localLPCA);

		// Setup Configuration
		Map<String, Gate> gates = this.createGates();
		this.createConfiguration(gates);
		conf.setGloModel(this.gloModel);
		this.conf.print();
		this.framework.setConfig(this.conf);

		// set framework
		CounterexampleComicsImpl counterExam = new CounterexampleComicsImpl(this.counterexampleFile,
				this.stateMapFile);
		this.framework.setCounterexample(counterExam);

	}

	// @Test
	public void testComposition() {
		// this.comVpca = new CompositeVPCA(null, this.epcas);
		// logger.info(gloModel.getGlobalPCA().getStateIDMap());
		try {
			// logger.info(gloModel.getGraphvizDot());
			gloModel.getGloLPCA().outputDotFigure(this.outputDir);
			gloModel.outputDTMCFile(this.dtmcFilePath);
			// gloModel.outputDot(outputDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testCounterexample() {
		CounterexampleComicsImpl counterExam = new CounterexampleComicsImpl(this.counterexampleFile,
				this.stateMapFile);
		Vector<Transition> tranPath = framework.restoreSETransition(counterExam.getSePath(),
				gloModel);
		tranPath.forEach(t -> {
			logger.info(t.getEventPCA().getLabel());
		});
	}

	@Test
	public void testGloRecovery() {

		// select a gloState based on the recovery type
		int gloState = 99;

		// Given a global state in the strongest evidence, localising the
		// inconsistencies in terms of <Ken, localStateID>
		Map<String, Integer> inconsistency = framework.localiseInconsistency(conf, "prop1",
				gloState);
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

		for (int i = 0; i < 99; i++) {
			String outputData = "./casestudy/epca/output/" + name + "_rec1_output_" + i + "_.txt";
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

	private Map<String, Gate> createGates() {

		Map<String, GateR> rGateMap = new HashMap<>();
		Map<String, GateP> pGateMap = new HashMap<>();

		GateP p, p1, p2;
		GateR r, r1, r2;

		for (Ken k : this.kens) {
			String name = k.getName();
			switch (name) {
			case "FM_ORDER":
				p = new GateP("DesignFuniture_P");
				pGateMap.put("DesignFuniture_P", p);
				p.setKen(k);
				k.addPGate(p);

				r = new GateR("MakeFurniture_R");
				rGateMap.put("MakeFurniture_R", r);
				r.setKen(k);
				k.addRGate(r);
				break;

			case "CUSTOMER":
				r = new GateR("DesignFuniture_R");
				rGateMap.put("DesignFuniture_R", r);
				r.setKen(k);
				k.addRGate(r);
				break;

			case "FM":
				p = new GateP("MakeFurniture_P");
				pGateMap.put("MakeFurniture_P", p);
				p.setKen(k);
				k.addPGate(p);

				r1 = new GateR("OrderSteel_R");
				rGateMap.put("OrderSteel_R", r1);
				r1.setKen(k);
				k.addRGate(r1);

				r2 = new GateR("OrderWood_R");
				rGateMap.put("OrderWood_R", r2);
				r2.setKen(k);
				k.addRGate(r2);
				break;

			case "WD":
				p1 = new GateP("OrderWood_P");
				pGateMap.put("OrderWood_P", p1);
				p1.setKen(k);
				k.addPGate(p1);

				p2 = new GateP("Resupply_P");
				pGateMap.put("Resupply_P", p2);
				p2.setKen(k);
				k.addPGate(p2);
				break;

			case "SD":
				p = new GateP("OrderSteel_P");
				pGateMap.put("OrderSteel_P", p);
				p.setKen(k);
				k.addPGate(p);
				break;

			case "LM":
				r = new GateR("Resupply_R");
				rGateMap.put("Resupply_R", r);
				r.setKen(k);
				k.addRGate(r);
				break;

			default:
				logger.info("Not captured???");
				break;
			}
		}

		Map<String, Gate> gates = new HashMap<>();
		gates.putAll(pGateMap);
		gates.putAll(rGateMap);
		return gates;
	}

	private Configuration createConfiguration(Map<String, Gate> gates) {
		Set<Binding> bindings = new HashSet<>();
		Gate s = gates.get("DesignFuniture_R");
		Gate t = gates.get("DesignFuniture_P");

		Binding b = new Binding("DesignFuniture", s, t);
		bindings.add(b);
		b = new Binding("MakeFurniture", gates.get("MakeFurniture_R"),
				gates.get("MakeFurniture_P"));
		bindings.add(b);
		b = new Binding("OrderSteel", gates.get("OrderSteel_R"), gates.get("OrderSteel_P"));
		bindings.add(b);
		b = new Binding("OrderWood", gates.get("OrderWood_R"), gates.get("OrderWood_P"));
		bindings.add(b);
		b = new Binding("Resupply", gates.get("Resupply_R"), gates.get("Resupply_P"));
		bindings.add(b);
		this.conf = new Configuration(this.kens, bindings);
		return this.conf;
	}

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

	private Vector<LabeledPCA> getCandidateLpca() {
		String rec1Path = "./casestudy/epca/" + name + "_rec.pca";
		Set<VariableType> variables = this.createVariables();
		Vector<LabeledPCA> candLPCAVec = new Vector<>();
		LabeledPCA transformedLpca = null;
		try {
			transformedLpca = this.manualTransformLPCA(rec1Path, "WD_REC",
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

	private Set<VariableType> createVariables() {
		Set<VariableType> vars = new HashSet<>();
		VariableType<Integer> va1 = new VariableType<>();
		va1.setNamespace("WD");
		va1.setVarName("owned");
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
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(2, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(12, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owned", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(-1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(4, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(5, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(6, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(7, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(8, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(9, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(10, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owned", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(11, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owned", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(13, varObj);

		return labelMap;
	}

}
