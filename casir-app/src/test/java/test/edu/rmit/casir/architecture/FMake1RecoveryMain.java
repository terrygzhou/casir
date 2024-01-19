package test.edu.rmit.casir.architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
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

public class FMake1RecoveryMain {

	static Logger logger = Logger.getLogger(FMake1RecoveryMain.class);
	CompositeVPCA comVpca;
	Map<String, String> abstractPCA;
	Hashtable<String, VPCA> epcaTable;
	Vector<VPCA> epcas = new Vector<>();
	Vector<Ken> kens = new Vector<>();
	Configuration conf;
	Map<String, Map<String, Double>> costMap;
	Framework framework;
	TestBed tb;
	// static Map<String, Object> cached;
	// folders configuration
	// String name = "furniture_simple";
	// String name = "furniture_simple_err";
	String name = "furniture_maker1";

	String rootDir = "./casestudy/thesis/";
	String workDir = rootDir + name + "/";
	String beforeRecDir = workDir + "BeforeRecovery/";
	String beforeRecDotDir = beforeRecDir + "dot/";
	String beforeRecDtmcDir = beforeRecDir + "dtmc/";

	/**
	 * Input file
	 */
	String epcaFilePath = workDir + name + ".epca";

	/**
	 * output files below
	 */
	String counterexampleFile = beforeRecDir + "counter_example.path";
	String stateMapFile = beforeRecDir + name + ".sta";

	/**
	 * Folders for recoveries
	 */
	Vector<String> recDirVec = null;
	Vector<String> recOutputDirVec = null;
	Vector<String> recOutputDotDirVec = null;
	Vector<String> recOutputDtmcDirVec = null;
	Vector<String> recOutputSimDirVec = null;

	/**
	 * Actions for evaluation
	 */
	Set<String> startActs = new HashSet<>();
	Set<String> termActs = new HashSet<>();

	public FMake1RecoveryMain() {
		try {
			this.init();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {

		FMake1RecoveryMain fm = new FMake1RecoveryMain();

		while (true) {
			System.out.println(
					"Please input 'recovery,globalState' where the recovery 1 to 4 are proactive "
							+ "and Rec5 is the reactive one using the the same handler as the Rec1's. \n"
							+ "globalState is the state from which the recovery starts");

			Scanner userInput = new Scanner(System.in);

			while (!userInput.hasNext())
				;

			String input = "";
			if (userInput.hasNext())
				input = userInput.nextLine();

			// String recNum = input.substring(0, input.indexOf(","));
			// String gloStateStr = input.substring(input.indexOf(",") + 1);

			// System.out.println("The selected recovery is 'Rec" + recNum
			// + "', and the global state for the recover is " + gloStateStr);

			// int gloState = Integer.parseInt(gloStateStr.trim());

			if (!input.equals("end")) {

				String recNum = input.substring(0, input.indexOf(","));
				String gloStateStr = input.substring(input.indexOf(",") + 1);
				int gloState = Integer.parseInt(gloStateStr.trim());

				System.out.println("The selected recovery is 'Rec" + recNum
						+ "', and the global state for the recover is " + gloStateStr);

				// main code
				int recIndex = Integer.parseInt(recNum.trim());
				// if (recIndex != 5)
				// gloState = 2;
				// else
				// gloState = 9;
				fm.recovery(recIndex, fm.recOutputSimDirVec.get(recIndex - 1), gloState);
			} else {
				userInput.close();
				System.exit(0);
			}
		} // while true

		// int gloState = 2;
		// // pro-active
		// fm.recovery(1, fm.recOutputSimDirVec.get(0), gloState);
		// fm.recovery(2, fm.recOutputSimDirVec.get(1), gloState);
		// fm.recovery(3, fm.recOutputSimDirVec.get(2), gloState);
		// fm.recovery(4, fm.recOutputSimDirVec.get(3), gloState);
		//
		// // reactive
		// gloState = 9;
		// fm.recovery(5, fm.recOutputSimDirVec.get(4), gloState);

	}

	public void init() throws Exception {

		// FMRecoveryMain.cached = new HashMap<>();

		this.recDirVec = new Vector<>();
		this.recOutputDirVec = new Vector<>();
		this.recOutputDotDirVec = new Vector<>();
		this.recOutputDtmcDirVec = new Vector<>();
		this.recOutputSimDirVec = new Vector<>();

		logger.info("----------------------------------------------------------");

		logger.info("Creating the output folders.....");
		for (int i = 0; i < 5; i++) {
			recDirVec.add(this.workDir + "Rec" + (i + 1) + "/");
			recOutputDirVec.add(recDirVec.get(i) + "output/");
			recOutputDotDirVec.add(recOutputDirVec.get(i) + "dot/");
			recOutputDtmcDirVec.add(recOutputDirVec.get(i) + "dtmc/");
			recOutputSimDirVec.add(recOutputDirVec.get(i) + "simulation/");
		}

		try {
			// create dir for the original model
			Files.createDirectories(Paths.get(this.workDir));
			Files.createDirectories(Paths.get(this.beforeRecDir + "UnfoledPFSP/"));
			Files.createDirectories(Paths.get(this.beforeRecDir + "AbstractPFSP/"));
			Files.createDirectories(Paths.get(this.beforeRecDtmcDir));
			Files.createDirectories(Paths.get(this.beforeRecDotDir));

			logger.info("creating " + this.workDir);
			logger.info("creating " + this.beforeRecDir + "UnfoledPFSP/");
			logger.info("creating " + this.beforeRecDtmcDir);
			logger.info("creating " + this.beforeRecDotDir);
			logger.info("----------------------------------------------------------");

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		VPCACompiler vPCACompiler;
		String epfspStr = null;
		try {
			logger.info("Reading the given vPCA from " + epcaFilePath);
			epfspStr = FileHandler.readFileToSB(epcaFilePath).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Compile the vPCA.....");
		// Thread.sleep(1000);
		vPCACompiler = new VPCACompiler(epfspStr);
		epcaTable = vPCACompiler.getVpcaTable();
		Set<Ken> kenSet = new HashSet<>();
		Vector<LabeledPCA> localLPCA = new Vector<>();
		logger.info("Parsing " + epcaTable.size() + " components: ");
		for (String k : epcaTable.keySet()) {
			KenBasic ken = new KenBasic(k);
			VPCA vpca = epcaTable.get(k);
			logger.info("\t parsing component " + k);
			// for debugging...
			if (k.equals("WD"))
				logger.debug("debugging");

			vpca.outputAbstractFSP(beforeRecDir + "AbstractPFSP/" + k + "_abstract.pca");
			// Thread.sleep(500);
			logger.info("Extract " + k + "'s abstract PCA into " + this.beforeRecDir
					+ "AbstractPFSP/" + k + "_abstract.pca");

			vpca.outputUnfoldedFSP(beforeRecDir + "UnfoledPFSP/" + k + "_unfolded.pca");
			// Thread.sleep(500);
			logger.info("Unfolding" + k + "'s local vPCA into " + this.beforeRecDir + "UnfoledPFSP/"
					+ k + "_unfolded.pca");

			ken.setVpca(vpca);
			// set k's variables
			ken.setVariables(vpca.getVariables());
			logger.info("component " + k + " has variables:\n " + "\t" + vpca.getVariables());
			kenSet.add(ken);
			this.kens.add(ken);
			LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();

			// FMRecoveryMain.cached.put(k, lpca);

			// persistene lpca
			// Map<Integer, Set<Transition>> lts=lpca.getLTS();
			// FilePersistence.write(this.rootOutputDir+k+"_lpca.obj", lts);

			lpca.outputDotFigure(beforeRecDotDir + k + "_unfolded.dot");
			// Thread.sleep(1000);
			logger.info("Visualise the LPCA as dot graphs in: " + beforeRecDotDir + k
					+ "_unfolded.dot");
			lpca.setVpca(vpca);
			lpca.setName(vpca.getProcessName());
			ken.setLpca(lpca);
			logger.debug(lpca.getName());
			logger.debug(lpca.getVariables());
			localLPCA.add(lpca);
		}

		// set up global model
		Vector<VPCA> vpcaVec = new Vector<>();
		logger.info("Printing the unfolded PFSPs as follows: \n");

		for (String vpcaName : this.epcaTable.keySet()) {
			VPCA vpca = this.epcaTable.get(vpcaName);
			// Thread.sleep(1000);
			logger.info("\n" + vpca.unfoldPfsp() + "\n");
			vpcaVec.add(vpca);
		}

		logger.info("Parallel compose local LPCA....\n");
		// Thread.sleep(1000);
		CompositeVPCA compositeVpca = new CompositeVPCA("ALL", vpcaVec);
		this.comVpca = compositeVpca;
		CompositeLPCA gloModel = new CompositeLPCA(localLPCA);
		gloModel.outputDotFile(this.beforeRecDotDir + this.name + ".dot");
		logger.info("Visualise the global LPCA in " + this.beforeRecDotDir + this.name + ".dot");

		gloModel.outputDTMCFile(this.beforeRecDtmcDir + this.name + ".pm");

		logger.info("----------------------------------------------------------");
		// Thread.sleep(1000);

		logger.info("Intepret the global model into the DTMC in " + this.beforeRecDtmcDir
				+ this.name + ".pm");
		// Thread.sleep(1000);

		// FMRecoveryMain.cached.put("composite", gloModel);
		logger.info("----------------------------------------------------------");
		logger.info("Set up the architecture configuration.");

		// set Kens and connections
		Map<String, Gate> gateMap = this.setGates(kenSet);

		// set AC
		this.conf = new Configuration(kenSet, this.setBindings(gateMap));
		this.conf.setGloModel(gloModel);
		logger.debug(this.conf.getGloModel().getGlobalPCA().getGlobalLTS());

		PctlProperty pro = new PctlProperty("IC1",
				"S=? [wd_over> 0 & fm_steel=0 & fm_wood=0 & ! \"error\"]",
				this.createPropVariables());
		pro.setProperty("reliability", "S=? [!\"error\"]");
		conf.setPctlProperty(pro);
		// Thread.sleep(1000);
		logger.info("----------------------------------------------------------");
		logger.info("Set the specification as in specs.pctl");

		// Thread.sleep(1000);
		logger.info("----------------------------------------------------------");
		logger.info("Specify the cost specfication by reading: ");
		// set costs
		Map<String, Map<String, Double>> costMap = this.getCost(this.workDir + "time_cost.txt",
				"time");
		this.costMap = new HashMap<>();
		this.costMap.putAll(costMap);
		conf.getCostMap().putAll(costMap);

		costMap = this.getCost(this.workDir + "dollar_cost.txt", "dollar");
		this.costMap.putAll(costMap);
		conf.getCostMap().putAll(costMap);
		logger.info("\t Time cost from " + this.workDir + "time_cost.txt");
		logger.info("\t Dollar cost from " + this.workDir + "dollar_cost.txt");

		gloModel.outputMRMFile(beforeRecDir + "dtmc/" + name, this.costMap);

		logger.info("Output the Markov Reward Model in " + this.beforeRecDir + "dtmc/");
		logger.info("----------------------------------------------------------\n\n");

		// set the framework by providing the output files
		// conf.print();
	}

	/**
	 * Parameterized recovery
	 * 
	 * @param index
	 * @param outputSimDir
	 * @param gloState
	 * @throws InterruptedException
	 */
	public void recovery(int index, String outputSimDir, int gloState) throws InterruptedException {

		logger.info("\n");
		logger.info("----------------------------------------------------------");
		logger.info("Ok, let us start the recovery from global state " + gloState);

		this.createFoldersForRec(index);

		/**
		 * To be replaced by automation
		 */
		// String recPath = this.recDirVec.get(index - 1) + name + "_rec" + index +
		// ".pca";

		this.framework = new Framework(
				this.recOutputDtmcDirVec.get(index - 1) + this.name + "_rec" + index + ".pm",
				this.recOutputDtmcDirVec.get(index - 1) + this.name + "_rec" + index,
				this.recOutputDotDirVec.get(index - 1) + this.name + "_rec" + index + ".dot");
		/**
		 * using COMICS to generate the counterexample and set it into the framework
		 */
		CounterexampleComicsImpl counterExam = new CounterexampleComicsImpl(this.counterexampleFile,
				this.stateMapFile);
		this.framework.setCounterexample(counterExam);
		logger.info("\n");
		logger.info("----------------------------------------------------------");

		logger.info("scan the following strongest evidence from " + this.counterexampleFile);

		// Thread.sleep(1000);

		logger.info(
				"Reflect the counterexample (unfolded) states to the global PCA stattes by cross-checking PRISM states model "
						+ this.stateMapFile);
		logger.info(counterExam.getSePath());

		// Thread.sleep(1000);

		/**
		 * Some manual work here to generate counterexample and identify the global
		 * state 2
		 */
		Map<String, Integer> inconsistency = framework.localiseInconsistency(conf, "IC1", gloState);

		inconsistency.forEach((a, b) -> {
			logger.info("localised component is " + a);
			logger.info("localised local state is " + b);
		});

		Ken connector = null;
		int locState = 0;

		Map<String, LabeledPCA> recLPCAMap = new HashMap<>();

		// For each local Ken, generate all recovered configurations
		for (String kenName : inconsistency.keySet()) {
			// if (!kenName.equals("WD"))
			// continue;
			logger.debug("Current KEN " + kenName);
			String recPath = this.recDirVec.get(index - 1) + kenName + "_rec" + index + ".pca";

			connector = this.conf.getKenByName(kenName);
			locState = inconsistency.get(kenName);
			logger.info("\n");
			logger.info("----------------------------------------------------------");

			logger.info("localised connector " + kenName + " where local state " + locState
					+ " is ready for the local recovery");

			logger.info("Recovery reasonsing in connector " + kenName + " from state " + locState
					+ " ......");

			Vector<LabeledPCA> candidateLPCAVec = this.getCandidateLpca(recPath,
					kenName + "_REC" + index);

			recLPCAMap.put(kenName, candidateLPCAVec.get(0));

			/**
			 * manually set the transformed Recoveries
			 */
			for (LabeledPCA transformedLpca : candidateLPCAVec) {
				logger.debug(kenName);
				logger.debug(transformedLpca.getVariables());
				logger.debug(transformedLpca.getLTS());
				framework.setRecLocLpca(kenName, transformedLpca);
				conf.setCostMap(this.costMap);
				// Configuration afterConf =
				framework.recovery(conf, connector, locState);
				logger.info("The architecture is reconfigured ");
			} // for each candidate recovery for the Ken
		} // for each Ken

		/**
		 * User select to combine the recoveries.
		 */
		logger.info(recLPCAMap);
		Set<String> recKenNames = new HashSet<>();
		Vector<LabeledPCA> localLPCA = new Vector<>();
		for (String recLpcaName : recLPCAMap.keySet()) {
			recKenNames.add(recLpcaName);
			localLPCA.add(recLPCAMap.get(recLpcaName));
		}

		Set<Ken> kens = conf.getKens();
		for (Ken k : kens) {
			String currentKen = k.getName();
			boolean selected = false;
			for (String selectedKen : recLPCAMap.keySet()) {
				if (selectedKen.startsWith(currentKen)) {
					selected = true;
					break;
				}
			}
			if (!selected)
				localLPCA.add(k.getLpca());
		}

		CompositeLPCA transformGloModel = new CompositeLPCA(localLPCA);
		// transformGloModel.outputDotFile(dotPath);
		try {
			transformGloModel.outputDTMCFile(this.recOutputDtmcDirVec.get(index - 1) + this.name
					+ "_combine_rec" + index + ".pm");
			Map<String, Map<String, Double>> map = conf.getCostMap();
			transformGloModel.outputMRMFile(	this.recOutputDtmcDirVec.get(index - 1) + this.name + "_rec" + index, map);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Evaluate the transformed configuration
	 * 
	 * @param afterConf
	 */
	private void evaluate(Configuration afterConf, int bound, String outputSimDir,
			Set<String> startActions, Set<String> completeActions) {
		// Vector<String> outputDataPathVec = new Vector<>();
		Map<String, String> outputDataPathMap = new HashMap<>();

		Map<String, Map<String, Double>> oneCost = afterConf.getCostMap();

		// int i = 0;
		// Map<String, Map<String, Double>> oneCost = afterConf.getCostMap();

		for (String resource : oneCost.keySet()) { // for each kind of resource

			String outputData = outputSimDir + name + "_rec_output_" + resource + ".txt";
			// outputDataPathVec.add(outputData);
			outputDataPathMap.put(resource, outputData);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			logger.info("\n");
			logger.info("----------------------------------------------------------");

			logger.info("Run the instances for resource " + resource + " evaluation");
			Map<String, Double> cost = oneCost.get(resource);

			// this.tb = new TestBed(afterConf, outputDataPathVec.get(i), startActions,
			// completeActions, cost);

			this.tb = new TestBed(afterConf, outputDataPathMap.get(resource), startActions,
					completeActions, cost);

			// i++;
			tb.evaluate(afterConf, bound);
		}
	}

	/**
	 * Create cost map by reading cost specs file
	 * 
	 * @param costFilePath
	 * @param resource
	 * @throws IOException
	 */
	private Map<String, Map<String, Double>> getCost(String costFilePath, String resource)
			throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(costFilePath));
		String line = br.readLine();
		String tranLabel = null;
		Map<String, Double> costmap = new HashMap<>();

		while (line != null) {
			if (line.trim().length() == 0) {
				line = br.readLine();
				continue;
			}
			int index = line.indexOf(":");
			tranLabel = line.substring(0, index);
			String costStr = line.substring(index + 1).trim();
			double cost = Double.parseDouble(costStr.trim());
			costmap.put(tranLabel, cost);
			line = br.readLine();
		}
		Map<String, Map<String, Double>> resultMap = new HashMap<>();
		resultMap.put(resource, costmap);
		return resultMap;
	}

	/**
	 * manually provide the recovered local model as LPCA
	 * 
	 * @return
	 */
	private Vector<LabeledPCA> getCandidateLpca(String afterLocModelPath, String processName) {
		Set<VariableType> variables = null;
		/**
		 * Manual work here..
		 */
		if (processName.startsWith("FM"))
			variables = this.createFMVariables();
		if (processName.startsWith("WD"))
			variables = this.createWDVariables();

		Vector<LabeledPCA> candLPCAVec = new Vector<>();
		LabeledPCA transformedLpca = null;
		try {
			transformedLpca = this.manualTransformLPCA(afterLocModelPath, processName,
					this.getStateLableForRec(processName), variables);
		} catch (IOException e) {
			e.printStackTrace();
		}
		candLPCAVec.add(transformedLpca);
		return candLPCAVec;
	}

	/**
	 * manually create a transformation model
	 * 
	 * @param pfspPath
	 * @param processName
	 * @param varLabels
	 * @param vars
	 * @return
	 * @throws IOException
	 */
	private LabeledPCA manualTransformLPCA(String pfspPath, String processName,
			Map<Integer, Map<String, Object>> varLabels, Set<VariableType> vars)
			throws IOException {
		PFSPCompiler comp = new PFSPCompiler();
		String fspStr = FileHandler.readFileToSB(pfspPath).toString();
		CompactState recLPCA = comp.compile(processName, fspStr);
		logger.debug(fspStr);
		LabeledPCA afterLpca = new LabeledPCA(recLPCA, varLabels);
		afterLpca.setVariables(vars);

		// pass the afterLpca about the variables inherited from the original vpca
		logger.debug(afterLpca.getLTS());
		return afterLpca;
	}

	/**
	 * set gates
	 * 
	 * @param kenSet
	 * @return
	 */
	private Map<String, Gate> setGates(Set<Ken> kenSet) {
		Map<String, Gate> gateMap = new HashMap<>();
		kenSet.forEach(ken -> {
			String kName = ken.getName();
			switch (kName) {
			case "LM":
				GateR r = new GateR("Resupply_R");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("Resupply_R", r);
				break;

			case "SD":
				GateP p = new GateP("OrderSteel_P");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("OrderSteel_P", p);
				break;

			case "WD":
				p = new GateP("OrderWood_P");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("OrderWood_P", p);

				p = new GateP("Resupply_P");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("Resupply_P", p);
				break;

			case "FM":
				p = new GateP("MakeFurniture_P");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("MakeFurniture_P", p);

				r = new GateR("OrderWood_R");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("OrderWood_R", r);

				r = new GateR("OrderSteel_R");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("OrderSteel_R", r);

				break;

			case "CUSTOMER":
				r = new GateR("DesignFurniture_R");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("DesignFurniture_R", r);
				break;

			case "FM_ORDER":
				r = new GateR("MakeFurniture_R");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("MakeFurniture_R", r);
				p = new GateP("DesignFurniture_P");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("DesignFurniture_P", p);
				break;

			default:
				break;
			}
			// read from persistence, failed due to non-seriziable sub objects
			// Map<Integer, Set<Transition>> lts=(Map<Integer,
			// Set<Transition>>)FilePersistence.read(this.rootOutputDir+ken.getName()+"_lpca.obj");
			// logger.debug(lts);

		});

		return gateMap;
	}

	/**
	 * Set Bindings
	 * 
	 * @param gateMap
	 * @return
	 */
	private Set<Binding> setBindings(Map<String, Gate> gateMap) {
		// set bindings
		Set<Binding> bindings = new HashSet<>();

		Binding b = new Binding("DesignFurniture", gateMap.get("DesignFurniture_R"),
				gateMap.get("DesignFurniture_P"));
		bindings.add(b);

		b = new Binding("MakeFurniture", gateMap.get("MakeFurniture_R"),
				gateMap.get("MakeFurniture_P"));
		bindings.add(b);

		b = new Binding("OrderSteel", gateMap.get("OrderSteel_R"), gateMap.get("OrderSteel_P"));
		bindings.add(b);

		b = new Binding("OrderWood", gateMap.get("OrderWood_R"), gateMap.get("OrderWood_P"));
		bindings.add(b);

		b = new Binding("Resupply", gateMap.get("Resupply_R"), gateMap.get("Resupply_P"));
		bindings.add(b);

		return bindings;
	}

	/**
	 * create formula variables
	 * 
	 * @return
	 */
	private Set<VariableType> createPropVariables() {
		Set<VariableType> vars = new HashSet<>();
		vars.addAll(this.createFMVariables());

		VariableType<Integer> va1 = new VariableType<>();
		va1.setNamespace("WD");
		va1.setVarName("over");
		Map<Integer, Double> pd = new TreeMap<>();
		pd.put(0, 1.0);
		pd.put(1, 0.0);
		va1.setProbDist(pd);
		va1.setKind(VariableType.LOCAL_KIND);
		vars.add(va1);
		vars.add(va1);

		return vars;

	}

	/**
	 * Create local variables for the local recovered model
	 * 
	 * @return
	 */
	private Set<VariableType> createFMVariables() {
		Set<VariableType> vars = new HashSet<>();
		// VariableType<Integer> va1 = new VariableType<>();
		// va1.setNamespace("WD");
		// va1.setVarName("owed");
		// Map<Integer, Double> pd = new TreeMap<>();
		// pd.put(0, 1.0);
		// pd.put(1, 0.0);
		// va1.setProbDist(pd);
		// va1.setKind(VariableType.LOCAL_KIND);
		// vars.add(va1);

		// VariableType<Integer> va2 = new VariableType<>();
		// va2.setNamespace("WD");
		// va2.setVarName("stock");
		// Map<Integer, Double> pd2 = new TreeMap<>();
		// pd2.put(0, 0.2);
		// pd2.put(1, 0.8);
		// va2.setProbDist(pd2);
		// va2.setKind(VariableType.LOCAL_KIND);
		// vars.add(va2);

		VariableType<Integer> va3 = new VariableType<>();
		va3.setNamespace("FM");
		va3.setVarName("wood");
		Map<Integer, Double> pd3 = new TreeMap<>();
		pd3.put(0, 1.0);
		pd3.put(1, 0.0);
		va3.setProbDist(pd3);
		va3.setKind(VariableType.LOCAL_KIND);
		vars.add(va3);

		VariableType<Integer> va4 = new VariableType<>();
		va4.setNamespace("FM");
		va4.setVarName("steel");
		Map<Integer, Double> pd4 = new TreeMap<>();
		pd4.put(0, 1.0);
		pd4.put(1, 0.0);
		va4.setProbDist(pd4);
		va4.setKind(VariableType.LOCAL_KIND);
		vars.add(va4);

		/**
		 * label "error"
		 */

		return vars;
	}

	private Set<VariableType> createWDVariables() {
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
		pd2.put(0, 0.6);
		pd2.put(1, 0.4);
		va2.setProbDist(pd2);
		va2.setKind(VariableType.LOCAL_KIND);
		vars.add(va2);

		VariableType<Integer> va3 = new VariableType<>();
		va3.setNamespace("WD");
		va3.setVarName("over");
		Map<Integer, Double> pd3 = new TreeMap<>();
		pd3.put(0, 1.0);
		pd3.put(1, 0.0);
		va3.setProbDist(pd3);
		va3.setKind(VariableType.LOCAL_KIND);
		vars.add(va3);

		/**
		 * label "error"
		 */

		return vars;
	}

	/**
	 * prepare for the recover ith
	 * 
	 * @param i
	 */
	private void createFoldersForRec(int j) {
		int i = j - 1;
		logger.info("---------------------------------------------------------");
		logger.info("Creating folders for the recovery");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			Files.createDirectories(Paths.get(this.recDirVec.get(i)));
			Files.createDirectories(Paths.get(this.recOutputDirVec.get(i)));
			Files.createDirectories(Paths.get(this.recOutputDotDirVec.get(i)));
			Files.createDirectories(Paths.get(this.recOutputDtmcDirVec.get(i)));
			Files.createDirectories(Paths.get(this.recOutputSimDirVec.get(i)));
			logger.info("Creating folder " + this.recDirVec.get(i));
			logger.info("Creating folder " + this.recOutputDirVec.get(i));
			logger.info("Creating folder " + this.recOutputDotDirVec.get(i));
			logger.info("Creating folder " + this.recOutputDtmcDirVec.get(i));
			logger.info("Creating folder " + this.recOutputSimDirVec.get(i));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Will be replaced by automation
	 * 
	 * @return
	 */
	private Map<Integer, Map<String, Object>> getStateLableForRec(String recProcessName) {

		switch (recProcessName) {

		case "FM_REC1":
			// startActs.add("avail_1");
			// startActs.add("~na_1");
			// termActs.add("confirm_amount_1"); // for rec1
			// termActs.add("nowood"); // for rec1
			// termActs.add("~na_1"); // for rec1
			return this.getStateLableForFMRec1();

		case "WD_REC1":
			// startActs.add("avail_1");
			// startActs.add("~na_1");
			// termActs.add("confirm_amount_1"); // for rec1
			// termActs.add("nowood"); // for rec1
			// termActs.add("~na_1"); // for rec1
			return this.getStateLableForWDRec1();

		case "FM_REC2":
			// startActs.add("avail_2");
			// startActs.add("~na_2");
			// termActs.add("supply_0"); // for rec1
			// termActs.add("supply_1"); // for rec1
			// termActs.add("~na_2"); // for rec1
			return this.getStateLableForRec2();

		case "WD_REC2":
			// startActs.add("avail_1");
			// startActs.add("~na_1");
			// termActs.add("confirm_amount_1"); // for rec1
			// termActs.add("nowood"); // for rec1
			// termActs.add("~na_1"); // for rec1
			return this.getStateLableForWDRec2();

		case "FM_REC3":
			// startActs.add("avail_1");
			// startActs.add("na_1");
			// termActs.add("~request");
			// termActs.add("nowood");
			// termActs.add("confirm_amount_1");
			// termActs.add("~na_2");
			// termActs.add("~login2");
			// termActs.add("wood_supply_1");
			// termActs.add("wood_supply_0");
			return this.getStateLableForRec3();

		case "FM_REC4":
			// startActs.add("avail_1");
			// startActs.add("~na_1");
			// termActs.add("~request");
			// termActs.add("nowood");
			// termActs.add("confirm_amount_1");
			// termActs.add("~na_1");
			// termActs.add("~login2");
			// termActs.add("wood_supply_1");
			// termActs.add("wood_supply_0");
			return this.getStateLableForRec4();

		case "FM_REC5":
			// startActs.add("avai");
			// startActs.add("~na");
			// termActs.add("~na");
			// termActs.add("~request");
			// termActs.add("nowood");
			// termActs.add("confirm_amount_1");
			return this.getStateLableForRec5();

		default:
			return null;
		}
	}

	/**
	 * Set state labels
	 * 
	 * @return
	 */
	private Map<Integer, Map<String, Object>> getStateLableForFMRec1() {
		Map<Integer, Map<String, Object>> labelMap = new HashMap<>();

		Map<String, Object> varObj = new HashMap<>();
		labelMap.put(0, varObj);

		varObj = new HashMap<>();
		labelMap.put(-1, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 0);
		labelMap.put(1, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 0);
		labelMap.put(19, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 0);
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 0);
		labelMap.put(6, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(8, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(23, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(24, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(10, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(11, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 1);
		labelMap.put(16, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 1);
		labelMap.put(18, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 0);
		labelMap.put(2, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 0);
		labelMap.put(20, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 1);
		labelMap.put(4, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 1);
		labelMap.put(21, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 0);
		labelMap.put(5, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 1);
		labelMap.put(7, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 0);
		varObj.put("FM::steel", 1);
		labelMap.put(9, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 1);
		labelMap.put(14, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 1);
		labelMap.put(17, varObj);

		varObj = new HashMap<>();
//		varObj.put("FM::wood", 1);
//		varObj.put("FM::steel", 1);
		labelMap.put(22, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(12, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(13, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(15, varObj);

		varObj = new HashMap<>();
		varObj.put("FM::wood", 1);
		varObj.put("FM::steel", 0);
		labelMap.put(25, varObj);

		return labelMap;
	}

	private Map<Integer, Map<String, Object>> getStateLableForWDRec2() {
		Map<Integer, Map<String, Object>> labelMap = new HashMap<>();

		Map<String, Object> varObj = new HashMap<>();
		labelMap.put(0, varObj);

		varObj = new HashMap<>();
		labelMap.put(-1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(2, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(5, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 1);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 1);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(4, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(7, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(6, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(8, varObj);

		return labelMap;
	}

	private Map<Integer, Map<String, Object>> getStateLableForWDRec1() {
		Map<Integer, Map<String, Object>> labelMap = new HashMap<>();

		Map<String, Object> varObj = new HashMap<>();
		labelMap.put(0, varObj);

		varObj = new HashMap<>();
		labelMap.put(-1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(2, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(5, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 1);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(4, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(6, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(7, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::over", 0);
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(8, varObj);

		return labelMap;
	}

	private Map<Integer, Map<String, Object>> getStateLableForRec2() {
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

	private Map<Integer, Map<String, Object>> getStateLableForRec3() {
		Map<Integer, Map<String, Object>> labelMap = new HashMap<>();
		Map<String, Object> varObj = new HashMap<>();
		labelMap.put(0, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(7, varObj);

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
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
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
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(8, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(9, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(10, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(11, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(12, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(13, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(14, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(15, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(-1, varObj);

		return labelMap;
	}

	private Map<Integer, Map<String, Object>> getStateLableForRec4() {
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
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(4, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(5, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(7, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(8, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(9, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(11, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(13, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(15, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(6, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(-1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(10, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(12, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(14, varObj);

		return labelMap;
	}

	private Map<Integer, Map<String, Object>> getStateLableForRec5() {
		Map<Integer, Map<String, Object>> labelMap = new HashMap<>();
		Map<String, Object> varObj = new HashMap<>();
		labelMap.put(0, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 1);
		labelMap.put(1, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(2, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(3, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(8, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 0);
		varObj.put("WD::stock", 0);
		labelMap.put(10, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(4, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(5, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(12, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(6, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(7, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(9, varObj);

		varObj = new HashMap<>();
		varObj.put("WD::owed", 1);
		varObj.put("WD::stock", 0);
		labelMap.put(11, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(-1, varObj);

		varObj = new HashMap<>();
		// varObj.put("WD::owed", 0);
		// varObj.put("WD::stock", 0);
		labelMap.put(13, varObj);

		return labelMap;
	}

}
