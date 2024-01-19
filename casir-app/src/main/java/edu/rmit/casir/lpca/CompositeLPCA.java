package edu.rmit.casir.lpca;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import api.PFSPCompiler;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.pca.CompositePCA;
import edu.rmit.casir.util.DistributedRandomNumberGenerator;
import edu.rmit.casir.util.FileHandler;
import edu.rmit.casir.util.GeneralUtil;
import lts.ActionName;
import lts.CompactState;
import lts.LTSException;
import lts.Pair;
import lts.Transition;

/**
 * A CompositePCA consists of: 1) a set of local LabeledPCA; 2) a global
 * LabeledPCA (used to generate DTMC for verification); 3) two steps mapping:
 * <globalstateID, vector<localStateID>> -> <localStateID, ExtState[]>
 * 
 * @author terryzhou
 *
 */
public class CompositeLPCA {

	Vector<LabeledPCA> localLPCAVec;

	String name;

	// Global PCA
	CompositePCA gloPCA;

	// why not have this one as the global model?
	LabeledPCA gloLPCA;

	// global state to local states mapping
	// Map<Integer, Vector<Integer>> stateIDMap;

	// globalStateID to local ExtStates mapping
	// Map<Integer, ExtState[]> gloStateToLocExtStateMap;

	Logger logger = Logger.getLogger(CompositeLPCA.class);

	// **************************** Methods ********************************

	public CompositeLPCA(Vector<LabeledPCA> locLPCAs) {

		Vector<CompactStateRef> localPCAs = new Vector<CompactStateRef>();

		for (LabeledPCA p : locLPCAs) {
			localPCAs.add(new CompactStateRef(p.getPcaObj()));
		}

		this.localLPCAVec = locLPCAs;

		CompactStateRef globalPCA = this.createGlobalPCA();

		this.gloPCA = new CompositePCA(globalPCA, localPCAs);

		Map<Integer, Map<String, Object>> varLabels = getStateMap();

		this.gloLPCA = new LabeledPCA(globalPCA.getPcaObj(), varLabels);

		// aggregate the local labeledPCA's varaibles
		Set<VariableType> vars = new HashSet<>();
		this.getLocalLPCAVec().forEach(l -> {
			Set<VariableType> locVars = l.getVariables();
			if (locVars != null)
				vars.addAll(l.getVariables());
		});
		this.gloLPCA.setVariables(vars);

	}

	/**
	 * Set the global state's variable labels by union all local LPCA's local
	 * state's variable labels
	 * 
	 * @return
	 */
	private Map<Integer, Map<String, Object>> getStateMap() {
		Map<Integer, Map<String, Object>> varLabels = new HashMap<>();
		Map<Integer, Vector<Integer>> stateIDMap = this.gloPCA.getStateIDMap();
		// set for each global state about the local state variable label in union
		for (int gloStateId : stateIDMap.keySet()) {
			Map<String, Object> localVar = new HashMap<String, Object>();
			Vector<Integer> locStateIds = stateIDMap.get(gloStateId);
			for (int lPCAindex = 0; lPCAindex < locStateIds.size(); lPCAindex++) {
				LabeledPCA locLpca = this.getLocalLPCAVec().get(lPCAindex);
				int locStateId = locStateIds.get(lPCAindex);
				Map<String, Object> varValue = locLpca.getVarLabel().get(locStateId);
				localVar.putAll(varValue);
			}
			varLabels.put(gloStateId, localVar);
		}
		return varLabels;

	}

	/**
	 * 
	 * @return the global PCA by synchronizing the localLPCAVec
	 */
	private CompactStateRef createGlobalPCA() {
		Vector<CompactState> machines = new Vector<>();
		for (LabeledPCA l : this.localLPCAVec) {
			machines.add(l.getPcaObj());
		}
		PFSPCompiler compiler = new PFSPCompiler();
		CompactState global = compiler.compose("COMPOSITE", machines);
		return new CompactStateRef(global);
	}

	/**
	 * Output dot image
	 * 
	 * @param dotPath
	 */
	public void outputDotFile(String dotPath) {

		FileHandler.delFile(dotPath);
		try {
			FileHandler.appendPathFile(dotPath,
					new StringBuffer(new StringBuffer(this.getGraphvizDot())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Refering to the LabeledPCA's function
	 * 
	 * @return
	 */
	public String getGraphvizDot() {
		return this.gloLPCA.getGraphvizDot();
	}

	/**
	 * generate dot using varlabels
	 * 
	 * @deprecated
	 * @return
	 */
	public String getGraphvizDot2() {
		Map<Integer, Map<String, Object>> varLabels = this.getStateMap();
		// bug: cs has inconsistent stateIDs with globLocExtStateMap
		CompactStateRef cs = this.getGlobalPCA().getComposite();

		Map<Integer, Set<Transition>> lts = cs.getLTS();
		logger.debug(lts.size());
		int maxStates = (Integer) cs.getPrivateObject("maxStates");
		logger.debug(maxStates);

		Map<Integer, Pair<Set<Transition>, Boolean>> successors = (Map<Integer, Pair<Set<Transition>, Boolean>>) cs
				.getPrivateObject("successors");

		logger.debug(successors.size()); // 20
		logger.debug(cs.getLTS().size()); // 22

		String name = (String) cs.getPrivateObject("name");
		StringBuilder graphvizSpec = new StringBuilder();

		graphvizSpec.append("digraph " + name + " {\n" + "\t rankdir = LR;\n"
				+ "\t graph [fontname = \"monospace\", ranksep=\"0.2\", nodesep=\"0.1\"]; \n"
				+ "\t node [fontname = \"monospace\"]; \n"
				+ "\t edge [fontname = \"monospace\"]; \n");
		int nStates = maxStates;
		boolean hasFailureAction = false;
		graphvizSpec.append("\t 0 [label =\" 0 \n SuperInitialState\" "
				+ "shape = box, color = black, style = filled, fillcolor = orange]; \n");

		for (int i = 1; i < nStates; ++i) {
			Map<String, Object> varObj = varLabels.get(i);
			StringBuffer stateNodeStr = new StringBuffer("\t" + i + "[label=\"" + i + "\n");
			if (i == cs.getPcaObj().getEndseq()) {
				stateNodeStr.append("SuperFinalState \"");
				graphvizSpec.append(stateNodeStr
						+ " shape = box, color = black, style = filled, fillcolor = yellow]; \n");
				continue;
			}
			/**
			 * Adding local VCs that are from the extStates to the local states
			 */
			for (String varName : varObj.keySet()) {
				Object value = varObj.get(varName);
				stateNodeStr.append(varName + "=" + value.toString() + "\n");

				/**
				 * adding local vPCA's abstract stateID in the state label
				 */
				// stateNodeStr.append("stateID==" + s.getStateID() + " absStateID=="
				// + s.getAbsStateID() + "\n");
			}
			stateNodeStr.append("\"");

			// Not showing the detailed local variables by disabling stateNodeStr
			// stateNodeStr =new StringBuffer( "\t" + i + "[label=\""+i+"\"");

			graphvizSpec.append(stateNodeStr
					+ " shape = box, color = black, style = filled, fillcolor = cyan]; \n");
		} // for each global state

		Iterator localIterator1 = successors.entrySet().iterator();

		while (localIterator1.hasNext()) {
			Map.Entry stateSuccInfo = (Map.Entry) localIterator1.next();
			int currentstate = ((Integer) stateSuccInfo.getKey()).intValue();
			Set<Transition> currentGlobStateSuccessors = (Set) ((Pair) stateSuccInfo.getValue())
					.getFirst();

			int tranCount = 0;
			double sumProb = 0;
			for (Transition successor : currentGlobStateSuccessors) {
				int destinationState = successor.getTo();
				ActionName label = successor.getEventPCA();
				double prob = label.getProbability();
				tranCount++;
				if (tranCount == currentGlobStateSuccessors.size())
					prob = 1 - sumProb;
				prob = GeneralUtil.round(prob, 4);
				/**
				 * represent the transitions into readable format, TBD
				 */
				graphvizSpec.append("\t " + currentstate + " -> " + destinationState + "[label = \""
						+ label.getTypeString() + "<" + prob + ">" + label.getLabel()
						+ "\" ];\n  ");

				sumProb = sumProb + prob;

				if (label.isFailureAction()) {
					hasFailureAction = true;
				}
			}
		}
		if (hasFailureAction) {
			graphvizSpec.append(
					"\t -1 [label=\"-1 \n ERROR \" shape = box, color = black, style = filled, fillcolor = red]; \n");
		}
		graphvizSpec.append("}\n");
		return graphvizSpec.toString();
	}

	/**
	 * @param dtmcPath
	 * @throws IOException
	 */
	public void outputDTMCFile(String dtmcPath) throws IOException {
		FileHandler.delFile(dtmcPath);
		StringBuffer dtmc;
		try {
			dtmc = new StringBuffer(this.getDTMC());
			FileHandler.appendPathFile(dtmcPath, dtmc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Interpret this to a DTMC, replacing the VPCA's one, the challenge is the
	 * PRISM variables declaration;
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getDTMC() throws Exception {

		CompactStateRef csf = this.getGloLPCA();
		String name = (String) csf.getPrivateObject("name");
		Set<String> failureActions = csf.getPcaObj().getFailureActions();
		Set<String> inputActions = csf.getPcaObj().getInputActions();
		Set<String> internalFailureActions = csf.getPcaObj().getInternalFailureActions();
		int maxStates = (Integer) csf.getPrivateObject("maxStates");
		Set<String> outputFailureActions = csf.getPcaObj().getOutputFailureActions();

		if (inputActions.size() > 0) {
			throw new LTSException(
					"Process " + name + " cannot be analysed since it is not a closed PCA");
		}
		StringBuilder prismModel = new StringBuilder();
		StringBuilder comments = null;
		prismModel.append("dtmc\n\n");
		prismModel.append("module " + name.toLowerCase() + " \n\n");
		int nStates = (failureActions.size() == 0) ? maxStates - 1 : maxStates;
		prismModel.append("\ts: [0.." + nStates + "] init 0;\n");

		/**
		 * Add EPCAs' all local Variable as DTMC prism's variables The local variables
		 * are considered in the format of "Namespace_varName" e.g. "P_v2"
		 */

		prismModel.append("\tfinish: bool init false;\n");
		prismModel.append("\n");

		/**
		 * Add declariation of local variables
		 */
		Vector<LabeledPCA> locLpca = this.getLocalLPCAVec();
		Set<VariableType> vars = new HashSet<>();

		for (LabeledPCA lpca : locLpca) {
			Set<VariableType> locVars = lpca.getVariables();
			logger.debug(lpca.getName());
			if(lpca.getName().equals("WD_REC1"))
				logger.debug("debug");
			locVars.forEach(lv->{
				logger.debug(lv.getNamespace()+"::"+lv.getVarName());
			});
			if (locVars != null)
				vars.addAll(locVars);
		}
		for (VariableType vt : vars) {
			if (vt == null)
				continue;
			String firstElem, lastElem;
			if (vt.getKind() == VariableType.LOCAL_KIND) {
				String varLabel = vt.getNamespace().toLowerCase() + "_" + vt.getVarName();
				firstElem = vt.getDomain().first().toString();
				lastElem = vt.getDomain().last().toString();
				if (vt.type == VariableType.TYPE_BOOLEAN)
					prismModel.append("\t" + varLabel.toLowerCase() + ":bool;\n");
				else
					prismModel.append("\t" + varLabel.toLowerCase() + ":[" + firstElem + ".."
							+ lastElem + "];\n");
				// set the initial value if needed. The initial values are not needed for the
				// SISs
			}
		}

		for (String failureActionLabel : outputFailureActions) {
			prismModel.append("\t" + failureActionLabel + " : bool init false;\n");
		}

		for (String failureActionLabel : internalFailureActions) {
			prismModel.append("\t" + failureActionLabel + " : bool init false;\n");
			comments = new StringBuilder("\n \t// ");
			comments.append("");

		}

		prismModel.append("\n");

		boolean hasFailureAction = false;
		Object deadlockStates = new HashSet();

		Map<Integer, Set<Transition>> gloLTS = this.getGlobalPCA().getGlobalLTS();

		for (int currentGlobState : gloLTS.keySet()) { // for each global state
			if (currentGlobState == -1) { // ERROR state
				// change to the maxState as index
				currentGlobState = maxStates;
			}
			prismModel.append("\t \n");

			// Not having the labels over the source state.
			prismModel.append("\t[] s = " + currentGlobState);

			/**
			 * attach the local vars on the currentstate
			 */
			Map<Integer, Map<String, Object>> varLabels = this.getGloLPCA().getVarLabel();
			Map<String, Object> locVars = null;

			prismModel.append(" -> ");
			int succ = 0;
			comments = new StringBuilder("\n \t// ");

			Set<Transition> gloOutTrans = gloLTS.get(currentGlobState);

			if (gloOutTrans == null || gloOutTrans.size() == 0) {// make the absorbing state
				prismModel.append("1.0:(s'=" + currentGlobState + ")");
				if (currentGlobState == maxStates)
					comments.append("1.0:(error=" + currentGlobState + ");");
				else {
					comments.append("1.0:(s'=" + currentGlobState + ");");
					//making all absorbing states finished?
//					prismModel.append("&(finish'=true)");
				}

			} else {
				int tranCount = 0;
				double sumProb = 0;

				for (Transition globSuccessor : gloOutTrans) { // for each outgoing
																// transition
					++succ;
					ActionName globSuccessorLabel = globSuccessor.getEventPCA();

					double prob = globSuccessorLabel.getProbability();
					tranCount++;
					if (tranCount == gloOutTrans.size())
						prob = 1 - sumProb;
					prob = GeneralUtil.round(prob, 4);

					String label = globSuccessorLabel.getTypeString()
							+ globSuccessorLabel.getLabel();

					int globDestState = (globSuccessor.getTo() == -1) ? maxStates
							: globSuccessor.getTo();

					locVars = varLabels.get(currentGlobState);

					// int globDestState = globSuccessor.getTo();

					prismModel.append(GeneralUtil.round(prob, 4) + ":(s' = " + globDestState + ")");

					sumProb = sumProb + prob;

					/**
					 * Annotate the local pcas' variables on destination state
					 */
					if (!this.isEndTransition(globSuccessor)) {
						locVars = varLabels.get(globDestState);
						if (locVars != null)
							for (String varName : locVars.keySet()) {
								// logger.info("var name " + varName);
								String prismVarLabel = varName.toLowerCase().replace("::", "_");
								prismModel.append("&(" + prismVarLabel + "'=");
								Object value = locVars.get(varName);
								prismModel.append(value.toString() + ")");
							}
					} else {
						/**
						 * if globSuccessor is an end transition, then the destination state
						 * preserves the source state's labels
						 */
						locVars = varLabels.get(globSuccessor.getFrom());
						if (locVars != null)
							for (String varName : locVars.keySet()) {
								// logger.info("var name " + varName);
								String prismVarLabel = varName.toLowerCase().replace("::", "_");
								prismModel.append("&(" + prismVarLabel + "'=");
								Object value = locVars.get(varName);
								prismModel.append(value.toString() + ")");
							}
					}
					comments.append(GeneralUtil.round(prob, 4) + ":(" + label + " = "
							+ globDestState + ")");

					if (globSuccessorLabel.isFailureAction()) {
						prismModel.append("&(" + globSuccessorLabel.getLabel() + "'=true)");
						hasFailureAction = true;
					}

					/**
					 * Revisit the criteria for setting "finish=true": if the toState's local states
					 * are all END/Initial states, then finish=true
					 */
					else if ((globSuccessor.getTo() == 0) || ((globSuccessor.getTo() != -1)
							&& (gloLTS.get((globSuccessor.getTo())) == null
									|| gloLTS.get((globSuccessor.getTo())).size() == 0))) {
						prismModel.append("&(finish'=true)");
					}

					if (succ < gloLTS.get(currentGlobState).size()) {
						prismModel.append(" + ");
						comments.append(" + ");
					}
				}
			}
			prismModel.append("; " + comments.toString() + "\n");
		} // for each global state

		if (hasFailureAction)
			prismModel.append("\t[] s = " + maxStates + " -> 1:(s' = " + maxStates + ");\n");
		for (

		Integer deadlockState : (Set<Integer>) deadlockStates) {
			prismModel.append("\t[] s = " + deadlockState + " -> 1:(s' = " + deadlockState
					+ "); // deadlock state \n");
		}

		prismModel.append("\nendmodule\n\n");

		if (hasFailureAction) {
			prismModel.append("label \"error\" = ");
		} else {
			// cater for the specification that has "error" checking
			prismModel.append("label \"error\" = false;");

		}

		Iterator outputFailureActionsIterator = outputFailureActions.iterator();
		if (outputFailureActionsIterator.hasNext()) {
			prismModel.append((String) outputFailureActionsIterator.next());
		}
		while (outputFailureActionsIterator.hasNext()) {
			prismModel.append(" | " + ((String) outputFailureActionsIterator.next()));
		}
		for (String failureActionLabel : internalFailureActions) {
			prismModel.append(" | " + failureActionLabel);
		}
		if (hasFailureAction)
			prismModel.append("; \n");
		return prismModel.toString();
	}

	/**
	 * To judge if the transition is a local component's super end transition
	 * Currently, it is determined by the transition name; in the future it will be
	 * replaced by automation
	 * 
	 * @param t
	 * @return
	 */
	private boolean isEndTransition(Transition t) {
		String label = t.getEventPCA().getLabel();
		if (label.contains("end"))
			return true;
		return false;
	}

	/**
	 * Randomly explore the traces as execution instances
	 */
	public void getExtStateTraces(int bound) {
		CompositePCA gloPCA = this.getGlobalPCA();
		Map<Integer, Set<Transition>> lts = gloPCA.getGlobalLTS();
		logger.debug(lts);

		for (int i = 0; i < bound; i++) {

			int stateID = 0;
			logger.debug(
					"***********************************************************************************************");
			Set<Transition> outTrans = lts.get(stateID);
			StringBuffer traceStr = new StringBuffer();

			while (outTrans != null && outTrans.size() > 0) {
				logger.debug("------------- From state ----------------");
				logger.debug("to state: " + stateID + " predicates: ");
				traceStr.append(stateID);
				ExtState[] localEStates = this.getGlobStateToExtStatesMap().get(stateID);

				for (ExtState state : localEStates) {
					logger.debug(state.getLocVarValueMap());
				}

				outTrans = lts.get(stateID);

				DistributedRandomNumberGenerator rn = new DistributedRandomNumberGenerator();

				Vector<Transition> outTranVec = new Vector<Transition>();
				int j = 0;
				logger.debug("------------- outgoing transitions ----------------");
				for (Transition t : outTrans) {
					double p = t.getEventPCA().getProbability();
					outTranVec.add(t);
					logger.debug("tran's prob: " + p + " transition: " + t.getLabel());
					rn.addNumber(j, p);
					j++;
				}
				int selectedTranID = rn.getDistributedRandomNumber();
				Transition selTran = outTranVec.get(selectedTranID);

				traceStr.append("->" + selTran.getEventPCA().getTypeString()
						+ selTran.getEvent().getOriginalLabel() + "->");

				logger.debug("random index: " + selectedTranID + " selected transition: "
						+ selTran.getEventPCA().getTypeString() + "\t"
						+ selTran.getEventPCA().getLabel() + " to state: " + selTran.getTo());

				stateID = selTran.getTo();
				// traceStr.append("->"+stateID);
				localEStates = this.getGlobStateToExtStatesMap().get(stateID);
				logger.debug("------------- Target state ----------------");
				logger.debug("to state: " + stateID + " predicates: ");
				for (ExtState state : localEStates) {
					logger.debug(state.getLocVarValueMap());
				}
				outTrans = lts.get(stateID);

				if (outTrans == null || outTrans.size() == 0) {
					traceStr.append(stateID);
					logger.info(traceStr.toString());
					logger.debug("termination at state " + stateID + " with predicates: ");
					for (ExtState state : localEStates) {
						logger.debug(state.getLocVarValueMap());
					}

				}
			}
		}

	}

	/**
	 * Given a global state (Integer) to local PCAs' states (Vector<Integer>) Map,
	 * further mapping the local unfolded PCA states to the EPCA's extState map.
	 *
	 * @param globMap
	 *            the glob map
	 * @return the glob state to ext states map
	 */
	public Map<Integer, ExtState[]> getGlobStateToExtStatesMap() {

		Map<Integer, ExtState[]> gloStateToLocExtStateMap = new TreeMap<>();
		CompositePCA compositePCA = this.getGlobalPCA();
		Map<Integer, Vector<Integer>> globMap = compositePCA.buildGloLocStateMap();
		logger.debug(globMap);
		/**
		 * does it include ERROR state?
		 */
		for (int globStateID : globMap.keySet()) {
			Vector<Integer> localStateVec = globMap.get(globStateID);
			ExtState[] localExtStateArr = new ExtState[localStateVec.size()];

			/**
			 * obatain the mappig from the CompositePCA
			 */
			CompositePCA comPCA = this.getGlobalPCA();

			Vector<Integer> localStateIDVec = comPCA.getStateIDMap().get(globStateID); // <1:[1,2]>

			for (int i = 0; i < localStateIDVec.size(); i++) {
				LabeledPCA lpca = this.localLPCAVec.get(i);

				// get ExtStates from the the local lPCA's vPCA, this is to be reviewed, may not
				// need vpca in labeledPCA
				VPCA vpca = lpca.getVpca();

				ExtState extState = vpca.getStateMap().get(localStateIDVec.get(i));

				localExtStateArr[i] = extState;
			}
			gloStateToLocExtStateMap.put(globStateID, localExtStateArr);
		}
		return gloStateToLocExtStateMap;
	}

	/**
	 * for testing...
	 */
	public void print() {
		logger.info("PRINTING CompositeLPCA");
		// logger.info("StateIDMap: " + this.stateIDMap);
		logger.info(this.getGlobalPCA().convertPCAToDTMC());
		logger.info("Showing <GlobalStateID, ExtState[]>");
		Map<Integer, Vector<Integer>> stateIDMap = this.getGlobalPCA().getStateIDMap();
		for (int gloStateID : this.getGlobStateToExtStatesMap().keySet()) {

			logger.info(gloStateID + "\t" + stateIDMap.get(gloStateID));

			Vector<Integer> localStateIDVec = stateIDMap.get(gloStateID);

			for (int lpcaIndex = 0; lpcaIndex < localStateIDVec.size(); lpcaIndex++) {
				LabeledPCA lpca = this.localLPCAVec.get(lpcaIndex);
				int localStateID = localStateIDVec.get(lpcaIndex);
				// ExtState es = lpca.extStates.get(localStateID);
				// logger.info(
				// localStateID + "\t" + es.getAbsStateID() + "\t" + es.getExtStateLabel());
				Map<String, Object> value = lpca.getVarLabel().get(localStateID);
				logger.info(localStateID + "\t" + value);
			}
			// for (ExtState es : this.getGlobStateToExtStatesMap().get(gloStateID)) {
			// logger.info(es.getAbsStateID() + "\t" + es.getExtStateLabel());
			// }
		}
		logger.info(this.getGraphvizDot());
	}

	/**
	 * Interpret this to a DTMC, replacing the VPCA's one, the challenge is the
	 * PRISM variables declaration;
	 * 
	 * @deprecated
	 * @return
	 * @throws Exception
	 */
	public String getDTMC_OLD() throws Exception {

		CompactStateRef csf = this.getGloLPCA();
		String name = (String) csf.getPrivateObject("name");
		Set<String> failureActions = csf.getPcaObj().getFailureActions();
		Set<String> inputActions = csf.getPcaObj().getInputActions();
		Set<String> internalFailureActions = csf.getPcaObj().getInternalFailureActions();
		int maxStates = (Integer) csf.getPrivateObject("maxStates");
		Set<String> outputFailureActions = csf.getPcaObj().getOutputFailureActions();

		if (inputActions.size() > 0) {
			throw new LTSException(
					"Process " + name + " cannot be analysed since it is not a closed PCA");
		}
		StringBuilder prismModel = new StringBuilder();
		prismModel.append("dtmc\n\n");
		prismModel.append("module " + name.toLowerCase() + " \n\n");
		int nStates = (failureActions.size() == 0) ? maxStates - 1 : maxStates;
		prismModel.append("\ts: [0.." + nStates + "] init 0;\n");

		/**
		 * Add EPCAs' all local Variable as DTMC prism's variables The local variables
		 * are considered in the format of "Namespace_varName" e.g. "P_v2"
		 */

		prismModel.append("\tfinish: bool init false;\n");
		prismModel.append("\n");

		/**
		 * Add declariation of local variables
		 */
		Vector<LabeledPCA> locLpca = this.getLocalLPCAVec();
		Set<VariableType> vars = new HashSet<>();

		for (LabeledPCA lpca : locLpca) {
			Set<VariableType> locVars = lpca.getVariables();
			if (locVars != null)
				vars.addAll(locVars);
		}

		for (VariableType vt : vars) {
			if (vt == null)
				continue;
			String firstElem, lastElem;
			if (vt.getKind() == VariableType.LOCAL_KIND) {
				String varLabel = vt.getNamespace().toLowerCase() + "_" + vt.getVarName();
				firstElem = vt.getDomain().first().toString();
				lastElem = vt.getDomain().last().toString();
				if (vt.type == VariableType.TYPE_BOOLEAN)
					prismModel.append("\t" + varLabel.toLowerCase() + ":bool;\n");
				else
					prismModel.append("\t" + varLabel.toLowerCase() + ":[" + firstElem + ".."
							+ lastElem + "];\n");
				// set the initial value if needed. The initial values are not needed for the
				// SISs
			}
		}

		for (String failureActionLabel : outputFailureActions) {
			prismModel.append("\t" + failureActionLabel + " : bool init false;\n");
		}

		for (String failureActionLabel : internalFailureActions) {
			prismModel.append("\t" + failureActionLabel + " : bool init false;\n");
		}

		prismModel.append("\n");

		boolean hasFailureAction = false;
		Object deadlockStates = new HashSet();

		/**
		 * Exploring from CompactStateRef Object's states that is different from
		 * this.convertEPCAToDot()
		 */
		Map<Integer, Pair<Set<Transition>, Boolean>> globSuccessors = (Map<Integer, Pair<Set<Transition>, Boolean>>) csf
				.getPrivateObject("successors");
		logger.debug(globSuccessors.size());
		// logger.debug(this.getComposedPCA().getLTS().size());

		Iterator globStateIterator2 = globSuccessors.entrySet().iterator();
		int currentGlobState;

		while (globStateIterator2.hasNext()) {
			// for each global state
			Map.Entry globStateSuccInfo = (Map.Entry) globStateIterator2.next();
			currentGlobState = ((Integer) globStateSuccInfo.getKey()).intValue();
			Set<Transition> currentGlobStateSuccessors = (Set) ((Pair) globStateSuccInfo.getValue())
					.getFirst();

			double totalProbability = 0.0D;
			for (Transition globStatSuccessor : currentGlobStateSuccessors) {
				totalProbability += globStatSuccessor.getEventPCA().getProbability();
			}
			prismModel.append("\t[] s = " + currentGlobState);

			/**
			 * attach the local vars on the currentstate
			 */
			Map<Integer, Map<String, Object>> varLabels = this.getGloLPCA().getVarLabel();
			Map<String, Object> locVars = varLabels.get(currentGlobState);

			if (locVars != null)
				// for END state that has no local variables,just leave it
				for (String varName : locVars.keySet()) {
					// logger.info("var name " + varName);
					String prismVarLabel = varName.toLowerCase().replace("::", "_");
					prismModel.append("&(" + prismVarLabel + "=");
					Object value = locVars.get(varName);
					prismModel.append(value.toString() + ")");
					// logger.info("value " + v.toString());
				}

			prismModel.append(" -> ");
			int succ = 0;
			StringBuilder comments = new StringBuilder("\n \t// ");

			for (Transition globSuccessor : currentGlobStateSuccessors) {
				++succ;
				ActionName globSuccessorLabel = globSuccessor.getEventPCA();
				globSuccessorLabel
						.setProbability(globSuccessorLabel.getProbability() / totalProbability);
				int globDestState = (globSuccessor.getTo() == -1) ? maxStates
						: globSuccessor.getTo();
				// int globDestState = globSuccessor.getTo();
				if (globDestState == maxStates)
					logger.debug("ERROR state");
				prismModel.append(GeneralUtil.round(globSuccessor.getEventPCA().getProbability(), 4)
						+ ":(s' = " + globDestState + ")");
				/**
				 * Annotate the local pcas' variables on destination state
				 */
				if (globDestState == maxStates)
					locVars = null;
				else
					locVars = varLabels.get(globDestState);

				if (locVars == null) {
					logger.debug(globDestState);
					logger.debug(maxStates);
					// throw new Exception("the variable's value exceeds the domain boundary");
				} else {
					locVars = varLabels.get(globDestState);
					if (locVars == null)
						continue;
					for (String varName : locVars.keySet()) {
						// logger.info("var name " + varName);
						String prismVarLabel = varName.toLowerCase().replace("::", "_");
						prismModel.append("&(" + prismVarLabel + "'=");
						Object value = locVars.get(varName);
						prismModel.append(value.toString() + ")");
					}
				}

				comments.append(GeneralUtil.round(globSuccessor.getEventPCA().getProbability(), 4)
						+ ":(" + globSuccessor.getEventPCA().getLabel() + " = " + globDestState
						+ ")");

				/**
				 * update the variables' values based on the toState's local values; if the
				 * transition is an internal transition from a process P,
				 */

				if (globSuccessorLabel.isFailureAction()) {
					prismModel.append("&(" + globSuccessorLabel.getLabel() + "'=true)");
					hasFailureAction = true;
				}

				// below to be reviewed.
				else if ((globSuccessor.getTo() == 0) || ((globSuccessor.getTo() != -1)
						&& (globSuccessors.get(Integer.valueOf(globSuccessor.getTo())) == null))) {
					prismModel.append("&(finish'=true)");
				}

				/**
				 * add another condition to determine "finish=true" i.e. if the target abstract
				 * state is 0 then it is set true;
				 */

				if (succ < currentGlobStateSuccessors.size()) {
					prismModel.append(" + ");
					comments.append(" + ");
				}

				if ((globSuccessors.get(Integer.valueOf(globSuccessor.getTo())) != null)
						|| (globSuccessor.getTo() == -1))
					continue;
				((Set) deadlockStates).add(Integer.valueOf(globSuccessor.getTo()));
			}
			prismModel.append("; " + comments.toString() + "\n");
		}

		if (hasFailureAction)
			prismModel.append("\t[] s = " + maxStates + " -> 1:(s' = " + maxStates + ");\n");
		for (Integer deadlockState : (Set<Integer>) deadlockStates) {
			prismModel.append("\t[] s = " + deadlockState + " -> 1:(s' = " + deadlockState
					+ "); // deadlock state \n");
		}

		prismModel.append("\nendmodule\n\n");
		if (hasFailureAction) {
			prismModel.append("label \"failure\" = ");
		}

		Iterator outputFailureActionsIterator = outputFailureActions.iterator();
		if (outputFailureActionsIterator.hasNext()) {
			prismModel.append((String) outputFailureActionsIterator.next());
		}

		while (outputFailureActionsIterator.hasNext()) {
			prismModel.append(" | " + ((String) outputFailureActionsIterator.next()));
		}

		for (String failureActionLabel : internalFailureActions) {
			prismModel.append(" | " + failureActionLabel);
		}

		if (hasFailureAction)
			prismModel.append("; \n");

		return prismModel.toString();
	}

	/**
	 * output multiple MRM files
	 * 
	 * @param mrmDirPath
	 * @throws IOException
	 */
	public void outputMRMFile(String mrmDirPath, Map<String, Map<String, Double>> multiCostMap)
			throws IOException {
		Map<String, String> mrmMap = this.getMultiMRM(multiCostMap);
		for (String resource : mrmMap.keySet()) {
			String mrmPath = mrmDirPath + "_" + resource + "_rewards.pm";
			FileHandler.delFile(mrmPath);
			FileHandler.appendPathFile(mrmPath, new StringBuffer(mrmMap.get(resource)));
		}
	}

	/**
	 * Get multiple resource based MRM for the cost estimation
	 * 
	 * @param multiCostMap
	 * @return
	 */
	public Map<String, String> getMultiMRM(Map<String, Map<String, Double>> multiCostMap) {
		Map<String, String> mrmSet = new HashMap<>();
		for (String resource : multiCostMap.keySet()) {
			String mrmStr = null;
			try {
				mrmStr = this.getMRM(resource, multiCostMap.get(resource));
			} catch (Exception e) {
				e.printStackTrace();
			}
			mrmSet.put(resource, mrmStr);
		}
		return mrmSet;
	}

	/**
	 * Generate the MRM model for rewards checking
	 * 
	 * @param costMap
	 * @return
	 * @throws Exception
	 */
	private String getMRM(String rewardsname, Map<String, Double> costMap) throws Exception {

		CompactStateRef csf = this.getGloLPCA();
		String name = (String) csf.getPrivateObject("name");
		Set<String> failureActions = csf.getPcaObj().getFailureActions();
		Set<String> inputActions = csf.getPcaObj().getInputActions();
		Set<String> internalFailureActions = csf.getPcaObj().getInternalFailureActions();
		int maxStates = (Integer) csf.getPrivateObject("maxStates");
		Set<String> outputFailureActions = csf.getPcaObj().getOutputFailureActions();

		int costTransSize = 0;

		Map<Integer, Set<Transition>> gloLTS = this.getGlobalPCA().getGlobalLTS();

		for (int i : gloLTS.keySet()) {
			Set<Transition> outTrans = gloLTS.get(i);
			for (Transition t : outTrans) {
				if (costMap.keySet().contains(t.getEventPCA().getLabel()))
					costTransSize++;
			}
		}

		StringBuilder rewardsStr = new StringBuilder();
		rewardsStr.append("rewards \"" + rewardsname + "\"" + "\n");

		if (inputActions.size() > 0) {
			throw new LTSException(
					"Process " + name + " cannot be analysed since it is not a closed PCA");
		}
		StringBuilder prismModel = new StringBuilder();
		StringBuilder comments = null;
		prismModel.append("dtmc\n\n");
		prismModel.append("module " + name.toLowerCase() + " \n\n");
		int nStates = (failureActions.size() == 0) ? maxStates - 1 : maxStates;
		int totalState = nStates + costTransSize;
		prismModel.append("\ts: [0.." + totalState + "] init 0;\n");

		/**
		 * Add EPCAs' all local Variable as DTMC prism's variables The local variables
		 * are considered in the format of "Namespace_varName" e.g. "P_v2"
		 */
		prismModel.append("\tfinish: bool init false;\n");
		prismModel.append("\n");

		/**
		 * Add declariation of local variables
		 */
		Vector<LabeledPCA> locLpca = this.getLocalLPCAVec();
		Set<VariableType> vars = new HashSet<>();

		for (LabeledPCA lpca : locLpca) {
			Set<VariableType> locVars = lpca.getVariables();
			if (locVars != null)
				vars.addAll(locVars);
		}
		for (VariableType vt : vars) {
			if (vt == null)
				continue;
			String firstElem, lastElem;
			if (vt.getKind() == VariableType.LOCAL_KIND) {
				String varLabel = vt.getNamespace().toLowerCase() + "_" + vt.getVarName();
				firstElem = vt.getDomain().first().toString();
				lastElem = vt.getDomain().last().toString();
				if (vt.type == VariableType.TYPE_BOOLEAN)
					prismModel.append("\t" + varLabel.toLowerCase() + ":bool;\n");
				else
					prismModel.append("\t" + varLabel.toLowerCase() + ":[" + firstElem + ".."
							+ lastElem + "];\n");
				// set the initial value if needed. The initial values are not needed for the
				// SISs
			}
		}

		for (String failureActionLabel : outputFailureActions) {
			prismModel.append("\t" + failureActionLabel + " : bool init false;\n");
		}

		for (String failureActionLabel : internalFailureActions) {
			prismModel.append("\t" + failureActionLabel + " : bool init false;\n");
			comments = new StringBuilder("\n \t// ");
			comments.append("");
		}

		prismModel.append("\n");

		boolean hasFailureAction = false;
		Object deadlockStates = new HashSet();

		// Map<Integer, Set<Transition>> gloLTS = this.getGlobalPCA().getGlobalLTS();

		int addIndex = 1;

		for (int currentGlobState : gloLTS.keySet()) { // for each global state
			StringBuilder costTranStr = new StringBuilder();

			if (currentGlobState == -1) { // ERROR state
				// change to the maxState as index
				currentGlobState = maxStates;
			}

			prismModel.append("\t \n");
			prismModel.append("\t[] s = " + currentGlobState);
			/**
			 * attach the local vars on the currentstate
			 */
			Map<Integer, Map<String, Object>> varLabels = this.getGloLPCA().getVarLabel();
			Map<String, Object> locVars = null;

			prismModel.append(" -> ");
			int succ = 0;
			comments = new StringBuilder("\n \t// ");

			Set<Transition> gloOutTrans = gloLTS.get(currentGlobState);

			if (gloOutTrans == null || gloOutTrans.size() == 0) {// make the absorbing state
				prismModel.append("1.0:(s'=" + currentGlobState + ")");
				if (currentGlobState == maxStates)
					comments.append("1.0:(error=" + currentGlobState + ");");
				else
					comments.append("1.0:(s'=" + currentGlobState + ");");

			} else {

				int tranCount = 0;
				double sumProb = 0;

				// for each outgoing transition
				for (Transition globSuccessor : gloOutTrans) {

					++succ;
					ActionName globSuccessorLabel = globSuccessor.getEventPCA();

					int globBefDestState = (globSuccessor.getTo() == -1) ? maxStates
							: globSuccessor.getTo();

					int interimDestState = globBefDestState;

					// String label = globSuccessor.getLabel();
					String label = globSuccessorLabel.getTypeString()
							+ globSuccessorLabel.getLabel();

					Double cost = costMap.get(label);

					double prob = globSuccessorLabel.getProbability();
					tranCount++;
					if (tranCount == gloOutTrans.size())
						prob = 1 - sumProb;

					prob = GeneralUtil.round(prob, 4);

					if (cost != null) {
						interimDestState = nStates + addIndex;
						// adding an cost transition
						costTranStr.append("\t[] s= " + interimDestState + " -> 1.0:(s' = "
								+ globBefDestState + ");\n");
						rewardsStr.append("\t s=" + interimDestState + ":" + cost + ";" + "\t // "
								+ label + "\n");

						prismModel.append(
								GeneralUtil.round(prob, 4) + ":(s' = " + interimDestState + ")");
						addIndex++;
					} else
						prismModel.append(
								GeneralUtil.round(prob, 4) + ":(s' = " + globBefDestState + ")");

					sumProb = sumProb + prob;

					/**
					 * Annotate the local pcas' variables on destination state
					 */
					locVars = varLabels.get(globBefDestState);
					if (locVars != null)
						for (String varName : locVars.keySet()) {
							// logger.info("var name " + varName);
							String prismVarLabel = varName.toLowerCase().replace("::", "_");
							prismModel.append("&(" + prismVarLabel + "'=");
							Object value = locVars.get(varName);
							prismModel.append(value.toString() + ")");
						}

					comments.append(
							GeneralUtil.round(globSuccessor.getEventPCA().getProbability(), 4)
									+ ":(" + globSuccessor.getEventPCA().getLabel() + " = "
									+ globBefDestState + ")");

					if (globSuccessorLabel.isFailureAction()) {
						prismModel.append("&(" + globSuccessorLabel.getLabel() + "'=true)");
						hasFailureAction = true;
					}

					else if ((globSuccessor.getTo() == 0) || ((globSuccessor.getTo() != -1)
							&& (gloLTS.get((globSuccessor.getTo())) == null
									|| gloLTS.get((globSuccessor.getTo())).size() == 0))) {
						prismModel.append("&(finish'=true)");
					}

					if (succ < gloLTS.get(currentGlobState).size()) {
						prismModel.append(" + ");
						comments.append(" + ");
					}
				} // for each outgoing transition
			} // else

			prismModel.append("; " + comments.toString() + "\n");

			prismModel.append(costTranStr.toString() + "\n");
		} // for each global state

		rewardsStr.append("endrewards \n");

		if (hasFailureAction)
			prismModel.append("\t[] s = " + maxStates + " -> 1:(s' = " + maxStates + ");\n");
		for (Integer deadlockState : (Set<Integer>) deadlockStates) {
			prismModel.append("\t[] s = " + deadlockState + " -> 1:(s' = " + deadlockState
					+ "); // deadlock state \n");
		}

		prismModel.append("\nendmodule\n\n");
		if (hasFailureAction) {
			prismModel.append("label \"error\" = ");
		} else {
			// in case the specs with "error" checking
			prismModel.append("label \"error\" = false;");

		}
		Iterator outputFailureActionsIterator = outputFailureActions.iterator();
		if (outputFailureActionsIterator.hasNext()) {
			prismModel.append((String) outputFailureActionsIterator.next());
		}
		while (outputFailureActionsIterator.hasNext()) {
			prismModel.append(" | " + ((String) outputFailureActionsIterator.next()));
		}
		for (String failureActionLabel : internalFailureActions) {
			prismModel.append(" | " + failureActionLabel);
		}
		if (hasFailureAction)
			prismModel.append("; \n");

		prismModel.append("\n" + rewardsStr.toString() + "\n");

		return prismModel.toString();
	}

	// ************************ GETTER and SETTER ************************

	public void setGlobalPCA(CompositePCA globalPCA) {
		this.gloPCA = globalPCA;
	}

	public CompositePCA getGlobalPCA() {
		return this.gloPCA;
	}

	public Vector<LabeledPCA> getLocalLPCAVec() {
		return localLPCAVec;
	}

	public void setLocalLPCAVec(Vector<LabeledPCA> localLPCAVec) {
		this.localLPCAVec = localLPCAVec;
	}

	public LabeledPCA getGloLPCA() {
		return gloLPCA;
	}

	public void setGloLPCA(LabeledPCA gloLPCA) {
		this.gloLPCA = gloLPCA;
	}

}
