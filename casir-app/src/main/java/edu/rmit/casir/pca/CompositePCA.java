package edu.rmit.casir.pca;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import api.APITest;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.util.DistributedRandomNumberGenerator;
import edu.rmit.casir.util.FileHandler;
import edu.rmit.casir.util.GeneralUtil;
import lts.ActionName;
import lts.LTSException;
import lts.Pair;
import lts.Transition;

/**
 * A composed PCA from a set of local PCA
 * 
 * @author terryzhou
 *
 */
public class CompositePCA {

	public static Logger logger = Logger.getLogger(CompositePCA.class);

	// the global PCA
	CompactStateRef composite;

	// a set of local PCAs
	Vector<CompactStateRef> locPCAs;

	int factor = 0;

	// global state to local states mapping
	Map<Integer, Vector<Integer>> stateIDMap;

	// ******************** Method ***********************

	public CompositePCA(CompactStateRef composite, Vector<CompactStateRef> locPcas) {
		this.composite = composite;
		this.locPCAs = locPcas;
	}

	/**
	 * get the global LTS of the composite PCA
	 * 
	 * @return
	 */
	public Map<Integer, Set<Transition>> getGlobalLTS() {
		return this.getComposite().getLTS();
	}

	/**
	 * Given sample is Map<"component index", Map<localStateID,
	 * Vector<"variable=value">>
	 * 
	 * @param bound
	 */
	public void virtualRun(int bound, Map<Integer, Map<Integer, Vector<String>>> sample,
			Map<String, Double> timeCost, Map<String, Double> dollarCost, String rec1RawDataPath,
			Set<String> startActions, Set<Integer> handlerLocCompletionStates,
			Set<Integer> icGloStates) {
		// CompositePCA gloPCA = this.getGlobalPCA();
		Map<Integer, Set<Transition>> lts = this.getGlobalLTS();

		double totalTime = 0;
		double totalDollar = 0;
		double totalRecTime = 0;
		double totalRecDollar = 0;
		double totalHandlerTime = 0;
		double totalHandlerDollar = 0;
		int recoveryNum = 0;
		double inconsistentNum = 0;
		double errors = 0;

		StringBuffer outputBuff = new StringBuffer();

		for (int i = 0; i < bound; i++) { // for each instance

			int stateID = 0;
			logger.debug(
					"***********************************************************************************************");
			Set<Transition> outTrans = lts.get(stateID);
			StringBuffer traceStr = new StringBuffer();
			double traceTime = 0;
			double traceDollar = 0;
			double recTime = 0;
			double handlerTime = 0;
			double handlerDollar = 0;
			double recDollar = 0;
			boolean handlerRange = false;
			boolean recovery = false;

			while (outTrans != null && outTrans.size() > 0) {
				logger.debug("------------- From state ----------------");
				logger.debug("to state: " + stateID + " predicates: ");
				traceStr.append(stateID);

				// add the local labels
				Vector<Integer> localStateIDVec = this.getStateIDMap().get(stateID);
				for (int index = 0; index < localStateIDVec.size(); index++) {
					int localID = localStateIDVec.get(index);
					if (index == 0) {
					}
					Map<Integer, Vector<String>> localVo = sample.get(index);
					if (localVo == null)
						continue;
					Vector<String> valObjs = localVo.get(localID);
					StringBuffer predicateStr = new StringBuffer();
					for (String field : valObjs) {
						predicateStr.append(field + " ");
					}
					traceStr.append(":( " + predicateStr + ")");
				}

				// select outgoing transition
				outTrans = lts.get(stateID);
				Transition selTran = this.getRandomTransition(outTrans);

				// simulate the execution time ms for only WD's transitions?
				// try {
				// Thread.sleep(20);
				// } catch (InterruptedException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

				// get the transition's costs
				String tranLabel = selTran.getLabel();

				// find next state
				stateID = selTran.getTo();

				// Recovery starts
				// if (this.isRecoverStarted(localWDID, recStartLocState)) {
				if (this.isRecoverStarted(tranLabel, startActions)) {
					handlerRange = true;
					recovery = true;
					recoveryNum = recoveryNum + 1;
					recTime = 0;
					recDollar = 0;
				}

				Double tCost = timeCost.get(tranLabel);
				if (tCost == null)
					tCost = 0.0;
				Double dCost = dollarCost.get(tranLabel);
				if (dCost == null)
					dCost = 0.0;
				traceTime = traceTime + tCost;
				traceDollar = traceDollar + dCost;

				if (recovery) {
					recTime = recTime + tCost;
					recDollar = recDollar + dCost;
				}

				if (handlerRange) {
					localStateIDVec = this.getStateIDMap().get(stateID);
					Integer localStateID = localStateIDVec.get(0);// get WD's local state

					if (this.isRecoveryComplete(localStateID, handlerLocCompletionStates)) {
						// bug for reaching non-successful recovery
						handlerRange = false;
						handlerTime = recTime;
						handlerDollar = recDollar;
					}
				}
				traceStr.append("->" + selTran.getEventPCA().getTypeString()
						+ selTran.getEvent().getOriginalLabel() + "[" + tCost + "," + dCost + "]"
						+ "->");

				// if consistent end, bugs here
				if (!this.isConsistentEnd(stateID, icGloStates)) {
					inconsistentNum++;
				}

				// int previousStateID = selTran.getFrom();

				// find next outgoing transitions
				outTrans = lts.get(stateID);

				// reaching the termination state
				if (outTrans == null || outTrans.size() == 0) {
					traceStr.append(stateID);
					logger.debug("termination at state " + stateID + " with predicates: ");

					localStateIDVec = this.getStateIDMap().get(stateID);

					StringBuffer predicateStr = new StringBuffer();

					for (int k = 0; k < localStateIDVec.size(); k++) {
						int localID = localStateIDVec.get(k);
						Map<Integer, Vector<String>> localVo = sample.get(k);
						if (localVo == null)
							continue;
						Vector<String> valObjs = localVo.get(localID);
						for (String field : valObjs) {
							predicateStr.append(field + " ");
						}
						traceStr.append(":( " + predicateStr + ")");
						if (predicateStr.toString().trim().equals("ERROR"))
							errors = errors + 1;
					} // for each component

					// traceStr.append(":( " + predicateStr + ")");

					logger.debug(traceStr.toString() + "\t total time cost:" + traceTime
							+ "\t total dollar cost:" + traceDollar);

					outputBuff.append(traceStr.toString() + "\n" + "Total Time Cost:" + traceTime
							+ "\t Total Dollar Cost:" + traceDollar + "\t Recovery Time Cost:"
							+ recTime + "\t Rec Dollar cost:" + recDollar + "\t Handler Time cost:"
							+ handlerTime + "\t Handler Dollar cost:" + handlerDollar + "\n \n");

					totalTime = totalTime + traceTime;
					totalDollar = totalDollar + traceDollar;

					totalRecTime = totalRecTime + recTime;
					totalRecDollar = totalRecDollar + recDollar;

					totalHandlerTime = totalHandlerTime + handlerTime;
					totalHandlerDollar = totalHandlerDollar + handlerDollar;

				} // termination state
			}
		} // for each instance

		logger.info("AVE Time Cost:" + totalTime / bound);
		logger.info("AVE Dollar Cost:" + totalDollar / bound);
		logger.info("The Recovery occured instances: " + recoveryNum);

		outputBuff.append("\n" + "Inconsistent instance before recovery:\t" + recoveryNum + "\n");
		outputBuff.append("Inconsistent instance after recovery:\t" + inconsistentNum + "\n");

		outputBuff.append("\n" + "AVE Total Time Cost:\t" + totalTime / bound + "\n");
		outputBuff.append("AVE Total Dollar Cost:\t" + totalDollar / bound + "\n");

		double recRateDollar = 0;
		double recRateTime = 0;
		double handRateDollar = 0;
		double handRateTime = 0;

		if (recoveryNum > 0) {
			recRateDollar = totalRecDollar / recoveryNum;
			recRateTime = totalRecTime / recoveryNum;
			handRateDollar = totalHandlerDollar / recoveryNum;
			handRateTime = totalHandlerTime / recoveryNum;
		}
		outputBuff.append("\n" + "AVE Recovery Time Cost:\t" + recRateTime + "\n");
		outputBuff.append("AVE Recovery Dollar Cost:\t" + recRateDollar + "\n");

		outputBuff.append("\n" + "AVE Handler Time Cost\t" + handRateTime + "\n");
		outputBuff.append("AVE Handler Dollar Cost:\t" + handRateDollar + "\n");

		outputBuff.append("\n" + "Reliability:\t" + (1 - errors / bound) + "\n");
		outputBuff.append("Inconsistnecy Prob:\t" + inconsistentNum / bound + "\n");

		try {
			FileHandler.delFile(rec1RawDataPath);
			FileHandler.appendPathFile(rec1RawDataPath, outputBuff);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * For experiment, to be removed, bugs here, should be state determination,
	 * However, it is chosen because of the sequence issue from a state where normal
	 * traces may occur, thus, change the second parameter to a set of actions.
	 */
	private boolean isRecoverStarted(String tranLabel, Set<String> startActions) {
		boolean recovery = false;
		if (startActions.contains(tranLabel)) {
			recovery = true;
		}
		return recovery;
	}

	/**
	 * For experiment, to be removed, bugs here, should be state determination, This
	 * will cause the overcounting the recoverNum due to the inclusion of the normal
	 * traces
	 * 
	 * @deprecated
	 */
	private boolean isRecoverStarted(int currentLocState, int startState) {
		boolean recovery = false;
		if (currentLocState == startState) {
			recovery = true;
		}
		return recovery;
	}

	/**
	 * * For experiment, to be removed, check local completion state
	 */
	private boolean isRecoveryComplete(int localState, Set<Integer> localCompletionStates) {
		boolean completeFlag = false;
		if (localCompletionStates.contains(localState))
			completeFlag = true;
		return completeFlag;
	}

	/**
	 * * For experiment, to be removed
	 */
	private boolean isConsistentEnd(int stateID, Set<Integer> icStates) {
		if (icStates.contains(stateID))
			return false;
		return true;
	}

	/**
	 * Randomly pick up an outgoing transition based on their PD
	 * 
	 * @param outTrans
	 * @return
	 */
	private Transition getRandomTransition(Set<Transition> outTrans) {
		DistributedRandomNumberGenerator rn = new DistributedRandomNumberGenerator();
		Vector<Transition> outTranVec = new Vector<Transition>();
		int j = 0;
		for (Transition t : outTrans) {
			double p = t.getEventPCA().getProbability();
			outTranVec.add(t);
			logger.debug("tran's prob: " + p + " transition: " + t.getLabel());
			rn.addNumber(j, p);
			j++;
		}
		int selectedTranID = rn.getDistributedRandomNumber();
		Transition selTran = outTranVec.get(selectedTranID);
		logger.debug("random index: " + selectedTranID + " selected transition: "
				+ selTran.getEventPCA().getTypeString() + "\t" + selTran.getEventPCA().getLabel()
				+ " to state: " + selTran.getTo());
		return selTran;

	}

	/**
	 * Travel all states of the global PCA, and build the local state mapping.
	 * <p>
	 * This is same as CompositeLPCA.getStateIDMap());
	 * 
	 * @return
	 */
	public Map<Integer, Vector<Integer>> buildGloLocStateMap() {

		Vector<CompactStateRef> locPcaRefs = this.locPCAs;

//		for (int i = 0; i < locPcaRefs.size(); i++) {
//			CompactStateRef pca = locPcaRefs.get(i);
//			logger.debug(pca.getName());
//			logger.debug(pca.getLTS());
//		}
		
		logger.debug("pca number " + this.locPCAs.size());
		Vector<Integer> currentLocStates = new Vector<Integer>();
		Map<Integer, Vector<Integer>> combStateMap = new TreeMap<Integer, Vector<Integer>>();

		// * mapping the initial state
		for (int i = 0; i < locPcaRefs.size(); i++) {
			currentLocStates.add(0);
		}

		combStateMap.put(0, currentLocStates); // initialize the local states
		Set<Integer> missingState = new HashSet<>();
		Set<Integer> visitedStates = new HashSet<>();

		Map<Integer, Set<Transition>> successors = this.getComposite().getLTS();
		int currentGlobstate;
		LinkedList<Integer> sourceStateQ = new LinkedList<>();
		sourceStateQ.addFirst(0);
		int missCount = 0;
		while (!sourceStateQ.isEmpty()) {
			currentGlobstate = sourceStateQ.removeLast();
			currentLocStates = combStateMap.get(currentGlobstate);
			int factor = computeEtaFactor(currentLocStates);

			if (currentLocStates == null) {
				missingState.add(currentGlobstate);
				continue;
			}

			if (!visitedStates.contains(currentGlobstate))
				visitedStates.add(currentGlobstate);

			Set<Transition> currentStateOutTrans = successors.get(currentGlobstate);

			if (currentStateOutTrans == null)
				continue;

			for (Transition gloOutTran : currentStateOutTrans) {

				// for debuging...
				if (gloOutTran.toString().contains("~")) {
					logger.debug(gloOutTran.toString());
				}
				// Reason about the constituted local states
				org.javatuples.Pair<Integer, Vector<Integer>> result = this
						.getTargetLocStates(currentGlobstate, currentLocStates, gloOutTran);

				logger.debug("currentState " + currentGlobstate + "," + currentLocStates + " <"
						+ gloOutTran.getEventPCA().getProbability() + ">" + gloOutTran.getLabel()
						+ "	target state " + result);

				int targetGloState = 0;
				if (result.getValue1() != null) {
					if (!combStateMap.keySet().contains(result.getValue0()))
						combStateMap.put(result.getValue0(), result.getValue1());

					targetGloState = result.getValue0();

					if (!visitedStates.contains(targetGloState)
							&& !sourceStateQ.contains(targetGloState))
						sourceStateQ.addFirst(targetGloState);

				} else {
					logger.debug("target state result is null");
					logger.debug("currentState " + currentGlobstate + "," + currentLocStates + " <"
							+ gloOutTran.getEventPCA().getProbability() + ">"
							+ gloOutTran.getLabel() + "	target state " + result);
					missCount++;
					targetGloState = gloOutTran.getTo();
					if (targetGloState == 21)
						logger.debug(targetGloState);
				}
			} // for each output transition
		}
		// logger.debug("missing count :" + missCount);
		logger.debug(
				"mapped " + combStateMap.size() + " states " + combStateMap.keySet().toString());
		logger.debug("missing " + missingState.size() + " states: " + missingState.toString());

		this.stateIDMap = combStateMap;
		return combStateMap;
	}

	/**
	 * Calculate the eta factor
	 * 
	 * @param localStateVec
	 * @return
	 */
	private int computeEtaFactor(Vector<Integer> localStateVec) {
		double f = 0;
		factor = 0;
		if (localStateVec == null || localStateVec.isEmpty()) {
			factor = 1;
			return factor;
		}

		for (int i = 0; i < localStateVec.size(); i++) {
			int stateID = localStateVec.get(i);
			CompactStateRef pcaRef = this.locPCAs.get(i);
			Set<Transition> outTrans = pcaRef.getOutTransFromState(stateID);

			for (Transition t : outTrans) {
				ActionName event = t.getEvent();
				String typeStr = event.getTypeString();
				if (!typeStr.equals("?") && !typeStr.equals("~?")) {
					f = f + event.getProbability();
				}
			}
			// logger.debug("state " + stateID + "\t" + outTrans);
		}
		factor = (int) f;
		return factor;

	}

	/**
	 * Given a global state and mapped local states, a global transition, find the
	 * target global state and local target states mapping
	 * 
	 * @param currentGlobState
	 * @param currentLocStates
	 * @param gloOutTran
	 * @return
	 */
	private org.javatuples.Pair<Integer, Vector<Integer>> getTargetLocStates(int currentGlobState,
			Vector<Integer> currentLocStates, Transition gloOutTran) {

		ActionName globalAction = gloOutTran.getEventPCA();
		int gloTargetState = gloOutTran.getTo();
		Vector<Integer> locStateIDs = new Vector<Integer>();
		Map<Integer, Set<Transition>> outTransitionMap = new TreeMap<>();

		// logger.debug(currentLocStates);

		for (int j = 0; j < this.locPCAs.size(); j++) {
			CompactStateRef localPca = this.locPCAs.get(j);
			int locState = 0;
			locState = currentLocStates.get(j);
			// int tgtState = locState;
			/**
			 * Catching the bug here!!!!!!
			 */
			if (locState == 0) {
				logger.debug(".........");
			}
			Set<Transition> outTrans = this.getMatchedOutTransitions(locState, globalAction,
					localPca);
			// logger.info("local output transitions " + locState + ":" + outTrans + " from
			// "
			// + localPca.getPcaObj().name);
			outTransitionMap.put(j, outTrans);
		} // for each local pca

		Vector<Integer> locTgtStates = this.shuffle(outTransitionMap, globalAction,
				currentLocStates);
		logger.debug("target state: " + gloTargetState + " " + locTgtStates);
		// logger.debug("-------------------------- \n");
		org.javatuples.Pair<Integer, Vector<Integer>> targetStateMap = org.javatuples.Pair
				.with(gloTargetState, locTgtStates);
		// targetStateMap.put(gloTargetState, locTgtStates);
		return targetStateMap;
	}

	/**
	 * the problem is type in composite is internal however we cannot tell if it is
	 * come from ?a+!a or ~?a+~!a. we may made mistake ?a+~!a. solution: we need
	 * know not only the source state, but the global target state, and check if the
	 * target state
	 * <p>
	 * Given a global action (with prob and type), a local PCA and its source state,
	 * find all possible transtions from the source state that can potentially match
	 * the global action
	 * 
	 * @param sourceStateID
	 * @param pca
	 * @return
	 */
	private Set<Transition> getMatchedOutTransitions(int sourceStateID, ActionName gloAct,
			CompactStateRef pca) {
		Set<Transition> resultTrans = new HashSet<>();
		/**
		 * Bug here!
		 */
		// Set<Transition> allOutTrans = pca.getOutTransFromState(sourceStateID);
		Set<Transition> allOutTrans = pca.getOutTransFromState2(sourceStateID);

		if (allOutTrans.isEmpty()) {
			logger.debug(
					"No output transitions from " + sourceStateID + " at " + pca.getPcaObj().name);
			return resultTrans;
		}
		String actionLabel = gloAct.getLabel();
		String type = gloAct.getTypeString();
		double prob = gloAct.getProbability();
		for (Transition t : allOutTrans) {
			// for each transitions from source state...
			ActionName action = t.getEventPCA();
			String actType = action.getTypeString();

			if (action.getLabel().equals(actionLabel)) {
				// check global action/transition's type
				switch (type) {
				case "": // internal or synchronised action
					// do I need distinct between internal and synchronised
					// actions?
					resultTrans.add(t);
					break;
				case "?":
					if (actType.equals("?") && (prob == action.getProbability()))
						resultTrans.add(t);
					break;
				case "!":
					// if (actType.equals("!") && (prob == action.getProbability()))
					if (actType.equals("!")) // relex the probability
						resultTrans.add(t);
					break;
				case "~?":
					if (actType.equals("~?") && (prob == action.getProbability()))
						resultTrans.add(t);
					break;
				case "~!":
					// if (actType.equals("~!") && (prob == action.getProbability()))
					if (actType.equals("~!")) // relex the probability
						resultTrans.add(t);
					break;
				case "~":
					// if (actType.equals("~") && (prob == action.getProbability()))
					if (actType.equals("~")) // relex the probability
						resultTrans.add(t);
					break;
				}
			}
		}
		/**
		 * debugging.......
		 */
		if (resultTrans.size() == 0) {
			logger.debug("resultTrans size is 0");
			// because there is no output transitions from the local state
			// sourceStateID, e.g. STOP or ERROR, END state
			// logger.debug("no transition from " + sourceStateID + " at " +
			// pca.getPcaObj().name
			// + " match " + gloAct.getTypeString() + gloAct.getLabel());
			// logger.info(sourceStateID + "\t" + gloAct.getLabel() + "\t" +
			// pca.getPca().name);
		}
		// else
		// logger.debug(resultTrans);
		return resultTrans;
	}

	/**
	 * Given a source state and outputTransition map <sourcestate,
	 * outputTransition>, a global action, and current local state vector, return a
	 * vector of target local statesIDs.
	 * 
	 * @param outTransPerPca
	 * @param globalAct
	 * @param locStates
	 * @return
	 */
	private Vector<Integer> shuffle(Map<Integer, Set<Transition>> outTransPerPca,
			ActionName globalAct, Vector<Integer> locStates) {
		Vector<Integer> targetLocState = new Vector<>();
		for (int i = 0; i < locStates.size(); i++) {
			targetLocState.add(i, locStates.get(i));
		}
		String globalActType = globalAct.getTypeString();
		if (globalActType != "") {
			/**
			 * if the global tran is NOT an internal(synchronised) action
			 */
			for (int i : outTransPerPca.keySet()) {
				Set<Transition> outTrans = outTransPerPca.get(i);
				for (Transition t : outTrans) {
					if (t.getEventPCA().getTypeString().equals(globalActType)) {
						// targetLocState.add(i, t.getTo());
						targetLocState.set(i, t.getTo());
						// make others localState and return;
						return targetLocState;
					}
				}
			}
		} // if not internal/syn global action
		else {// if the global act is internal/syn action
			for (int i : outTransPerPca.keySet()) {
				Set<Transition> outTrans = outTransPerPca.get(i);
				for (Transition t : outTrans) {
					if (t.getEventPCA().getTypeString().equals("")) {
						targetLocState.set(i, t.getTo());
						// make others localState
						return targetLocState;
					}
				}
			} // for internal actions

			for (int i : outTransPerPca.keySet()) {
				// for interface synchronized actions
				Set<Transition> outTrans = outTransPerPca.get(i);
				for (Transition sourceTran : outTrans) {
					ActionName sourceAction = sourceTran.getEventPCA();
					String type1 = sourceAction.getTypeString();
					double p1 = sourceAction.getProbability();
					// find the counterpart transition and pca
					Map<Integer, Transition> matchedTran = this.findMatchTran(outTransPerPca, i,
							globalAct, sourceAction);

					if (matchedTran.size() > 0) {
						// logger.info(matchedTran);
						// found matched transition,
						// and update local tgtstates
						// return targetLocState;
						targetLocState.set(i, sourceTran.getTo());
						for (int matchedPid : matchedTran.keySet()) {
							Transition mt = matchedTran.get(matchedPid);
							targetLocState.set(matchedPid, mt.getTo());
							// 4 debugging
							// logger.debug(sourceAction.getTypeString() + sourceAction.getLabel()
							// + "\t match \t" + mt.getEventPCA().getTypeString()
							// + mt.getEventPCA().getLabel());
							// ------------------------------------
							// error handling reset to initial state
							if (sourceAction.getTypeString().contains("~")) {
								if (sourceAction.getTypeString().contains("!"))
									targetLocState.set(i, 0);
								else if (mt.getEventPCA().getTypeString().contains("!"))
									targetLocState.set(matchedPid, 0);
							}
							// ---------------------------------------
							break;
						}
						return targetLocState;
					}
				}
			} // for synchronised action
		} // else if internal or synchronised action
		return null;
	}

	/**
	 * Given global ActionName act, find the matched synchronized action from
	 * Map<Integer, Set<Transition>>
	 * 
	 * @param outTransPerPca
	 * @param locAct
	 */
	private Map<Integer, Transition> findMatchTran(Map<Integer, Set<Transition>> outTransPerPca,
			int exclID, ActionName gloAct, ActionName locAct) {
		Map<Integer, Transition> matchedTran = new HashMap<>();
		String actType = locAct.getTypeString();
		String actLabel = locAct.getLabel();
		double prob = locAct.getProbability();
		prob = GeneralUtil.round(prob, 4);
		double gloProb = GeneralUtil.round(gloAct.getProbability(), 4);
		/**
		 * if gloProb>prob, no need to find a prob that >1
		 */
		if (prob < gloProb)
			return matchedTran;

		for (int pid : outTransPerPca.keySet()) {
			if (pid == exclID)
				continue;
			Set<Transition> trans = outTransPerPca.get(pid);

			for (Transition t : trans) {
				String tName = t.getEventPCA().getLabel();
				String tType = t.getEventPCA().getTypeString();
				double tProb = t.getEventPCA().getProbability();
				tProb = GeneralUtil.round(tProb, 4);
				// double pProb=prob * tProb;
				double pProb = GeneralUtil.round(prob * tProb, 4);
				/**
				 * pProb is to be adjusted based on outgoing transitions and their probabilities
				 * weight, rather than the probability as is.
				 */
				// logger.debug(prob + "\t" + tProb + "\t" + pProb + "\t VS.\t" + gloProb);
				// logger.debug("factor " + factor);

				// Relaxing the probability constratints
				// if (tName.equals(actLabel)
				// && (gloProb == GeneralUtil.round(pProb / this.factor, 4))) {
				if (tName.equals(actLabel)) {

					switch (actType) {
					case "?":
						if (tType.equals("!")) {
							matchedTran.put(pid, t);
							return matchedTran;
						}
						continue;
					case "!":
						if (tType.equals("?")) {
							matchedTran.put(pid, t);
							return matchedTran;
						}
						continue;

					case "~?":
						if (tType.equals("~!")) {
							matchedTran.put(pid, t);
							return matchedTran;
						}
						continue;

					case "~!":
						if (tType.equals("~?")) {
							matchedTran.put(pid, t);
							return matchedTran;
						}
						continue;

					}// switch
				} // if

			} // for each transition

		} // for each pca

		return matchedTran;
	}

	/**
	 * Create a DTMC Prism for the composite unfolded PCA models, Given a
	 * CompactState composite, Vector<CompactState> localPCAs, Vector <EPCA>
	 * localEPCAs. The objective: For each global state, to map a vector of local
	 * states from where we can extract the local variable-values.
	 * <P>
	 * Algorithm: 1. From initial state s0, ensure the mapping of s0(p1_0, p2_0, ...
	 * , pn_0) is valid. 2. then for every given start global state s(p1_s1,
	 * p2_s2,...), based on the prism comments, trace the local target state when
	 * apply the local transition on the source local state p1_s1. 3. For local
	 * target state e.g. p1_s2, find its represented ExtState, and obtain the local
	 * vars 4. Create PRISM vars based on all vars obtained, and associate vars on
	 * Prism states by rebuilding PRISM
	 * 
	 * @return
	 * @throws LTSException
	 */
	public String convertPCAToDTMC() throws LTSException {
		logger.info("THIS IS USING CompactStateRef.convertEPCAToDTMC METHOD");
		return this.composite.convertPCAToDTMC();
	}

	public CompactStateRef getComposite() {
		return composite;
	}

	public void setComposite(CompactStateRef composite) {
		this.composite = composite;
	}

	public Map<Integer, Vector<Integer>> getStateIDMap() {
		if (this.stateIDMap == null)
			this.buildGloLocStateMap();
		return stateIDMap;
	}

	public void setStateIDMap(Map<Integer, Vector<Integer>> stateIDMap) {
		this.stateIDMap = stateIDMap;
	}

}
