package edu.rmit.casir.architecture.testBed;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;

import edu.rmit.casir.architecture.Configuration;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.DistributedRandomNumberGenerator;
import edu.rmit.casir.util.FileHandler;
import lts.Transition;

/**
 * TestBed runs the transformed model virtually and collects the raw data for
 * each run including: the states, transitions and the costs; The input
 * parameters include: transformed LPCA, cost specification,
 * 
 * @author terryzhou
 *
 */
public class TestBed {

	Logger logger = Logger.getLogger(TestBed.class);
	CompositeLPCA gloLpca;
	String outputPath;
	Map<String, Double> costMap;

	// the following actions are given based on the handler
	Set<String> startRecActionLabels; // e.g. avail
	Set<String> completeRecActionLabel; // e.g. ~request, supply_amount_1, supply_amount_0 etc.

	public TestBed(Configuration afterConf, String outputPath, Set<String> initRecActLabels,
			Set<String> termRecActLabels, Map<String, Double> costSpecs) {

		this.gloLpca = afterConf.getGloModel();
		this.outputPath = outputPath;
		this.startRecActionLabels = initRecActLabels;
		this.completeRecActionLabel = termRecActLabels;
		this.costMap = costSpecs;
	}

	/**
	 * Evaluate the transformed AC, by: 1
	 * 
	 * @param afterConf
	 */
	public void evaluate(Configuration afterConf, int bound) {
		this.virtualRun(bound);
	}

	/**
	 * Virtually run the executable model bound-times instances
	 * 
	 * @param bound
	 */
	public void virtualRun(int bound) {
		LabeledPCA gloLpca = this.gloLpca.getGloLPCA();
		Map<Integer, Set<Transition>> lts = gloLpca.getLTS();
		Map<Integer, Map<String, Object>> varLabels = gloLpca.getVarLabel();

		double totalCost = 0;
		double totalRecCost = 0;
		double totalHandlerCost = 0;
		int recoveryNum = 0;
		double inconsistentNum = 0;
		double errors = 0;

		StringBuffer outputBuff = new StringBuffer();

		for (int i = 0; i < bound; i++) { // for each instance
			int stateID = 0;
			logger.debug("*************************************************************");
			Set<Transition> outTrans = lts.get(stateID);
			StringBuffer traceStr = new StringBuffer();

			double traceCost = 0;
			double recCost = 0;
			double handlerCost = 0;

			boolean inHandlerRange = false;
			boolean recovery = false;

			// go thru the path until no further outgoing transitions
			while (outTrans != null && outTrans.size() > 0) {

				logger.debug("------------- From state ----------------");
				logger.debug("to state: " + stateID + " predicates: ");
				StringBuilder predicateStr = new StringBuilder();
				Map<String, Object> stateVarLabel = varLabels.get(stateID);
				int varSize = 0;
				for (String varName : stateVarLabel.keySet()) {
					predicateStr.append(varName + "=" + stateVarLabel.get(varName).toString());
					varSize++;
					if (varSize < stateVarLabel.keySet().size())
						predicateStr.append(",");
				}
				traceStr.append(stateID);
				traceStr.append(":(" + predicateStr + ")");
				Transition selTran = this.getRandomTransition(outTrans);

				// find next state
				stateID = selTran.getTo();
				// String tranLabel = selTran.getLabel();
				String tranLabel = selTran.getEventPCA().getTypeString()
						+ selTran.getEventPCA().getLabel();
				logger.debug(tranLabel);

				if (this.isRecoverStarted(tranLabel, this.startRecActionLabels)) {
					inHandlerRange = true;
					recovery = true;
					recoveryNum = recoveryNum + 1;
					recCost = 0;
				}

				Double cost = costMap.get(tranLabel);
				this.costMap.get(tranLabel);

				if (cost == null)
					cost = 0.0;

				traceCost = traceCost + cost;
				if (recovery) {
					recCost = recCost + cost;
				}

				if (inHandlerRange) {
					if (this.isRecoveryComplete(selTran.getLabel())) {
						// bug for reaching non-successful recovery
						inHandlerRange = false;
						handlerCost = recCost;
					}
				}
				traceStr.append(" -> " + selTran.getEventPCA().getTypeString()
						+ selTran.getEvent().getOriginalLabel() + "[" + cost + "]" + " -> ");
				outTrans = lts.get(stateID);
				if (outTrans == null || outTrans.size() == 0) {
					traceStr.append(stateID);
					logger.debug("termination at state " + stateID + " with predicates: ");

					predicateStr = new StringBuilder();

					Map<String, Object> gloStateLabel = this.gloLpca.getGloLPCA().getVarLabel()
							.get(stateID);
					logger.debug(gloStateLabel);
					if (stateID == -1) {
						predicateStr.append("ERROR");
						errors = errors + 1;
					} else
						for (String varName : gloStateLabel.keySet()) {
							predicateStr.append(varName + "=" + gloStateLabel.get(varName));
							// if (predicateStr.toString().trim().equals("ERROR"))
							// errors = errors + 1;
						}
					traceStr.append(":(" + predicateStr + ")");

					logger.debug(traceStr.toString() + "\t total cost:" + traceCost);
					outputBuff.append(traceStr.toString() + "\n" + "Total  Cost:" + traceCost
							+ "\t Recovery  Cost:" + recCost + "\t Handler  cost:" + handlerCost
							+ "\n \n");
					totalCost = totalCost + traceCost;
					totalRecCost = totalRecCost + recCost;
					totalHandlerCost = totalHandlerCost + handlerCost;
				}
			}
		} // bound times

		double recRateTime = 0;
		double handRateTime = 0;

		if (recoveryNum > 0) {
			recRateTime = totalRecCost / recoveryNum;
			handRateTime = totalHandlerCost / recoveryNum;
		}

			// logger.info("The Recovery occured on " + recoveryNum+" instances.");
		logger.info("Inconsistent instance before recovery:\t" + recoveryNum );
		logger.info("Checking the inconsistent instance after recovery from the test result \n");
		
		logger.info("AVE Total Cost:\t" + totalCost / bound);
		logger.info("AVE Recovery  Cost:\t" + recRateTime);
		logger.info("AVE Handler  Cost\t" + handRateTime);

		// logger.info("Inconsistent instance after recovery:\t" + inconsistentNum +
		// "\n");
		logger.info("Reliability:\t" + (1 - errors / bound) + "\n");

		outputBuff.append("\n" + "Inconsistent instance before recovery:\t" + recoveryNum + "\n");
		// outputBuff.append("Inconsistent instance after recovery:\t" + inconsistentNum
		// + "\n");
		outputBuff.append("\n" + "AVE Total Cost:\t" + totalCost / bound + "\n");

		outputBuff.append("\n" + "AVE Recovery  Cost:\t" + recRateTime + "\n");
		outputBuff.append("\n" + "AVE Handler  Cost\t" + handRateTime + "\n");
		outputBuff.append("\n" + "Reliability:\t" + (1 - errors / bound) + "\n");
		// outputBuff.append("Inconsistnecy Prob:\t" + inconsistentNum / bound + "\n");

		try {
			FileHandler.delFile(outputPath);
			FileHandler.appendPathFile(outputPath, outputBuff);
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
	 * * For experiment, to be removed, check local completion state
	 */
	private boolean isRecoveryComplete(String actLabel) {
		boolean completeFlag = false;
		if (this.completeRecActionLabel.contains(actLabel))
			completeFlag = true;
		return completeFlag;
	}

	/**
	 * Randomly select a locally controlled transition based on the PD
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

}
