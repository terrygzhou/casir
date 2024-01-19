package test.edu.rmit.casir.casestudy;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import api.PFSPCompiler;
import edu.rmit.casir.architecture.Binding;
import edu.rmit.casir.architecture.Configuration;
import edu.rmit.casir.architecture.Framework;
import edu.rmit.casir.architecture.Gate;
import edu.rmit.casir.architecture.GateP;
import edu.rmit.casir.architecture.GateR;
import edu.rmit.casir.architecture.Ken;
import edu.rmit.casir.architecture.KenBasic;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.pca.CompositePCA;
import edu.rmit.casir.util.FileHandler;
import lts.CompactState;
import test.edu.rmit.casir.vpca.CompositeVPCATest;

public class FMRecovery5 {
	static Logger logger = Logger.getLogger(FMRecovery5.class);
	CompositeVPCA comVpca;
	Map<String, String> abstractPCA;
	Hashtable<String, VPCA> epcaTable;
	Vector<VPCA> epcas = new Vector<>();
	String name = "furniture_simple";
	Vector<Ken> kens = new Vector<>();
	Configuration conf;
	Framework framework;

	String epcaFilePath = "./casestudy/epca/" + name + ".epca";
	String unfoldedPcaPath = "./casestudy/epca/output/" + name + ".pca";
	String absPcaPath = "./casestudy/epca/output/" + name + "_abs.pca";
	String compositeDTMCPath = "./casestudy/epca/output/" + name + ".pm";
	String composteDotPath = "./casestudy/epca/output/" + name + ".dot";
	String rec5Path = "./casestudy/epca/" + name + "_rec5.pca";
	String efmPath = "./casestudy/epca/" + name + "_efm.pca";
	String rec5RawData = "./casestudy/epca/output/" + name + "_rec5_raw.txt";
	String rec5BigRawData = "./casestudy/epca/output/" + name + "_rec5_raw_big.txt";

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
		Set<Binding> bindings = new HashSet<>();

		epcaTable.forEach((k, epca) -> {
			KenBasic component = new KenBasic(k);
			component.setVpca(epca);
			kenSet.add(component);
			this.kens.add(component);
			Vector<LabeledPCA> localLPCAVec = new Vector<>();
			localLPCAVec.add(epca.getUnfoldedLabeledPCA());
			component.setLpca(new CompositeLPCA(localLPCAVec).getGloLPCA());
		});

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

		Binding b = new Binding("order_wood", gateMap.get("RGate_FM"), gateMap.get("PGate_WD"));
		bindings.add(b);

		this.conf = new Configuration(kenSet, bindings);
		this.framework = new Framework();
		this.abstractPCA = vPCACompiler.getTemplatePCAMap();
		// this.unfoldedPCA();
	}

	@Test
	public void testLocalRecovery() {
		CompositePCA global_rec1_com = this.getTransformedModel();
		logger.info(global_rec1_com.getStateIDMap());
		logger.info(global_rec1_com.getGlobalLTS());
		Set<Integer> handlerCompletionLocStates = new HashSet<>();

		handlerCompletionLocStates.add(-1); // ERROR
		handlerCompletionLocStates.add(8); // confirm_amount_1
		handlerCompletionLocStates.add(9); // nowood

		Set<Integer> icStates = new HashSet<Integer>();
		icStates.add(12);

		Set<String> startActions = new HashSet<>();
		startActions.add("avai");
		startActions.add("na");

		global_rec1_com.virtualRun(10000, this.createSample(), this.createTimeCostMap(),
				this.createDollarCostMap(), this.rec5RawData, startActions,
				handlerCompletionLocStates, icStates);

		global_rec1_com.virtualRun(100000, this.createSample(), this.createTimeCostMap(),
				this.createDollarCostMap(), this.rec5BigRawData, startActions,
				handlerCompletionLocStates, icStates);

	}

	/**
	 * ************************Sample creation************************************
	 */

	private CompositePCA getTransformedModel() {
		String fspStr = null;
		String efmFSPStr = null;
		try {
			fspStr = FileHandler.readFileToSB(this.rec5Path).toString();
			efmFSPStr = FileHandler.readFileToSB(this.efmPath).toString();
			logger.info(fspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		PFSPCompiler comp = new PFSPCompiler();
		CompactState wdRec4 = comp.compile("EWD_REACT", fspStr);
		APITest.printPCA(wdRec4);
		CompactState efm = comp.compile("EFM", efmFSPStr);
		APITest.printPCA(efm);

		CompactStateRef cs1 = new CompactStateRef(wdRec4);
		logger.info(cs1.getLTS());
		LabeledPCA lp1 = new LabeledPCA(wdRec4, null);

		CompactStateRef cs2 = new CompactStateRef(efm);
		logger.info(cs2.getLTS());

		Vector machines = new Vector(2);
		machines.add(wdRec4);
		machines.add(efm);
		CompactState globalRec1 = comp.compose("GLOBAL_REC1", machines);

		CompactStateRef global_rec1 = new CompactStateRef(globalRec1);
		logger.info(global_rec1.getLTS());

		Vector<CompactStateRef> pcaVec = new Vector<>();
		pcaVec.add(cs1);
		pcaVec.add(cs2);
		CompositePCA globalTransform = new CompositePCA(global_rec1, pcaVec);

		return globalTransform;
	}

	private Map<Integer, Map<Integer, Vector<String>>> createSample() {
		Map<Integer, Map<Integer, Vector<String>>> sample = new HashMap<>();
		// 1:EFM, 0:WD
		Map<Integer, Vector<String>> localWDLabels = createWDLabels();
		Map<Integer, Vector<String>> localFMLabels = createFMLabels();
		sample.put(0, localWDLabels);
		sample.put(1, localFMLabels);

		return sample;
	}

	private Map<String, Double> createTimeCostMap() {
		Map<String, Double> costMap = new HashMap<>();
		costMap.put("nowood", 1.1);
		costMap.put("confirm_amount_1", 3.1);
		costMap.put("wood_check", 0.2);
		costMap.put("orderWood__amount_1", 3.1);
		costMap.put("order_ack", 1.0);

		return costMap;
	}

	private Map<String, Double> createDollarCostMap() {
		Map<String, Double> costMap = new HashMap<>();
		costMap.put("nowood", 6.5);
		costMap.put("confirm_amount_1", 140.0);
		costMap.put("orderWood__amount_1", 90.0);
		costMap.put("order_ack", 10.0);
		return costMap;
	}

	private Map<Integer, Vector<String>> createWDLabels() {
		Map<Integer, Vector<String>> sample = new HashMap<>();
		Vector<String> localVC = new Vector<>();
		sample.put(0, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=0");
		localVC.add("wd_stock=1");
		sample.put(1, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=0");
		localVC.add("wd_stock=0");
		sample.put(2, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=0");
		localVC.add("wd_stock=0");
		sample.put(3, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=1");
		localVC.add("wd_stock=0");
		sample.put(4, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=1");
		localVC.add("wd_stock=0");
		sample.put(5, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=1");
		localVC.add("wd_stock=0");
		sample.put(12, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=1");
		localVC.add("wd_stock=0");
		sample.put(6, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=1");
		localVC.add("wd_stock=0");
		sample.put(7, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=0");
		localVC.add("wd_stock=0");
		sample.put(8, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=0");
		localVC.add("wd_stock=0");
		sample.put(10, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=1");
		localVC.add("wd_stock=0");
		sample.put(9, localVC);

		localVC = new Vector<>();
		localVC.add("END");
		sample.put(13, localVC);

		localVC = new Vector<>();
		localVC.add("wd_owed=1");
		localVC.add("wd_stock=0");
		sample.put(11, localVC);

		localVC = new Vector<>();
		localVC.add("ERROR");
		sample.put(-1, localVC);

		return sample;
	}

	private Map<Integer, Vector<String>> createFMLabels() {
		return null;
	}
}
