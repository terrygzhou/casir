package edu.rmit.casir.epca;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import lts.ActionName;
import lts.CompactState;
import lts.EventState;
import lts.Pair;
import lts.SymbolTable;
import lts.Transition;
import org.apache.log4j.Logger;
import org.javatuples.Triplet;

import api.APITest;
import api.PFSPCompiler;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.CollectionUtil;
import edu.rmit.casir.util.FileHandler;
import edu.rmit.casir.util.GeneralUtil;

/**
 * EPCA is the formal model of EPCA by the argumentation of PCA with variables'
 * guards
 * <p>
 * EPCA model is built by reading an EPCA-FSP file. EPCA represents a single
 * EPCA process that is derived/generated from EPCA-FSP's single process
 * 
 * @author terryzhou
 */
public class VPCA {

	Logger logger = Logger.getLogger("edu.rmit.casir.epca.EPCA");

	// the following attributes are given by design
	String processName;

	Set<VariableType> variables;

	Set<ExtAction> extActions;

	// The key is the label i.e. abstract_state_name{processName::vc}
	Map<String, ExtState> extStates;

	// Map<actType<prob>actLabel, List<VariableInstance>>
	Map<String, List<VariableInstance>> instValues;

	// Map<String, ExtTransition> extTransitions = new HashMap<String,
	// ExtTransition>();

	Set<ExtTransition> extTrans = new HashSet<ExtTransition>();

	// the following attributes are derived from EPCA

	// Abstract protocol, extracted from EPFSP
	CompactStateRef temptlatePca;

	public String[] alphabet;

	public EventState[] states;

	Map<Integer, ExtState> stateMap;

	// the unfoldedLPCA compiled from the unfolded P-FSP, redundant with the
	// unfoldedLPCA
	private CompactStateRef unfoldingPCA;
	// redundant with unfoldedLPCA, to be fixed.

	String unfoldedFSP;

	String abstractFSP;

	// The unfolded labeled PCA, must be created after object unfoldingPCA is
	// created via this.getUnfoldingPCA()
	LabeledPCA unfoledLPCA;

	// the key is the extState's label, the value is the set of outgoing
	// ExtTransitions
	SortedMap<String, Set<ExtTransition>> extLTS;

	// ****************************** Methods ********************************

	/**
	 * Read epfsp into EPCA model:1) read PCA model to set template PCA model 2)
	 * associate guards and update function to extActions. Result: (1) Set all
	 * variables, initial values and domains; (2) Set all Guards and update
	 * functions. (3) attach guards and update functions to actions (4) Generate
	 * EPCA model (5)unfolding to PCA model
	 * 
	 * @param epfspProcess
	 */
	public VPCA(String name) {
		this.variables = new HashSet<VariableType>();
		this.extActions = new HashSet<ExtAction>();
		this.extStates = new HashMap<>();
		this.processName = name;
	}

	public void addExtAction(ExtAction extAction) {
		this.extActions.add(extAction);
	}

	/**
	 * @param actions
	 */
	public void setExtActions(Set<ExtAction> actions) {
		this.extActions = actions;
	}

	/**
	 * Generate a set of initial ext states from the super start state
	 * 
	 * @param ssState
	 * @param lts
	 * @return
	 */
	public Set<ExtState> initialiseState(ExtState ssState,
			SortedMap<String, Set<ExtTransition>> lts) {
		TreeMap<String, Map<Object, Double>> vars = new TreeMap<>();
		Set<ExtState> originalInitStates = new HashSet<>();

		ssState.getLocVarValueMap().keySet().stream().forEach(var -> {
			Map<Object, Double> cloneVarObj = new TreeMap<>();
			Map<Object, Double> val = ssState.getLocVarValueMap().get(var);
			ssState.getLocVarValueMap().get(var).keySet().stream().forEach(valueObj -> {
				cloneVarObj.put(valueObj, val.get(valueObj));
			});
			vars.put(var, cloneVarObj);
		});

		// Map<String, Map<Object, Double>> allDefaultVars =
		// this.getInterVarDefaultValues();
		Map<String, Map<Object, Double>> relevantVars = new HashMap<>();
		/**
		 * Do NOT need include interface variables in the initialisation transitions
		 */
		Set<ExtTransition> outTrans = new HashSet<>();
		List<List<Triplet<String, Object, Double>>> allAssValues = VPCA
				.cartesianProductdValues(vars);

		for (List<Triplet<String, Object, Double>> comb : allAssValues) {
			String varStr = "";
			double p = 1;

			for (Triplet<String, Object, Double> v : comb) {
				p = p * v.getValue2();
				varStr = varStr + "_" + v.getValue0() + "_" + v.getValue1();
			}

			/**
			 * Java 8, the first "_" of varStr is missing using Java 8
			 */
			// p =comb.stream().mapToDouble(v -> v.getValue2()).reduce(1, (a, b)-> a*b);
			// varStr=comb.stream().map(v -> v.getValue0()+"_"+v.getValue1())
			// .reduce("", (a, str)-> a+str);

			if (p == 0)
				continue;
			ExtAction initAction = new ExtAction("init_" + this.processName.toLowerCase(),
					ExtAction.TYPE_INTERNAL, 1.0);
			ExtTransition initTran = new ExtInitTransition(initAction, comb);
			if (initTran.getProbability() == 0)
				continue;
			initTran.setExtEventLabel("init_" + this.processName.toLowerCase() + varStr);
			initTran.setFromState(ssState);
			ExtState initState = new ExtState(this.processName, 0,
					initTran.convertTreeMapValues(comb));

			// if(initState.getInTrans()==null)
			// initState.setInTrans(new HashSet<ExtTransition>());
			initState.getInTrans().add(initTran);

			// if(ssState.getOutTrans()==null)
			// ssState.setOutTrans(new HashSet<ExtTransition>());
			ssState.getOutTrans().add(initTran);

			originalInitStates.add(initState);
			initTran.setToState(initState);
			outTrans.add(initTran);

			// this.getExtTransitions().put(initTran.getExtEventLabel(), initTran);

		} // for
		lts.put(ssState.getExtStateLabel(), outTrans);
		return originalInitStates;
	}

	/**
	 * Generating underlying Labeled transition system that can be visualized
	 * <p>
	 * whereby the PFSP from given VPFSP, get the template PCA(automata) model.
	 * <P>
	 * Condition: (1) The template PCA has been complied with LTS produced; (2)
	 * Variables with initial values, ExtActions been set;
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map<String, Set<ExtTransition>> generateUnfoldLTS() throws Exception {
		/**
		 * get template PCA to produce template LTS (tLTS), by callout
		 */
		// CompactStateRef csr = new CompactStateRef(this.temptlatePca);
		CompactStateRef csr = this.temptlatePca;
		Map<Integer, Set<Transition>> templateLts = null;

		/**
		 * There is a bug for Travel_Agency_6.epca example
		 */
		templateLts = csr.getLTS();
		// templateLts = csr.outputLTS2();

		/**
		 * From the initial state in the template PCA, attach values and apply extAction
		 * rules, to generate refined LTS with relabeling.
		 */
		Set<String> visitedExtStates = new HashSet<>();
		LinkedList<ExtState> stateQueue = new LinkedList<ExtState>();
		this.extLTS = new TreeMap<String, Set<ExtTransition>>();
		// EPCA super initial state is always -2
		ExtState sisState = new ExtState(this.processName, -2, this.getSuperStartLocValue());
		sisState.setInTrans(null);

		this.extStates.put(sisState.getExtStateLabel(), sisState);
		Set<ExtState> initStates = this.initialiseState(sisState, this.extLTS);
		stateQueue.addAll(initStates);
		logger.debug(initStates);

		while (!stateQueue.isEmpty()) {
			// dequeue
			ExtState currentExtState = stateQueue.removeFirst();
			
			if(currentExtState.getExtStateLabel().equals("WD0{WD::over={0=1.0}, WD::owed={0=1.0}, WD::stock={0=1.0}}"))
				logger.debug("debug");

			// mark it visited
			visitedExtStates.add(currentExtState.getExtStateLabel());
			// logger.info(visitedExtStates);

			if (currentExtState.getAbsStateID() == -1) {
				continue;
			}
			int absStateID = currentExtState.getAbsStateID();

			// * ambiguous here between the temp PCA and unfolded PCA...
			Set<Transition> outTempPCATrans = templateLts.get(absStateID);

			// for a state without outgoing transitions e.g. deadlock or END
			if (outTempPCATrans == null) {
				this.extStates.put(currentExtState.getExtStateLabel(), currentExtState);
				if (this.temptlatePca.getPcaObj().getEndseq() == absStateID) {
					logger.debug("END state found");
					// * create super final state
					ExtState sfs = new ExtState(processName, absStateID + 1, null);
					sfs.setExtStateLabel("END");
					sfs.setExplicit(true);
					this.extStates.put(sfs.getExtStateLabel(), sfs);

					// * super termination transition, leading to the super END state.
					ExtAction ea = new ExtAction(processName.toLowerCase() + "_end",
							ExtAction.TYPE_INTERNAL, 1.0);
					ExtTransition endTran = new ExtInternalTransition(ea, null);

					if (sfs.getInTrans() == null)
						sfs.setInTrans(new HashSet<ExtTransition>());
					sfs.getInTrans().add(endTran);

					// currentState.outTrans.add(endTran);
					endTran.fromState = currentExtState;
					endTran.toState = sfs;
					Set<ExtTransition> extTrans = new HashSet<ExtTransition>();
					extTrans.add(endTran);

					currentExtState.outTrans = extTrans;
					extLTS.put(currentExtState.getExtStateLabel(), extTrans);

				} else {
					logger.debug("STOP state found??");
					// create a super deadlock state???
				}
				continue;
			} // if end/stop state

			Set<ExtTransition> outTempTrans = new HashSet<ExtTransition>();

			// outTempTrans.add(getExtTransitions())

			for (Transition tempPcaTran : outTempPCATrans) {
				Set<ExtTransition> refinedExtTrans = this.refineAbstractTransition(tempPcaTran,
						currentExtState);
				double p = 0;

					
				if (currentExtState.outTrans == null) {
					logger.debug(currentExtState.getExtStateLabel());
					continue;
				}

				for (ExtTransition et : refinedExtTrans) {
					logger.debug(et.getExtEventLabel());

					p = p + et.getProbability();
					// et.setFromState(currentState);

					currentExtState.outTrans.add(et);

					Map<String, Map<Object, Double>> targetLocValues = et.getTargetValues();
					logger.debug(targetLocValues);

					int targetAbsStateID = tempPcaTran.getTo();
					ExtState extToState = null;
					// targetLocValues=[<namespace::varname, <value1, p1>,
					// <value2, p2>... >]
					/**
					 * validate the target values to ensure them within the var's domain
					 */
					if (!isValidateValue(targetLocValues)) {
						/*
						 * also need change action type to ~ action.. This might be semantically
						 * deviated from original semantics To be discussed with Heinz.
						 */
						// et.getExtAction().setActionType(ActionType.);
						extToState = this.createExtTargetState(et, -1, this.extStates);
						logger.fatal("out of the bound of domain");
						// System.exit(-1);
						// throw new Exception("out of the domain");
					} else
						/**
						 * create a new target ExtState, and set the target state for the transition
						 */
						extToState = this.createExtTargetState(et, targetAbsStateID,
								this.extStates);

					if (!visitedExtStates.contains(extToState.getExtStateLabel())) {
						stateQueue.addLast(extToState);
						logger.debug("push new target statte in Q \t" + extToState.getFSPLabel());
						logger.debug("Q status:\t" + stateQueue);

					}

				} // for each extTran, setting from/to states

				outTempTrans.addAll(refinedExtTrans);

			} // for template t

			extLTS.put(currentExtState.getExtStateLabel(), outTempTrans);
			logger.debug(extLTS.size());
			logger.debug(extLTS);
			this.extStates.put(currentExtState.getExtStateLabel(), currentExtState);
		} // while

		// logger.debug(extLTS);
		logger.debug("********************	debug VPCA's LTS		**********************");
		extLTS.keySet().forEach(state -> {
			extLTS.get(state).forEach(extTran -> {
				logger.debug(state + "\t" + extTran.getExtEventLabel() + "\t"
						+ extTran.getToState().getFSPLabel());
			});
		});
		logger.debug(this.extStates);
		return extLTS;
	}

	/**
	 * Visualie the VPCA via its LTS
	 * 
	 * @return
	 */
	public String visualizeLTS() {
		if (this.extLTS == null)
			try {
				this.generateUnfoldLTS();
			} catch (Exception e) {
				e.printStackTrace();
			}
		// in case the stateID has not yet been set
		this.getStateIDToExtStateMap();

		StringBuffer graphvizSpec = new StringBuffer();
		graphvizSpec.append("digraph " + this.getProcessName() + " {\n" + "\t rankdir = LR;\n"
				+ "\t graph [fontname = \"monospace\", ranksep=\"0.2\", nodesep=\"0.1\"]; \n"
				+ "\t node [fontname = \"monospace\"]; \n"
				+ "\t edge [fontname = \"monospace\"]; \n");

		// graphvizSpec.append("\t 0 [label =\" 0 \\n SuperInitialState\" "
		// + "shape = box, color = black, style = filled, fillcolor = orange]; \n");
		for (String label : extLTS.keySet()) {
			ExtState extState = this.getExtStates().get(label);

			int stateID = extState.getStateID();
			int absStateID = extState.getAbsStateID();
			if (absStateID == 2)
				logger.debug("...........");
			TreeMap<String, Map<Object, Double>> locVars = extState.getLocVarValueMap();

			// StringBuffer stateNodeStr = new StringBuffer(
			// "\t" + extState.getFSPLabel() + "[label=\"" + label);
			StringBuffer stateNodeStr = new StringBuffer("\t" + extState.getStateID() + "[label=\""
					+ extState.getStateID() + "\n" + label);

			if (locVars != null) {
				for (String key : locVars.keySet()) {
					Map<Object, Double> value = locVars.get(key);
					stateNodeStr.append("\\n" + key + "==" + value.keySet().toArray()[0]);
				}
				/**
				 * adding local vPCA's abstract stateID in the state label
				 */
				stateNodeStr.append("\\n abstractstate ==" + extState.getAbsStateID() + "\"");
				graphvizSpec.append(stateNodeStr
						+ " shape = box, color = black, style = filled, fillcolor = cyan]; \n");
			}
		}

		for (String currentStateLabel : extLTS.keySet()) {
			ExtState fromExtState = this.getExtStates().get(currentStateLabel);
			Set<ExtTransition> outTrans = extLTS.get(currentStateLabel);
			for (ExtTransition extTran : outTrans) {
				ExtState toState = extTran.getToState();
				extTran.extAction.getActionLabel();
				String tranLabel = extTran.getExtAction().getActionType() + "<"
						+ extTran.getProbability() + ">" + extTran.getFSPLabel() + "\"];\n ";
				graphvizSpec.append("\t " + fromExtState.getStateID() + " -> "
						+ toState.getStateID() + "[label = \"" + tranLabel);
			}
		}
		graphvizSpec.append("}\n");
		return graphvizSpec.toString();
	}

	/**
	 * Return a set of refined ExtTransition based on an abstract transition and
	 * current ExtState (with local VCs)
	 * 
	 * @param absPcaTran
	 * @return
	 * @throws Exception
	 */
	public Set<ExtTransition> refineAbstractTransition(Transition absPcaTran,
			ExtState sourceExtState) throws Exception {

		// find the extAction from template PCA's action (label)
		String actLabel = absPcaTran.getEventPCA().getOriginalLabel();
		String type = absPcaTran.getEventPCA().getTypeString();
		// int targetAbstractStateID = tempPcaTran.getTo();
		// logger.info("***************\t" + targetAbstractStateID);
		double prob = absPcaTran.getEventPCA().getProbability();

		ExtAction extAction = null;
		try {
			/**
			 * Find the existing ExtAction.
			 * <p>
			 * The assumption is the unique action is found from the vPFSP by its label,
			 * type and probability.
			 */
			extAction = this.findExtAction(actLabel, type, prob);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		TreeMap<String, Map<Object, Double>> vars = new TreeMap<>();
		/**
		 * By default, vars are local variables; for an interface action,taking into
		 * account interface vars
		 */
		if (type.contains("?") || type.contains("!")) {
			Map<String, Map<Object, Double>> intVarMap = this.getInterVarDefaultValues();
			for (String key : intVarMap.keySet()) {
				String ns = key.substring(0, key.indexOf(":"));
				if (ns.equals(actLabel))
					vars.put(key, intVarMap.get(key));
			}
			// vars.putAll(this.getInterVarDefaultValues());
			/**
			 * overwrite if instance values are assigned
			 */
			String actKey = extAction.getActionType() + "<" + extAction.getProbability() + ">"
					+ extAction.getActionLabel();
			logger.debug(actKey);
			for (String akey : this.getInstnaceValues().keySet()) {
				if (akey.equals(actKey)) {
					logger.debug("Matched " + akey);
					List<VariableInstance> instVars = this.getInstnaceValues().get(akey);
					logger.debug(instVars);
					for (VariableInstance vi : instVars) {
						logger.debug(vi.instValueDist);
						vars.put(vi.namespace + "::" + vi.varName, vi.getInstValueDist());
					}
				} // if instance variables are applied on the action
			}
		}

		List<List<Triplet<String, Object, Double>>> allAssValues = VPCA
				.cartesianProductdValues(vars);
		logger.debug("allAssValues size " + allAssValues.size());

		/**
		 * Creating ExtTransition based on the ExtAction by enriching the interface
		 * variables, w.r.t the following scenarios
		 */
		ExtTransition extTran = null;

		Set<ExtTransition> extTrans = new HashSet<>();

		switch (type) {
		case ExtAction.TYPE_INPUT:
		case ExtAction.TYPE_INPUT_FAIL:
			if (allAssValues.size() == 0) {
				extTran = new ExtInputTransition(extAction, null);
				extTran.setFromState(sourceExtState);
				// sourceExtState.outTrans.add(extTran);

			} else
				for (List<Triplet<String, Object, Double>> valueInstance : allAssValues) {
					extTran = new ExtInputTransition(extAction, valueInstance);
					extTran.setFromState(sourceExtState);
					// sourceExtState.getOutTrans().add(extTran);
					// remove all trans with p=0
					if (extTran.getProbability() > 0)
						extTrans.add(extTran);
				}
			break;
		case ExtAction.TYPE_ONPUT:
		case ExtAction.TYPE_ONPUT_FAIL:
			if (allAssValues.size() == 0) {
				extTran = new ExtOutputTransition(extAction, null);
				extTran.setFromState(sourceExtState);
				// sourceExtState.getOutTrans().add(extTran);
			} else
				for (List<Triplet<String, Object, Double>> valueInstance : allAssValues) {
					logger.debug(valueInstance);
					extTran = new ExtOutputTransition(extAction, valueInstance);
					// sourceExtState.getOutTrans().add(extTran);
					if (extTran.getProbability() > 0) {
						extTran.setFromState(sourceExtState);
						extTrans.add(extTran);
					}
				}
			break;
		case ExtAction.TYPE_INTERNAL:
		case ExtAction.TYPE_INTERNAL_FAIL:
			if (allAssValues.size() == 0) {
				extTran = new ExtInternalTransition(extAction, null);
				extTran.setFromState(sourceExtState);
				// sourceExtState.getOutTrans().add(extTran);

			} else
				for (List<Triplet<String, Object, Double>> valueInstance : allAssValues) {
					extTran = new ExtInternalTransition(extAction, valueInstance);
					extTran.setFromState(sourceExtState);
					// sourceExtState.getOutTrans().add(extTran);
					if (extTran.getProbability() > 0)
						extTrans.add(extTran);
				}
			break;
		} // switch type

		Set<ExtTransition> outTrans = sourceExtState.getOutTrans();
		if (outTrans == null) {
			outTrans = new HashSet<>();
			sourceExtState.setOutTrans(outTrans);
		}

		if (sourceExtState.getAbsStateID() == 10)
			logger.debug("debug");

		sourceExtState.getOutTrans().addAll(extTrans);
		logger.debug(sourceExtState + "\t" + extAction.getActionType() + "<"
				+ extAction.getProbability() + ">" + extAction.getActionLabel() + "\n" + extTrans);

		return extTrans;
	}

	/**
	 * Create target ExtState based on a given ExtTransition et (with the source
	 * ExtState set) and the target abstract StateID, and set the state as the
	 * target ExtState for the ExtTransition et.
	 * <p>
	 * binding the et and targetstate in bi-direction
	 * 
	 * @param t
	 * @return
	 */
	private ExtState createExtTargetState(ExtTransition et, int targetAbstractStateID,
			Map<String, ExtState> extStates) {

		// int targetAbstractStateID = et.toState.getAbsStateID();
		logger.debug("***************\t" + targetAbstractStateID);

		ExtState extToState = new ExtState(this.processName, targetAbstractStateID,
				et.getTargetValues());

		String toStateLabel = extToState.getExtStateLabel();
		/**
		 * for debugging...
		 */
		// if (toStateLabel.contains("FC4")) {
		// logger.debug("debugging......");
		// APITest.printPCA(this.temptlatePca.getPcaObj());
		// }
		if (extStates.get(toStateLabel) != null) {
			extToState = extStates.get(toStateLabel);
		} else {
			// logger.info("put the extState into the extStates?");
			// the extStates is updated on fromState, instead of toState
			// this.extStates.put(extToState.getExtStateLabel(), extToState);
		}

		logger.debug(this.temptlatePca.getPcaObj().getEndseq());
		/**
		 * to be replaced by a internal func
		 */
		if (targetAbstractStateID == this.temptlatePca.getPcaObj().getEndseq()) {
			// if (this.isStateWithoutOutTrans(targetAbstractStateID, this.temptlatePca)) {
			logger.debug("find END state.");
			// extToState.setExtStateLabel("END");
			extToState.setOutTrans(null);
			/**
			 * Questions here: how to deal with SFS in unfolding PCA???
			 */
			extToState.setExplicit(false);
		}

		List<Guard> gList = et.getExtAction().getGuardsList();

		if ((gList == null || gList.size() == 0) && extToState.getAbsStateID() != -1)
			extToState.setExplicit(false);

		et.setToState(extToState);
		logger.debug(et.getFromState().getExtStateLabel() + "\t" + et.getExtAction().getActionType()
				+ "<" + et.getProbability() + ">" + et.getExtEventLabel() + "\t"
				+ extToState.getExtStateLabel());

		extToState.getInTrans().add(et);
		return extToState;
	}

	private boolean isStateWithoutOutTrans(int stateID, CompactStateRef pca) {
		EventState[] pcaStates = pca.getPcaObj().states;
		for (int i = 0; i < pca.getPcaObj().maxStates; i++) {
			if (stateID == 4) {
				EventState s = pcaStates[i];
				// if(s.getNext()==null)
				logger.debug(s.getNext());
				return true;
			}
		}

		return false;
	}

	/**
	 * this is to merge same actionlabel with the addition of the probabilities
	 * 
	 * @return
	 */
	public Map<String, Set<ExtTransition>> normalizeExtLTS() {
		if (this.getExtLTS() == null)
			try {
				this.generateUnfoldLTS();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		for (String stateExtLabel : this.getExtLTS().keySet()) {
			ExtState fromState = this.extStates.get(stateExtLabel);
			Set<ExtTransition> outTrans = this.extLTS.get(stateExtLabel);
			Set<String> toExtStateStrSet = new HashSet<>();
			Map<String, Set<ExtTransition>> toStateTranMap = new HashMap<>();
			Set<ExtTransition> tobeDel = new HashSet<>();
			logger.debug("from state " + fromState.getExtStateLabel() + "\t" + outTrans);
			/**
			 * For each outgoing transition from a state
			 */
			for (ExtTransition outTran : outTrans) {
				logger.debug(outTran);
				String outTranLabel = outTran.getExtEventLabel();
				ExtState toState = outTran.getToState();
				String toStateLabel = toState.getExtStateLabel();
				if (toExtStateStrSet.contains(toStateLabel)) {
					logger.debug(toExtStateStrSet + "\n" + toStateLabel);

					Set<ExtTransition> existTrans = toStateTranMap.get(toStateLabel);
					logger.debug(existTrans + "\t" + outTran);
					Set<ExtTransition> tobeAdded = new HashSet<>();

					for (ExtTransition existTran : existTrans) {
						String existTranLabel = existTran.getExtEventLabel();
						logger.debug(existTran);
						if (existTranLabel.equals(outTranLabel)) {
							double existProb = GeneralUtil.round(existTran.getProbability(), 4);
							double newProb = existProb
									+ GeneralUtil.round(outTran.getProbability(), 4);
							existTran.setProbability(GeneralUtil.round(newProb, 4));
							/**
							 * remove outTran from ExtLTS
							 */
							toState.inTrans.remove(outTran);
							fromState.outTrans.remove(outTran);
							tobeDel.add(outTran);
						} else {
							if (!isMemberOfTransitionSet(existTrans, outTran))
								tobeAdded.add(outTran);
						}
					} // for all existing normal Tran
					existTrans.addAll(tobeAdded);
					toStateTranMap.put(toStateLabel, existTrans);
				} else {
					// toStateTranMap.put(toStateLabel, outTran);
					Set<ExtTransition> mergedTrans = toStateTranMap.get(toStateLabel);
					if (mergedTrans == null)
						mergedTrans = new HashSet<>();
					mergedTrans.add(outTran);
					toStateTranMap.put(toStateLabel, mergedTrans);
					toExtStateStrSet.add(toStateLabel);
				}

			} // for each outTran

			/**
			 * remove the duplicated outTrans
			 */
			logger.debug(tobeDel);
			logger.debug(this.extLTS.get(stateExtLabel));
			this.extLTS.get(stateExtLabel).removeAll(tobeDel);
			logger.debug(this.extLTS.get(stateExtLabel));
		}
		return this.extLTS;
	}

	private boolean isMemberOfTransitionSet(Set<ExtTransition> group, ExtTransition tran) {
		String tranLabel = tran.getExtEventLabel();
		for (ExtTransition t : group) {
			String tLabel = t.getExtEventLabel();
			if (tLabel.equals(tranLabel))
				return true;
		}
		return false;
	}

	/**
	 * check if the values are in the variables' domains
	 * 
	 * @param values
	 * @return
	 */
	private boolean isValidateValue(Map<String, Map<Object, Double>> values) {
		for (String vKey : values.keySet()) {
			VariableType vt = this.getVarByNamespaceAndName(vKey);
			Set<Object> domain = vt.getDomain();
			Set<String> domainStr = new HashSet<>();
			Map<Object, Double> value = values.get(vKey);
			for (Object o : value.keySet()) {
				// if(domainStr.contains(o.toString()))
				if (GeneralUtil.isBoolean(o.toString()))
					continue;
				if (!domain.contains(o))
					return false;
			}
		}
		return true;
	}

	/**
	 * Given an extended LTS, output the P-FSP to generate PCA using LTSA-PCA
	 * Assuming the states are all connected in LTS
	 * 
	 * @param extLTS
	 * @return
	 */
	public String unfoldPfsp() {
		if (this.getExtLTS() == null)
			try {
				this.generateUnfoldLTS();
			} catch (Exception e) {
				e.printStackTrace();
			}
		// this.normalizeExtLTS();
		// this.extFspTransitions = new HashMap<String, ExtTransition>();
		StringBuffer fsp = new StringBuffer();
		// fsp.append("pca \n");
		// fsp.append(this.processName + this.getInitValue() + "=(");
		Set<String> visited = new HashSet<String>();
		LinkedList<ExtState> queue = new LinkedList<ExtState>();
		ExtState ssState = this.findSuperStartExtState(extLTS);
		queue.addFirst(ssState);
		while (!queue.isEmpty()) {
			ExtState state = queue.removeLast();

			/**
			 * debugging
			 */
			logger.debug("visiting " + state.getExtStateLabel());
			logger.debug("queue status:");
			queue.forEach(estate -> {
				logger.debug(estate.getFSPLabel());
			});
			// if state has not been visited
			if (state.isExplicit() && !visited.contains(state.getExtStateLabel())) {
				visited.add(state.getExtStateLabel());
				Set<ExtTransition> outTrans = extLTS.get(state.getExtStateLabel());

				if (outTrans == null || outTrans.size() == 0) {
					logger.debug("dead end state" + state.getFSPLabel());
					continue;
				}

				if (state == this.findSuperStartExtState(extLTS))
					// super start state is shown P_ss
					// fsp.append(this.processName + "_ss = (");
					fsp.append(this.processName + " =  (");

				else
					fsp.append(state.getFSPLabel() + " =  (");

				int i = 0;
				for (ExtTransition outTran : outTrans) {
					i++;
					if (outTran == null)
						continue;

					ExtAction act = outTran.getExtAction();
					String actType = act.getActionType();
					// get probability from extAction
					double prob = outTran.getProbability();
					// get guard label for relabelling
					String extLabel = outTran.getFSPLabel();
					fsp.append(actType + "<" + prob + ">" + extLabel + " -> ");

					// to be modified to Set<>
					// this.extTransitions.put(extLabel, outTran);
					this.extTrans.add(outTran);

					ExtState toState = outTran.getToState();
					String toStateLabel = null;

					if (this.isTerminalState(toState, extLTS)) {
						// if (this.isTerminalState(toState)) {
						if (toState.getAbsStateID() == -1)
							toStateLabel = "ERROR";
						else
							toStateLabel = "END";

						// where is "STOP" state?

					} else if (toState.getExtStateLabel()
							.equals(this.findSuperStartExtState(extLTS).getExtStateLabel())) {
						// if it is a super start state
						toStateLabel = this.processName;
					} else {
						// for other non-terminal states
						toStateLabel = toState.getFSPLabel();
					}
					if (toState.explicit) {
						// logger.info("explicit state "+
						// toState.getExtStateLabel());
						fsp.append(toStateLabel);
						if (!visited.contains(toState.getExtStateLabel())) {
							queue.addFirst(toState);
						}
					} else { // toState is an implicit state
						logger.debug(toState.getExtStateLabel());
						displayImplState(toState, fsp, queue, visited);
					} // for implicit state
						// logger.info(fsp.toString());
						// if not the last the outTran
					if (i < outTrans.size()) {
						fsp.append("\n \t |");
					} else {
						fsp.append("),\n");
					}
				} // for outTrans
			} // if not visited
		} // while queue isNot empty
		fsp.replace(fsp.length() - 2, fsp.length() - 1, ".");
		this.setUnfoldedFSP(fsp.toString());
		return fsp.toString();
	}

	/**
	 * Output the unfolded PFSP
	 * 
	 * @param unfoldedFSPFile
	 */
	public void outputUnfoldedFSP(String unfoldedFSPFile) {
		StringBuffer unfoldedFSPSB = new StringBuffer();
		unfoldedFSPSB.append("pca \n");
		unfoldedFSPSB.append(this.getUnfoldedFSP());
		FileHandler.delFile(unfoldedFSPFile);
		try {
			FileHandler.outputPathFile(unfoldedFSPFile, unfoldedFSPSB);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void outputAbstractFSP(String absPFSPFile) {
		StringBuffer absFSPSB = new StringBuffer();
		absFSPSB.append(this.getAbstractFSP());
		FileHandler.delFile(absPFSPFile);
		try {
			FileHandler.outputPathFile(absPFSPFile, absFSPSB);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * construct fsp from the given implicit state toState. "..->(!<0.9>a1 -> ...|
	 * !<0.03>a2 -> ... | !<0.02>a3 -> ...)
	 * 
	 * @param toState
	 * @param fsp
	 * @param queue
	 * @param visited
	 */
	private void displayImplState(ExtState toState, StringBuffer fsp, LinkedList<ExtState> queue,
			Set<String> visited) {
		LinkedList<ExtTransition> tStack = new LinkedList();
		// LinkedList<ExtState> sStack = new LinkedList();
		// sStack.addLast(toState);

		String stateLabel = toState.getExtStateLabel();
		// debugging....
		if (toState.getAbsStateID() == 10)
			logger.debug(stateLabel);

		// assuming "toState" already registered, bugs here?
		Set<ExtTransition> outExtTrans = this.getOutTransitions(stateLabel);

		if (outExtTrans == null || outExtTrans.size() == 0) {
			logger.debug(toState);
			fsp.append("END");
			return;
		}

		for (ExtTransition t : outExtTrans) {
			tStack.addLast(t);
		}

		boolean firstFlag = true;
		boolean lastFlag = false;
		int braket = 0;
		while (!tStack.isEmpty()) {
			// logger.info(tStack);
			ExtTransition t = tStack.removeFirst();
			logger.debug(tStack.toString());
			// ExtState topState = sStack.getFirst();

			if (firstFlag) {
				fsp.append("(");
				firstFlag = false;
				braket++;
			} else {
				fsp.append("|");
			}

			ExtState tgtState = t.getToState();

			if (tgtState == null)
				logger.debug("debug");

			fsp.append(t.getExtAction().getActionType() + "<" + t.getProbability() + ">"
					+ t.getFSPLabel());
			fsp.append(" -> ");

			if (tgtState.isExplicit()) {
				String toStateLabel = tgtState.getFSPLabel();
				if (this.isTerminalState(tgtState, extLTS)) {
					// if (this.isTerminalState(toState)) {
					if (tgtState.getAbsStateID() == -1)
						toStateLabel = "ERROR";
					else
						toStateLabel = "END";
					// where is "STOP" state?
				}
				fsp.append(toStateLabel);
				if (!tStack.isEmpty()) {
					lastFlag = false;
					// fsp.append("|");
				} else
					lastFlag = true;
				if (!visited.contains(tgtState.getExtStateLabel())) {
					queue.addFirst(tgtState);
				}

				if (lastFlag) {
					fsp.append(")");
					braket--;
					lastFlag = false;
					return;
				}
				// }
			} else { // for implicit states
				// ....
				this.displayImplState(tgtState, fsp, queue, visited);
			} // else
		}
		for (int i = 0; i < braket; i++)
			fsp.append(")");
	}

	/**
	 * Find the superstart ExtState Object
	 * 
	 * @param extLTS
	 * @return
	 */
	public ExtState findSuperStartExtState(Map<String, Set<ExtTransition>> extLTS) {
		TreeMap<String, Map<Object, Double>> initValue = this.getSuperStartLocValue();
		for (String sid : extLTS.keySet()) {
			ExtState s = this.extStates.get(sid);
			if (s.getAbsStateID() == -2) {
				// if(initValue.toString().equals(s.getLocVarValueMap().toString()))
				if (s.getExtStateLabel().equals(this.processName + "_ss" + "-2" + initValue))
					return s;
			}
		}
		return null;
	}

	private TreeMap<String, Map<Object, Double>> getSuperStartLocValue() {
		TreeMap<String, Map<Object, Double>> initValues = new TreeMap<>();
		for (VariableType v : this.variables) {
			if (v.getKind() == VariableType.LOCAL_KIND) {
				initValues.put(v.getNamespace() + "::" + v.getVarName(), v.getProbDist());
			}
		}
		// logger.info(initValues); ok
		return initValues;
	}

	/**
	 * @param vars
	 *            e.g. [<v1,[<1,0.4><2,0.6>]>, v2,[<true,0.9><false,0.1>]]
	 * @return the cartesian product
	 */
	public static List<List<Triplet<String, Object, Double>>> cartesianProductdValues(
			TreeMap<String, Map<Object, Double>> vars) {
		List<List<Triplet<String, Object, Double>>> varList = new ArrayList<>();
		for (Entry<String, Map<Object, Double>> var : vars.entrySet()) {
			// VariableType v=var.getKey();
			String varName = var.getKey();
			Map<Object, Double> valueDist = var.getValue();
			List<Triplet<String, Object, Double>> varGroup = new ArrayList<>();
			for (Entry<Object, Double> value : valueDist.entrySet()) {
				Triplet<String, Object, Double> varUnit = Triplet.with(varName, value.getKey(),
						value.getValue());
				varGroup.add(varUnit);
			}
			varList.add(varGroup);
		}
		return CollectionUtil.cartesianProduct(varList);
	}

	/**
	 * @param state
	 * @param extLTS
	 * @return
	 */
	private boolean isTerminalState(ExtState state, Map<String, Set<ExtTransition>> extLTS) {
		for (String key : extLTS.keySet()) {
			ExtState extState = this.extStates.get(key);
			if (state.toString().equals(extState.toString())) {
				Set<ExtTransition> outTrans = extLTS.get(key);
				if (outTrans == null || outTrans.size() == 0)
					return true;
				else
					return false;
			}
		}
		return true;
	}

	/**
	 * find an extAction by given action label/name?
	 * 
	 * @param type
	 *            : !, ?, ~!, ~?, ""
	 * @return
	 * @throws Exception
	 */
	private ExtAction findExtAction(String actLabel, String type, double prob) throws Exception {
		for (ExtAction extAct : this.extActions) {
			String extActlabel = extAct.getActionLabel();
			String extActType = extAct.getActionType();
			if (extActlabel.equals(actLabel) && (extActType.equals(type))
					&& (extAct.getProbability() == prob)) {
				return extAct;
			}
		}
		logger.debug(type + "\t" + prob + "\t" + actLabel);
		this.testExtActions();
		throw new Exception("no valid ExtAction defined");
	}

	/**
	 * need to consider unspecified initial values i.e. marked by "?"
	 * 
	 * @return
	 */
	private TreeMap<String, Map<Object, Double>> getInitLocalValue() {
		TreeMap<String, Map<Object, Double>> initValues = new TreeMap<>();
		for (VariableType v : this.variables) {
			if (v.getKind() == VariableType.LOCAL_KIND) {
				initValues.put(v.getNamespace() + "::" + v.getVarName(), v.getProbDist());
			}
		}
		return initValues;
	}

	/**
	 * Compile this (EPCA) instance to an unfoldedPCA CompactState Object. todo:
	 * change to CompactStateRef object as a labeled PCA
	 * 
	 * @return
	 */
	private CompactStateRef getUnfoldingPCA() {
		// input of pFSP string reasoned from this.unfoldPfsp();
		String pfsp = this.unfoldPfsp();
		logger.debug(pfsp);
		SymbolTable.init();
		PFSPCompiler comp = new PFSPCompiler();
		// this.unfoldingPCA = comp.compile(this.getProcessName() + "_ss",
		// pfsp);
		/**
		 * compile from the pfsp to PCA object where the states are changed, and labels
		 * are lost
		 */
		this.unfoldingPCA = new CompactStateRef(comp.compile(this.getProcessName(), pfsp));
		return this.unfoldingPCA;
	}

	/**
	 * state labeling with mapping by relating this.getStateMap to CompactStateRef's
	 * statelabels
	 */
	public LabeledPCA getUnfoldedLabeledPCA() {
		CompactStateRef pca = this.unfoldingPCA;
		if (pca == null)
			pca = this.getUnfoldingPCA();

		Map<Integer, String> labels = new HashMap<Integer, String>();
		Map<Integer, Map<String, Object>> varLabels = new HashMap<>();

		// LabeledPCA labeledPCA = new LabeledPCA(pca, labels);
		Map<Integer, ExtState> stateMap = this.getStateIDToExtStateMap();

		// convert stateMap to Map<Integer, Map<String, Object>>
		Map<Integer, Map<String, Object>> varLabel = new HashMap<>();
		for (int stateId : stateMap.keySet()) { // for each state
			ExtState eState = stateMap.get(stateId);
			if (eState == null || eState.getStateID() == -1) {
				varLabel.put(stateId, new HashMap<>());
				continue;
			}
			TreeMap<String, Map<Object, Double>> vars = eState.getLocVarValueMap();
			Map<String, Object> varObj = new HashMap<>();

			for (String varName : vars.keySet()) { // for each variable
				Map<Object, Double> value = vars.get(varName);
				for (Object o : value.keySet()) {
					Double p = value.get(o);
					// only consider those variables whose probabilities are ones
					if (p == 1)
						varObj.put(varName, o);
				}
			}
			varLabel.put(stateId, varObj);
		}

		LabeledPCA labeledPCA = new LabeledPCA(pca.getPcaObj(), varLabel);

		// optionally set the reference back
		labeledPCA.setVpca(this);

		// set the same name on its unfolded LabeledPCA for traceability
		labeledPCA.setName(this.getProcessName());
		labeledPCA.setStateLabels(labels);

		// set variables for PRISM DTMC construction, possible bugs to include all
		// vPFSP's variables into the LPCA
		Set<VariableType> localVarSet=new HashSet<>();
		this.getVariables().forEach(v->{
			if(v.getNamespace().equalsIgnoreCase(this.getProcessName()))
				localVarSet.add(v);
		});
		labeledPCA.setVariables(localVarSet);

		Map<Integer, Set<Transition>> lts = pca.getLTS();
		logger.debug(lts);

		/**
		 * ERROR state is not included in the pca.eventstate[], bug!!! the following is
		 * duplicated with this.getStateMap().
		 */
		if (pca.getPcaObj().hasERROR()) {
			labels.put(-1, "ERROR");
		}

		for (int stateId : lts.keySet()) {

			if (stateId == -1)
				logger.debug("-1");

			ExtState extState = stateMap.get(stateId);
			logger.debug(extState.getExtStateLabel());

			labels.put(stateId, extState.getExtStateLabel());

			Map<String, Object> varObj = new HashMap<>();
			Map<String, Map<Object, Double>> var = extState.getLocVarValueMap();

			for (String varName : var.keySet()) {
				Map<Object, Double> values = var.get(varName);
				for (Object v : values.keySet()) {
					double p = values.get(v);
					if (p >= 1)
						varObj.put(varName, v);
				}
			}

			Set<Transition> outTrans = lts.get(stateId);
			if (outTrans == null || outTrans.size() == 0) {
				// treate it as the super final state
				varLabels.put(stateId, new HashMap<String, Object>());
			} else
				varLabels.put(stateId, varObj);
		}

		logger.debug(varLabels);
		labeledPCA.setVarLabel(varLabels);
		this.unfoledLPCA = labeledPCA;
		return labeledPCA;
	}

	/**
	 * Given a stateID, output that state's label that consists of local variable
	 * configurations
	 * 
	 * @param stateID
	 * @return
	 */
	public String getStateLabel(int stateID) {
		ExtState extState = this.getStateIDToExtStateMap().get(stateID);
		return extState.extStateLabel;
	}

	/**
	 * Return an extState from the localStateID
	 * 
	 * @param stateID
	 * @return
	 */
	public ExtState getExtStateByStateID(int stateID) {
		ExtState extState = this.getStateIDToExtStateMap().get(stateID);
		return extState;
	}

	/**
	 * Find the local PCA's stateID to the VPCA's extState mapping
	 * 
	 * @deprecated
	 * @return the unfoled PCA's state (Integer) to EPCA's ExtState map
	 */
	public Map<Integer, ExtState> getStateIDToExtStateMap3() {
		// if (this.stateMap != null && this.stateMap.size() > 0)
		// return this.stateMap;

		if (this.unfoldingPCA == null)
			this.unfoldPfsp();

		if (this.getExtLTS() == null)
			return null;

		stateMap = new HashMap<>();
		// super initial state is located 0
		ExtState sisExtState = this.findSuperStartExtState(extLTS);
		sisExtState.setStateID(0);
		stateMap.put(0, sisExtState);

		CompactState pcaObj = this.getUnfoldingPCA().getPcaObj();
		// CompactState pcaObj = this.getUnfoldedLabeledPCA().getPcaObj();

		/**
		 * the ERROR stateID is -1, with null VC, which is not included by
		 * pca.EventState[]
		 */
		if (pcaObj.hasERROR()) {
			// add Error ExtState into VPCA's extstates
			ExtState errExtState = new ExtState(this.processName, -1, null);
			this.getExtStates().put("ERROR", errExtState);
			// the absStateID = stateID for the ERROR state as it is unique
			errExtState.setStateID(-1);
			stateMap.put(-1, errExtState);
		}

		/**
		 * there is inconsistent error between pcaObj.maxStates and successor based
		 * travels
		 */
		for (int i = 0; i < pcaObj.maxStates; ++i) {
			EventState current = pcaObj.states[i];
			ExtState tgtState = null;
			while (current != null) {
				String label = current.getEventPCA().getLabel();
				String type = current.getEventPCA().getTypeString();
				double prob = current.getEventPCA().getProbability();
				int next = current.getNext();

				logger.debug(current + "\t" + label + "\t" + next);

				String extStateLabel = stateMap.get(i).getExtStateLabel();
				Set<ExtTransition> trans = extLTS.get(extStateLabel);
				/**
				 * for debugging
				 */
				if (trans == null)
					logger.debug(extStateLabel);
				if (trans != null)
					for (ExtTransition t : trans) {
						String tranLabel = t.getFSPLabel();
						String tranType = t.getExtAction().getActionType();
						double tranProb = t.getProbability();
						if (tranLabel.equals(label)
								&& (type.equals(tranType) && prob == tranProb)) {
							tgtState = t.getToState();
							// logger.info(tranLabel + " from state " + next + "
							// to
							// state " + tgtState);
							tgtState.setStateID(next);
							stateMap.put(next, tgtState);
							break;
						}
					}
				current = current.getList();
			} // while current!=null
		} // for each i
		return stateMap;
		// return csr.getStateMap();
	}

	/**
	 * Find the local PCA's stateID to the VPCA's extState mapping
	 * 
	 * @return the unfoled PCA's state (Integer) to EPCA's ExtState map
	 */
	public Map<Integer, ExtState> getStateIDToExtStateMap() {
		if (this.stateMap != null && this.stateMap.size() > 0)
			return this.stateMap;

		if (this.unfoldingPCA == null)
			this.unfoldPfsp();

		if (this.getExtLTS() == null)
			return null;

		stateMap = new HashMap<>();
		// super initial state is located 0
		ExtState sisExtState = this.findSuperStartExtState(extLTS);
		sisExtState.setStateID(0);
		stateMap.put(0, sisExtState);

		CompactState pcaObj = this.getUnfoldingPCA().getPcaObj();
		// CompactState pcaObj = this.getUnfoldedLabeledPCA().getPcaObj();

		/**
		 * the ERROR stateID is -1, with null VC, which is not included by
		 * pca.EventState[]
		 */
		if (pcaObj.hasERROR()) {
			// add Error ExtState into VPCA's extstates
			ExtState errExtState = new ExtState(this.processName, -1, null);
			this.getExtStates().put("ERROR", errExtState);
			// the absStateID = stateID for the ERROR state as it is unique
			errExtState.setStateID(-1);
			stateMap.put(-1, errExtState);
		}

		/**
		 * there is inconsistent error between pcaObj.maxStates and successor based
		 * travels
		 */

		Map<Integer, Set<Transition>> lts = this.getUnfoldingPCA().getLTS();
		Map<String, Set<ExtTransition>> extlts = this.extLTS;
		Map<String, ExtState> extStates = this.getExtStates();

		for (ExtTransition extTran : this.getExtTrans()) {
			logger.debug(extTran.getFromState() + "\t" + extTran.getFSPLabel() + "\t"
					+ extTran.getToState());
		}

		LinkedList<Integer> pcaStateQ = new LinkedList<Integer>();
		Set<Integer> visitPcaState = new HashSet<Integer>();
		LinkedList<ExtState> ltsStateQ = new LinkedList<ExtState>();
		// Set<String> visitExtState = new HashSet<String>();

		pcaStateQ.addFirst(0);
		ltsStateQ.addFirst(this.findSuperStartExtState(extLTS));

		while (!pcaStateQ.isEmpty()) {
			int currentPcaStateID = pcaStateQ.removeLast();
			Set<Transition> outTrans = lts.get(currentPcaStateID);
			ExtState currentExtState = ltsStateQ.removeLast();
			Set<ExtTransition> outExtTrans = extlts.get(currentExtState.getExtStateLabel());
			// mismatch transitions
			for (Transition t : outTrans) {
				String tLabel = t.getLabel();
				for (ExtTransition et : outExtTrans) {
					if (tLabel.equals(et.getFSPLabel())
							& t.getEvent().getProbability() == et.getProbability()) {
						String type = et.getExtAction().getActionType();
						String pcaTranType = t.getEvent().getTypeString();
						if (type.equals(pcaTranType)) {
							// match the output transition
							int targetPcaStateID = t.getTo();
							ExtState targetExtState = et.getToState();
							targetExtState.setStateID(targetPcaStateID);
							stateMap.put(targetPcaStateID, targetExtState);
							logger.debug(stateMap);
							if (!visitPcaState.contains(targetPcaStateID)) {
								visitPcaState.add(targetPcaStateID);
								pcaStateQ.addFirst(targetPcaStateID);
								ltsStateQ.addFirst(targetExtState);
							}
							break;
						}
					}
				}
			}
		}
		return stateMap;
	}

	/**
	 * Given an ExtState, reasoning about the target states
	 * 
	 * @param es
	 */
	public Vector<ExtState> getTargetExtStates(ExtState es) {
		/**
		 * identify the abstract state in the template PCA
		 */

		/**
		 * identify the outgoing transitions from the abstract state
		 */

		/**
		 * create extAction based on the vpfsp?
		 */

		/**
		 * reason about target states of the outgoing transitions
		 */

		/**
		 * return a set of target ExtStates
		 */
		return null;
	}

	// *************************** GETTER and SETTER **************************

	/**
	 * 
	 * @return the EPCA's local varaibles
	 */
	public Vector<VariableType> getLocVariables() {
		Vector<VariableType> locVars = new Vector<>();
		this.getVariables().forEach(v -> {
			if (v.getKind() == VariableType.LOCAL_KIND)
				locVars.add(v);
		});
		return locVars;
	}

	public CompactStateRef getTemptlatePca() {
		return temptlatePca;
	}

	public void setTemptlatePca(CompactStateRef temptlatePca) {
		this.temptlatePca = temptlatePca;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public Set<ExtAction> getExtActions() {
		return extActions;
	}

	public String[] getAlphabet() {
		return alphabet;
	}

	public void setAlphabet(String[] alphabet) {
		this.alphabet = alphabet;
	}

	public EventState[] getStates() {
		return states;
	}

	public void setStates(EventState[] states) {
		this.states = states;
	}

	/**
	 * Get a variableType by varName
	 * 
	 * @param varName
	 * @return
	 */
	public VariableType getVariable(String varName) {
		for (VariableType v : this.variables) {
			if (v.getVarName().equals(varName))
				return v;
		}
		return null;
	}

	public VariableType getVarByNamespaceAndName(String nsName) {
		for (VariableType v : this.variables) {
			String fullName = v.getNamespace() + "::" + v.getVarName();
			if (fullName.equals(nsName)) {
				return v;
			}
		}
		return null;
	}

	public Set<VariableType> getVariables() {
		return variables;
	}

	public void setVariables(Set<VariableType> variables) {
		this.variables = variables;
	}

	public Map<String, List<VariableInstance>> getInstnaceValues() {
		return instValues;
	}

	public void setInstnaceValues(Map<String, List<VariableInstance>> instnaceValues) {

		this.instValues = instnaceValues;
	}

	/**
	 * @return all interface variables' default value (with prob) combination
	 */
	public Map<String, Map<Object, Double>> getInterVarDefaultValues() {
		Map<String, Map<Object, Double>> interfaceValues = new TreeMap<>();
		for (VariableType interVar : this.getInterfaceVars()) {
			String varName = interVar.getVarName();
			interfaceValues.put(interVar.getNamespace() + "::" + varName, interVar.getProbDist());
		}
		return interfaceValues;
	}

	public List<VariableType> getInterfaceVars() {
		List<VariableType> interfaceVars = new ArrayList<>();
		for (VariableType v : this.variables) {
			if (v.getKind() == VariableType.INTERFACE_KIND) {
				interfaceVars.add(v);
			}
		}
		return interfaceVars;
	}

	public SortedMap<String, Set<ExtTransition>> getExtLTS() {
		return extLTS;
	}

	public Set<ExtTransition> getOutTransitions(String extStateLabel) {
		logger.debug("print extstates");
		this.extStates.forEach((a, b) -> {
			logger.debug(a);
		});
		ExtState st = this.extStates.get(extStateLabel);
		if (st == null) {
			logger.debug(this.getExtLTS());
			logger.fatal("no such an ExtState of " + extStateLabel);
			System.exit(-1);
		}
		return st.outTrans;
	}

	public Map<String, ExtState> getExtStates() {
		return extStates;
	}

	public void setExtStates(Map<String, ExtState> extStates) {
		this.extStates = extStates;
	}

	public Set<ExtTransition> getExtTrans() {
		if (this.extTrans.size() == 0)
			this.unfoldPfsp();
		return extTrans;
	}

	public void setExtTrans(Set<ExtTransition> extTrans) {
		this.extTrans = extTrans;
	}

	public Map<Integer, ExtState> getStateMap() {
		return stateMap;
	}

	public void setStateMap(Map<Integer, ExtState> stateMap) {
		this.stateMap = stateMap;
	}

	public String getUnfoldedFSP() {
		if (this.unfoldedFSP == null)
			this.unfoldPfsp();
		return unfoldedFSP;
	}

	public void setUnfoldedFSP(String unfoldedFSP) {
		this.unfoldedFSP = unfoldedFSP;
	}

	public String getAbstractFSP() {
		return abstractFSP;
	}

	public void setAbstractFSP(String abstractFSP) {
		this.abstractFSP = abstractFSP;
	}

	// ****************************** Testing **************************
	public void testExtStates() {
		SortedMap<String, Set<ExtTransition>> lts = this.extLTS;
		for (String stateLabel : lts.keySet()) {
			ExtState es = this.extStates.get(stateLabel);
			logger.info(stateLabel + "\t" + es.getExtStateLabel() + "\t" + es.isExplicit());
		}
	}

	public void testInstanceValues() {
		logger.info("\n testing variable instnace");
		for (String actKey : this.instValues.keySet()) {
			logger.info(actKey);
			List<VariableInstance> varList = this.instValues.get(actKey);
			for (VariableInstance vi : varList) {
				logger.info(vi.getInstValueDist());
			}
		}
	}

	public void testEPCA() {
		// show template pca
		this.testVariableTypes();
		this.testInstanceValues();
		this.testExtActions();
		// this.testExtStates();
	}

	private void testVariableTypes() {
		// show variables
		logger.info("\n show variables");
		for (VariableType vt : this.getVariables()) {
			logger.info(vt.getNamespace() + "::" + vt.getVarName() + " " + vt.getKind());
			logger.info(vt.getProbDist());
			logger.info(vt.getDomain());
			// logger.info(vt.getInitValue());
		}
	}

	private void testExtActions() {
		logger.info("\n show Action.......");
		for (ExtAction extAct : this.getExtActions()) {
			logger.info(extAct.getActionType() + "<" + extAct.getProbability() + ">"
					+ extAct.getActionLabel());
			logger.info(extAct.getGuardsList());
		}

	}

	@Override
	public String toString() {
		return super.toString();
	}
}
