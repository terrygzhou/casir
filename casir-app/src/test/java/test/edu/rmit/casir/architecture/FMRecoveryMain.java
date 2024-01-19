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

public class FMRecoveryMain {

	static Logger logger = Logger.getLogger(FMRecoveryMain.class);
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
	 String name = "furniture_simple";
//	String name = "furniture_simple_err";
//	String name = "furniture_maker1";
	
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

	public FMRecoveryMain() {
		try {
			this.init();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {

		FMRecoveryMain fm = new FMRecoveryMain();

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
		Thread.sleep(1000);
		vPCACompiler = new VPCACompiler(epfspStr);
		epcaTable = vPCACompiler.getVpcaTable();
		Set<Ken> kenSet = new HashSet<>();
		Set<Binding> bindings = new HashSet<>();

		Vector<LabeledPCA> localLPCA = new Vector<>();
		logger.info("Parsing " + epcaTable.size() + " components: ");
		for (String k : epcaTable.keySet()) {
			KenBasic ken = new KenBasic(k);
			VPCA vpca = epcaTable.get(k);
			logger.info("\t parsing component " + k);

			vpca.outputAbstractFSP(beforeRecDir + "AbstractPFSP/" + k + "_abstract.pca");
			Thread.sleep(500);
			logger.info("Extract " + k + "'s abstract PCA into " + this.beforeRecDir
					+ "AbstractPFSP/" + k + "_abstract.pca");

			vpca.outputUnfoldedFSP(beforeRecDir + "UnfoledPFSP/" + k + "_unfolded.pca");
			Thread.sleep(500);
			logger.info("Unfolding" + k + "'s local vPCA into " + this.beforeRecDir + "UnfoledPFSP/"
					+ k + "_unfolded.pca");

			ken.setVpca(vpca);
			// set k's variables
			ken.setVariables(vpca.getVariables());
			logger.info(
					"component " + k + " has variables:\n " + "\t\t\t\t\t" + vpca.getVariables());
			kenSet.add(ken);
			this.kens.add(ken);
			LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();

			// FMRecoveryMain.cached.put(k, lpca);

			// persistene lpca
			// Map<Integer, Set<Transition>> lts=lpca.getLTS();
			// FilePersistence.write(this.rootOutputDir+k+"_lpca.obj", lts);

			lpca.outputDotFigure(beforeRecDotDir + k + "_unfolded.dot");
			Thread.sleep(1000);
			logger.info("Visualise the LPCA as dot graphs in: " + beforeRecDotDir + k
					+ "_unfolded.dot");

			lpca.setVpca(vpca);
			lpca.setName(vpca.getProcessName());
			ken.setLpca(lpca);
			localLPCA.add(lpca);
		}

		// set up global model
		Vector<VPCA> vpcaVec = new Vector<>();
		logger.info("Printing the unfolded PFSPs as follows: \n");

		for (String vpcaName : this.epcaTable.keySet()) {
			VPCA vpca = this.epcaTable.get(vpcaName);
			Thread.sleep(1000);
			logger.info("\n" + vpca.unfoldPfsp() + "\n");
			vpcaVec.add(vpca);
		}

		logger.info("Parallel compose local LPCA....\n");
		Thread.sleep(1000);
		CompositeVPCA compositeVpca = new CompositeVPCA("ALL", vpcaVec);
		this.comVpca = compositeVpca;
		CompositeLPCA gloModel = new CompositeLPCA(localLPCA);
		gloModel.outputDotFile(this.beforeRecDotDir + this.name + ".dot");
		logger.info("Visualise the global LPCA in " + this.beforeRecDotDir + this.name + ".dot");

		gloModel.outputDTMCFile(this.beforeRecDtmcDir + this.name + ".pm");

		logger.info("----------------------------------------------------------");
		Thread.sleep(1000);

		logger.info("Intepret the global model into the DTMC in " + this.beforeRecDtmcDir
				+ this.name + ".pm");
		Thread.sleep(1000);

		// FMRecoveryMain.cached.put("composite", gloModel);
		logger.info("----------------------------------------------------------");

		logger.info("Set up the architecture configuration.");
		// set Kens and connections
		Map<String, Gate> gateMap = new HashMap<>();
		kenSet.forEach(ken -> {
			logger.debug(ken.getName());

			// read from persistence, failed due to non-seriziable sub objects
			// Map<Integer, Set<Transition>> lts=(Map<Integer,
			// Set<Transition>>)FilePersistence.read(this.rootOutputDir+ken.getName()+"_lpca.obj");
			// logger.debug(lts);

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
		Thread.sleep(1000);
		logger.info("----------------------------------------------------------");
		logger.info("Set the specification as :" + "S<0.12 [wd_owed>0]");
		logger.info("Set the reliability as :" + "S>0.99 [!\"error\"]\n");

		Thread.sleep(1000);
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
		String recPath = this.recDirVec.get(index - 1) + name + "_rec" + index + ".pca";

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

		Thread.sleep(1000);

		logger.info(
				"Reflect the counterexample (unfolded) states to the global PCA stattes by cross-checking PRISM states model "
						+ this.stateMapFile);
		logger.info(counterExam.getSePath());
		Thread.sleep(1000);

		/**
		 * Some manual work here to generate counterexample and identify the global
		 * state 2
		 */
		// proactive recovery starting from gloState=2
		Map<String, Integer> inconsistency = framework.localiseInconsistency(conf, "IC1", gloState);

		inconsistency.forEach((a, b) -> {
			logger.info("localised component is " + a);
			logger.info("localised global state is " + b);
		});

		Ken connector = null;
		int locState = 0;

		// For each local Ken, generate all recovered configurations
		for (String kenName : inconsistency.keySet()) {
			connector = this.conf.getKenByName(kenName);
			locState = inconsistency.get(kenName);
			logger.info("\n");
			logger.info("----------------------------------------------------------");

			logger.info("localised connector " + kenName + " where local state " + locState
					+ " is ready for the local recovery");

			Vector<LabeledPCA> candidateLPCAVec = this.getCandidateLpca(recPath, "WD_REC" + index);

			logger.info("Recovery reasonsing in connector " + kenName + " from state " + locState
					+ " ......");

			Thread.sleep(1000);

			logger.info("\n");
			logger.info("----------------------------------------------------------");

			/**
			 * manually set the transformed Recoveries
			 */
			for (LabeledPCA transformedLpca : candidateLPCAVec) {
				framework.setRecLocLpca(kenName, transformedLpca);
				conf.setCostMap(this.costMap);

				Configuration afterConf = framework.recovery(conf, connector, locState);

				logger.info("\n");
				logger.info("----------------------------------------------------------");

				logger.info(
						"Currently the local transformed model is created manually based on available FSPF constructs. In the future, it will be replaced by automation when integrating with the AI planner and supported by PDDL");
				logger.info("The transformed model is loaded from " + recPath);
				Thread.sleep(1000);

				logger.info("The architecture is reconfigured ");
				Thread.sleep(1000);

				if (afterConf == null)
					continue;
				afterConf.setCostMap(this.costMap);

				logger.info("\n");
				logger.info("----------------------------------------------------------");

				logger.info("The transformed model is evaluated by the virtual run in the testbed");
				Thread.sleep(1000);

				/**
				 * Evaluate each reconfigured architecture by simulating the runs
				 */
				this.evaluate(afterConf, 1000, outputSimDir, this.startActs, this.termActs);
			} // for each candidate recovery
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
		Set<VariableType> variables = this.createVariables();
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

		case "WD_REC1":
			startActs.add("avail_1");
			startActs.add("~na_1");
			termActs.add("confirm_amount_1"); // for rec1
			termActs.add("nowood"); // for rec1
			termActs.add("~na_1"); // for rec1
			return this.getStateLableForRec1();

		case "WD_REC2":
			startActs.add("avail_2");
			startActs.add("~na_2");
			termActs.add("supply_0"); // for rec1
			termActs.add("supply_1"); // for rec1
			termActs.add("~na_2"); // for rec1
			return this.getStateLableForRec2();

		case "WD_REC3":
			startActs.add("avail_1");
			startActs.add("na_1");
			termActs.add("~request");
			termActs.add("nowood");
			termActs.add("confirm_amount_1");
			termActs.add("~na_2");
			termActs.add("~login2");
			termActs.add("wood_supply_1");
			termActs.add("wood_supply_0");
			return this.getStateLableForRec3();

		case "WD_REC4":
			startActs.add("avail_1");
			startActs.add("~na_1");
			termActs.add("~request");
			termActs.add("nowood");
			termActs.add("confirm_amount_1");
			termActs.add("~na_1");
			termActs.add("~login2");
			termActs.add("wood_supply_1");
			termActs.add("wood_supply_0");
			return this.getStateLableForRec4();

		case "WD_REC5":
			startActs.add("avai");
			startActs.add("~na");
			termActs.add("~na");
			termActs.add("~request");
			termActs.add("nowood");
			termActs.add("confirm_amount_1");
			return this.getStateLableForRec5();

		default:
			return null;
		}
	}

	private Map<Integer, Map<String, Object>> getStateLableForRec1() {
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
