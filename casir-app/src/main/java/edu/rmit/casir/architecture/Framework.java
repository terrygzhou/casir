package edu.rmit.casir.architecture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.javatuples.Triplet;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.ExtTransition;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.verification.CounterexampleComicsImpl;
import edu.rmit.casir.verification.PctlProperty;
import lts.CompactState;
import lts.Transition;

public class Framework {

	// A set of objective configurations
	Set<Configuration> toBeconfigs;

	CounterexampleComicsImpl counterexample;

	PctlProperty prop;

	// current configuration
	Configuration config;
	Logger logger = Logger.getLogger(Framework.class);

	// to be deleted
	// LabeledPCA recLocLpca;

	Map<String, LabeledPCA> recLocLpcaMap;

	String dtmcPath;
	String mrmDirPath;
	String dotPath;
	String workDir;

	// ************************ Methods ****************************************

	public Framework() {

	}

	public Framework(String dtmcPath, String mrmPath, String dotPath) {
		this.dtmcPath = dtmcPath;
		this.mrmDirPath = mrmPath;
		this.dotPath = dotPath;
	}

	/**
	 * Model checking and localise the inconsistency at the Kens and local states,
	 * based on the given a global state and the configuration which includes the
	 * property,
	 * <p>
	 * The result: <Ken's name/ID, local stateID> and further <localStateID,
	 * extState> can be inferred.
	 * 
	 * @param config
	 */
	public Map<String, Integer> localiseInconsistency(Configuration config, String propertyName,
			int gloState) {

		Vector<Integer> strongEve = this.getCounterexample().getSePath();

		if (strongEve == null || strongEve.size() == 0)
			return null;

		Map<String, Integer> result = new HashMap<>();

		int globalStateID = gloState;

		CompositeLPCA globalLPCA = config.getGloModel();

		// mapping the index of the local components and local states
		Vector<LabeledPCA> localLpcaVec = globalLPCA.getLocalLPCAVec();

		/**
		 * Do I need get the globalStateID to localStateID mapping?
		 */
		Map<Integer, Vector<Integer>> localStateMap = globalLPCA.getGlobalPCA().getStateIDMap();
		Vector<Integer> localStateIDVec = localStateMap.get(globalStateID);

		Set<Ken> relevantKens = config.getFormulaRelevantKens(propertyName); // ?

		Set<String> relevantKenNames = new HashSet<>();
		relevantKens.forEach(k -> {
			relevantKenNames.add(k.getName());
		});

		for (int i = 0; i < localLpcaVec.size(); i++) {
			LabeledPCA lpca = localLpcaVec.get(i);
			int localStateID = localStateIDVec.get(i);
			// only return formula-relevant kens
			if (relevantKenNames.contains(lpca.getName()))
				result.put(lpca.getName(), localStateID);
		}

		return result;
	}

	/**
	 * find an effective state from the strongest evidence for an effective
	 * recovery; This determines reactive or pro-active recoveries
	 * 
	 * @param sePath
	 * @return
	 */
	private int getInitState4Recovery(Vector<Integer> sePath, String recoveryType) {

		Set<Integer> states = this.getRecStates(sePath, recoveryType);
		// state 7 is selected with inconsistnecy

		return 18;
	}

	/**
	 * Based on
	 * 
	 * @param sePath
	 * @return
	 */
	private Set<Integer> getRecStates(Vector<Integer> sePath, String recType) {
		Set<Integer> reactiveStates = new HashSet<>();
		Set<Integer> proactiveStates = new HashSet<>();

		for (int stateid : sePath) {
			if (this.isSatisfied(stateid, this.prop))
				proactiveStates.add(stateid);
			else
				reactiveStates.add(stateid);
		}
		if (recType.equals("proactive"))
			return proactiveStates;
		else
			return reactiveStates;
	}

	/**
	 * Restrictively speaking, PRISM model checking the model by starting from the
	 * new initial states is needed;
	 * 
	 * @param stateId
	 * @param prop
	 * @return
	 */
	private boolean isSatisfied(int stateId, PctlProperty prop) {
		// Map<Integer, Map<String, Object>> stateLables =
		// this.config.getGloModel().getGloLPCA()
		// .getVarLabel();
		// Map<String, Object> content = this.prop.getProperties();

		// for simplicity, just check the state formular over the state

		return true;
	}

	/**
	 * Recover from the locState of a given ken, with maintaining ken's P-Gate
	 * (possibly an R-Gate depending on the variables, decided by what?).
	 * <p>
	 * The framework recoveries on the ken.
	 * 
	 * @param ken
	 * @param locState
	 *            of Ken
	 */
	public Configuration recovery(Configuration config, Ken ken, int locStateID) {

		/**
		 * 1.Get initial ExtState for planning, assuming only one PGate of Ken, and one
		 * client
		 */
		KenBasic basicKen = (KenBasic) ken;
		ExtState initExtState = basicKen.getVpca().getExtStateByStateID(locStateID);

		/**
		 * 2. identify the abstract goal states from Ken's template PCA
		 */
		Vector<Integer> absGoalStateIDVec = this.getAbsGoalStates(initExtState.getAbsStateID(),
				basicKen);
		logger.info(
				"Local state " + locStateID + " corresponding abstract state " + absGoalStateIDVec);

		/**
		 * 3. Construct the goal extStates by Exhausting all VCs, i.e. allAssValues on
		 * the goal abstract state
		 */
		Vector<VariableType> locVars = basicKen.getVpca().getLocVariables();
		VPCA vpca = basicKen.getVpca();
		Vector<ExtState> locGoalExtStateVec = new Vector<>();
		
		for (int goalAbsStateId : absGoalStateIDVec) {
			Vector<ExtState> goalExtStates = this.getCandidateGoalExtStates(goalAbsStateId, locVars,
					basicKen);
			Vector<ExtState> filtedGoalExtState = new Vector<>();
			locGoalExtStateVec.addAll(goalExtStates);
		}
		logger.debug(locGoalExtStateVec);
		logger.info("Reason about the local goal state " + locGoalExtStateVec
				+ " that enriches abstract state " + absGoalStateIDVec + " on connector "
				+ ken.getName());

		/**
		 * 4. For each goal state, transforms a local Ken, and further global
		 * transformed model
		 */
		for (ExtState goalExtState : locGoalExtStateVec) {
			Vector<LabeledPCA> localLPCA = new Vector<>();
			// * compose the global transformed model
			Set<Ken> allKens = config.getKens();
			Configuration afterConfig = new Configuration(config.getKens(), config.getBindings());

			for (Ken k : allKens) {
				LabeledPCA lpca = k.getLpca();
				logger.debug(lpca.getLTS());
				logger.debug(lpca.getVarLabel());
				// add excluded-recover component Ken
				logger.debug(ken.getName());
				if (!k.getName().equals(ken.getName())) {
					localLPCA.add(lpca);
					logger.debug(lpca.getLTS());
				}
			}

			// to be replaced by automatic reasoning
			LabeledPCA transformedLocLpca = this.getRecLocLpca(ken.getName());

			if (transformedLocLpca == null) {
				logger.info("There is NO recovery!");
			} else {
				logger.debug(transformedLocLpca.getLTS());
				logger.debug(transformedLocLpca.getVarLabel());
				localLPCA.add(transformedLocLpca);
			}

			CompositeLPCA transformGloModel = new CompositeLPCA(localLPCA);
			logger.debug(transformGloModel.getGloLPCA().getLTS());
			logger.debug(transformGloModel.getGlobalPCA().getStateIDMap());
			logger.debug(transformGloModel.getGloLPCA().getVarLabel());

			/**
			 * Architecture reconfiguration is to be done!
			 */

			// * Verify the transformed model
			try {
				if (this.isAcceptable(afterConfig)) {
					transformGloModel.outputDotFile(this.dotPath);
					transformGloModel.outputDTMCFile(this.dtmcPath);
					afterConfig.setGloModel(transformGloModel);
					Map<String, Map<String, Double>> map = config.getCostMap();
					afterConfig.setCostMap(map);
					transformGloModel.outputMRMFile(mrmDirPath, map);

					// Thread.sleep(1000);

					logger.info("\n");
					logger.info("----------------------------------------------------------");

					logger.info("output the transformed global model as dot figure into "
							+ this.dotPath);
					logger.info("The interpreted DTMC of the transformed global model is put into "
							+ this.dtmcPath);
					logger.info("the Markov Rewards models are put into " + mrmDirPath);
					return afterConfig;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // for each goal state

		return null;
	}

	/**
	 * Find the abstract goal state(s) from a local state "locStateID" of a local
	 * ken, w.r.t. dependent formula-components
	 * 
	 * @return
	 */
	private Vector<Integer> getAbsGoalStates(int locAbsStateID, KenBasic ken) {
		Vector<Integer> absGoalStateIDVec = new Vector<Integer>();
		Set<GateP> pGates = ken.getpGates();
		GateP p = (GateP) pGates.toArray()[0];

		int absGoalStateID = ken.getAbsGoalState(locAbsStateID, p);
		// could return a set of goal states, set it as 0 for the FM example for the
		// proactive and 2 for the reactive, for the demo purpose...
		if (locAbsStateID == 0)
			absGoalStateID = 0;
		else
			// TBD
			absGoalStateID = 2;
		absGoalStateIDVec.add(absGoalStateID);
		return absGoalStateIDVec;
	}

	/**
	 * Verify the given composite LPCA is consistent with the specification, incl
	 * consistency rate, reliability, or budgets . the parameter are passed by Kens
	 * in the Configuration.
	 * 
	 * @param model
	 * @return
	 * @throws Exception
	 */
	private boolean isAcceptable(Configuration conf) throws Exception {
		CompositeLPCA model = conf.getGloModel();
		// model.outputDTMCFile(dtmcPath);
		Map<String, Map<String, Double>> costMaps = conf.getCostMap();
		// model.outputMRMFile(mrmDirPath, model.getMultiMRM(costMaps));
		// model.outputDotFile(dotPath);
		return true;
	}

	/**
	 * Transform the before LabeledPCA by disconnecting some behaviour replaced by
	 * the lpca2
	 * 
	 * @param beforeLPCA
	 * @param handler
	 * @return
	 */
	private LabeledPCA mergeLPCA(VPCA vpca, LabeledPCA beforeLPCA, int startState, int goalState,
			LabeledPCA handler) {

		/**
		 * 1. Construct the rest model from the goal extState to the final states (by
		 * exploring the EPCA)
		 */

		Map<Integer, Map<String, Object>> originVarLabels = beforeLPCA.getVarLabel();
		LabeledPCA lpca = new LabeledPCA(beforeLPCA.getPcaObj().myclone(), originVarLabels);

		Map<Integer, Set<Transition>> originLTS = lpca.getLTS();
		logger.debug(originLTS);

		Map<Integer, Set<Transition>> handlerLTS = handler.getLTS();

		Set<Transition> handOutTrans = null;
		LinkedList<Integer> queue = new LinkedList<>();
		Set<Integer> hVisited = new HashSet<>();
		int size = originLTS.size();
		int count = 0;
		int oFrom = 0;
		while (!queue.isEmpty()) {
			int hState = queue.getLast();
			hVisited.add(hState);
			handOutTrans = handlerLTS.get(hState);
			Set<Transition> oriOutTrans = cloneTransitions(handOutTrans);

			if (handOutTrans == null) {
				/**
				 * reach the handler's termination states which must be either ERROR state or
				 * the goal states. The origin model is assumed to have the new goal states
				 * explored before being assembled with the handler
				 */
				Map<String, Object> endStateVC = handler.getVarLabel().get(hState);
				boolean match = true;
				// for (int goal : goalStates) {
				Map<String, Object> goalStateVC = beforeLPCA.getVarLabel().get(goalState);
				for (String varName : goalStateVC.keySet()) { // for each variable
					Object value = goalStateVC.get(varName);
					Object hValue = endStateVC.get(varName);
					if (!value.toString().equals(hValue.toString())) {
						match = false;
						break;
					}
				}
				if (match) {
					// find the goal state

				}
				// }
			}
			if (hState == 0) {
				originLTS.put(startState, oriOutTrans);
				oFrom = startState;
			} else {
				originLTS.put(size + count, oriOutTrans);
				oFrom = size + count;
				count++;
			}
			for (Transition t : handOutTrans) {
				int targetState = t.getTo();
				if (!hVisited.contains(targetState)) {
					queue.addFirst(targetState);
				}

				for (Transition tt : oriOutTrans) {
					if (tt.getLabel().equals(t.getLabel()) && tt.getFrom() == t.getFrom()
							&& tt.getTo() == t.getTo()) {
						tt.setFrom(oFrom);
						tt.setTo(size + count);
						count++;
					}
				}
				// if t.getTo is the terminal state

			}
		}

		/**
		 * 2. Build global transformed LTS model by concatenating the recovery model to
		 * ken's epca, and disconnect from the inconsistent state
		 */
		SortedMap<String, Set<ExtTransition>> transformedLTS = null;
		Map<Integer, ExtState> eStateMap = null;
		// transformedLPCA = new LabeledPCA(cs, null);

		/**
		 * 4. Generate PFSP from the after LTS
		 */

		/**
		 * 5. compile and generate CompactState of the after model whereby the new
		 * LabeledPCA is generated and returned
		 */

		return null;
	}

	/**
	 * clone the transitions
	 * 
	 * @param transitions
	 * @return
	 */
	private Set<Transition> cloneTransitions(Set<Transition> transitions) {
		Set<Transition> cloneTrans = new HashSet<>();
		for (Transition t : transitions) {
			cloneTrans.add(t.clone());
		}
		return cloneTrans;
	}

	/**
	 * check if Ken ken is the consumer in the configuration, to be implemented
	 * 
	 * @param conf
	 * @param ken
	 * @return
	 */
	private boolean isConsumer(Configuration conf, Ken ken) {
		if (ken.getName().equals("FM"))
			return true;
		return false;
	}

	/**
	 * Exhausting a shuffle of local VCs on the abstract state absStrateID
	 * 
	 * @param absStateID
	 * @return
	 */
	private Vector<ExtState> getCandidateGoalExtStates(int absStateID, Vector<VariableType> locVars,
			Ken ken) {
		Vector<ExtState> extStates = new Vector<>();
		// Map<String, Map<Object, Double>> vars = new TreeMap<String, Map<Object,
		// Double>>();
		TreeMap<String, Map<Object, Double>> vars = new TreeMap<>();
		for (VariableType v : locVars) {
			if (v.getKind() == VariableType.LOCAL_KIND) {
				vars.put(v.getNamespace() + "::" + v.getVarName(), v.getProbDist());
			}
		}
		// * shuffle the locVars on the absStateID
		// List<List<Triplet<String, Object, Double>>> vcs =
		// ExtState.cartProdAssignedValues(vars);
		List<List<Triplet<String, Object, Double>>> vcs = VPCA.cartesianProductdValues(vars);

		TreeMap<String, Map<Object, Double>> vcValues = new TreeMap<>();

		for (List<Triplet<String, Object, Double>> list : vcs) {
			logger.debug("*********	For each VC ***************");
			list.forEach(triplet -> {
				double p = triplet.getValue2();
				String varName = triplet.getValue0();
				Object varValue = triplet.getValue1();
				logger.debug(varName + "=" + varValue + "\t prob:" + p);
				// initValues.put(.getNamespace() + "::" + v.getVarName(), v.getProbDist());
				Map<Object, Double> value = new HashMap<>();
				value.put(varValue, 1.0);
				vcValues.put(varName, value);
			});
			ExtState eState = new ExtState(ken.getName(), absStateID, vcValues);
			extStates.add(eState);
		}
		logger.debug(extStates);
		return extStates;
	}

	/**
	 * construct the local post recovery behaviour from the goal state to the
	 * termination states
	 * 
	 * @param goalExtState
	 * @param vpca
	 * @return
	 */
	private LabeledPCA getPostRecoveryBehaviour(ExtState goalExtState, VPCA vpca) {
		// meta question: which object do you place the algorithm? VPCA? or ken's
		// labeledPCA? the solution is VPCA, but generating a LabeledPCA object
		LinkedList<String> extStateQueue = new LinkedList<>();

		// 1). find abstract state by goalExtState -> abstract state ID
		int absStateID = goalExtState.getAbsStateID();

		// 2). get outgoing abstract transitions from the absStrateID, should be in
		LinkedList<Integer> absStateIdQ = new LinkedList<>();
		absStateIdQ.addFirst(absStateID);

		while (!absStateIdQ.isEmpty()) {
			absStateID = absStateIdQ.removeLast();
			// get all outgoing abstract transitions from the goal abstract state
			Set<Transition> outTrans = vpca.getTemptlatePca().getOutTransFromState(absStateID);

			// for each abstract transition absTran:
			for (Transition absTran : outTrans) {
				// 3). for each absPcaTran, creating a set of ExtTransitions using
				// refineAbstractTransition(absPcaTran, goalExtState)
				Set<ExtTransition> extTrans = null;
				try {
					extTrans = vpca.refineAbstractTransition(absTran, goalExtState);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// 4). For each refined ExtTransitiions, get a set of target ExtStates until
				// all states are visited.
				for (ExtTransition extTran : extTrans) {
					ExtState targetExtState = extTran.getToState();
					// if the targetExtState does not exist
					if (!vpca.getExtStates().entrySet()
							.contains(targetExtState.getExtStateLabel())) {
						// optionally set the extTransitions
						// vpca.getExtTransitions().put(extTran.getExtEventLabel(),
						// extTran);
						vpca.getExtTrans().add(extTran);
						extStateQueue.addFirst(targetExtState.getExtStateLabel());
						// adding new state to the vpca
						vpca.getExtStates().put(targetExtState.getExtStateLabel(), targetExtState);
						absStateIdQ.addFirst(targetExtState.getAbsStateID());
					} else {
						logger.debug("existing ExtState " + targetExtState.getExtStateLabel());
					}
				}
			}
		}

		/**
		 * debug the transformed model LTS
		 */
		logger.debug(vpca.getExtTrans());
		logger.debug(vpca.getExtStates());

		// To be made by hand
		CompactState cs = null;
		Map<Integer, ExtState> extStates = null;
		LabeledPCA postRecovery = new LabeledPCA(cs, null);
		return postRecovery;
	}

	/**
	 * Generate a recovery plan based on transition-based approach, with
	 * corresponding AC.
	 * 
	 * @param initExtState
	 * @param goalExtState
	 * @param basicKen
	 */
	private LabeledPCA getHandlerLpca(ExtState initExtState, ExtState goalExtState, Ken basicKen) {
		/**
		 * Planning rec paths (CompositeLPCA) from extState to the goal extState as a
		 * recovery, the plan is a global model synced between ken and dependent kens
		 */
		// Vector<CompositeLPCA> recComLpcaVec = config.recoveryPlan(initExtState,
		// basicKen,
		// goalExtState);

		/**
		 * Generate transformed architecture configuration, which step to update the AC?
		 */

		// Configuration newConfig = null;

		/**
		 * concatenating the comLpca into the existing basicKen's model
		 */
		// recComLpcaVec.forEach(comLpca -> {
		// config.getTransformedLPCA(comLpca, initExtState, goalExtState, basicKen);
		// });// For each planned recovery
		//
		CompactState compactState = null;
		// Map<Integer, ExtState> extStates = null;
		LabeledPCA handler = new LabeledPCA(compactState, null);
		return handler;
	}

	/**
	 * Recover from each relevant local ken, to achieve the global consistency.
	 * 
	 * @param conf
	 * @param relevantKens
	 * @param goalExtState
	 */
	public void gloRecovery(Configuration conf, Vector<Ken> relevantKens, int goalExtState) {
		for (Ken ken : relevantKens) {
			int locStateID = 0; // get corresponding localstate from the global stateID
			this.recovery(conf, ken, locStateID);
		}
	}

	/**
	 * Restore the counterexample in terms of transitions. Note that the gloModel
	 * must be generated at the same time when the PRISM model is generated which is
	 * the basis of the state mapping and countereample generator input.
	 * 
	 * @param sePath
	 * @param gloModel
	 * @return
	 */
	public Vector<Transition> restoreSETransition(Vector<Integer> sePath, CompositeLPCA gloModel) {
		Vector<Transition> seTrans = new Vector<>();
		Map<Integer, Set<Transition>> lts = gloModel.getGloLPCA().getLTS();
		for (int index = 0; index < sePath.size() - 1; index++) {
			int currentState = sePath.get(index);
			int nextState = sePath.get(index + 1);
			Set<Transition> outTrans = lts.get(currentState);
			outTrans.forEach(t -> {
				if (t.getTo() == nextState)
					seTrans.add(t);
			});
		}
		return seTrans;
	}

	/**
	 * Model checking the architecture configuration's global model against the
	 * property, and return the counterexample if any
	 * 
	 * @param conf
	 * @param property
	 * @return
	 */
	public Vector<Integer> modelchecking(Configuration conf, String propName) {

		// using Comics to generate the counterexample paths, and generate the
		CounterexampleComicsImpl ce = new CounterexampleComicsImpl(propName, propName);

		return ce.getSePath();
	}

	// ****************** GETTER and SETTER Methods *******************************

	public LabeledPCA getRecLocLpca(String kenName) {
		return this.recLocLpcaMap.get(kenName);
	}

	/**
	 * specify the Ken's name with its transformed pca
	 * 
	 * @param name
	 * @param recLocLpca
	 */
	public void setRecLocLpca(String name, LabeledPCA recLocLpca) {
		if (this.recLocLpcaMap == null)
			this.recLocLpcaMap = new HashMap<>();
		this.recLocLpcaMap.put(name, recLocLpca);
	}

	public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}

	public CounterexampleComicsImpl getCounterexample() {
		return counterexample;
	}

	public void setCounterexample(CounterexampleComicsImpl counterexample) {
		this.counterexample = counterexample;
	}

}
