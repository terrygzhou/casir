package edu.rmit.casir.lpca;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.util.FileHandler;
import edu.rmit.casir.util.GeneralUtil;
import lts.ActionName;
import lts.CompactState;
import lts.Pair;
import lts.Transition;

public class LabeledPCA extends CompactStateRef implements Serializable {

	Set<VariableType> variables;

	String name;

	/**
	 * the Key is starting from -1 if there is ERROR state
	 */
	// Map<localStateID, ExtState>, any difference between VPCA's extStates?
	// Map<Integer, ExtState> extStates;

	Map<Integer, Map<String, Object>> varLabel;

	// stateLabels are derived from extStates by including the abstract stateID, and
	// VCs
	Map<Integer, String> stateLabels;

	VPCA vpca; // optional reference to the VPCA

	Logger logger = Logger.getLogger(LabeledPCA.class);

	// ****************************** methods *************************************

	public LabeledPCA(CompactState compactState, Map<Integer, Map<String, Object>> varlabel) {
		super(compactState);
		this.varLabel = varlabel;
		this.name = compactState.name;
	}

	/**
	 * Output Graphiz dot file
	 * 
	 * @param outputDirPath
	 */
	public void outputDotFigure(String outputPath) {
		// String name = (String) this.getPrivateObject("name");
		// String outputPath = outputDirPath + name + ".dot";
		StringBuffer dotSB = new StringBuffer(this.getGraphvizDot());
		FileHandler.delFile(outputPath);
		try {
			FileHandler.outputPathFile(outputPath, dotSB);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * generate dot using varlabels
	 * 
	 * @return
	 */
	public String getGraphvizDot() {
		Map<Integer, Map<String, Object>> varLabels = this.getVarLabel();

		CompactStateRef cs = this;// .get.getGlobalPCA().getComposite();
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
		boolean hasFailureAction = false;
		for (int i : lts.keySet()) {
			if (i == 0)
				graphvizSpec.append("\t" + i + "[label =\" 0 \n SuperInitialState\" "
						+ "shape = box, color = black, style = filled, fillcolor = orange]; \n");
			else {
				// the identifier is not just i, it should include the varlabels;
				Map<String, Object> varObj = varLabels.get(i);
				StringBuffer stateNodeStr = new StringBuffer();
				// StringBuffer idPostfix = new StringBuffer();
				// idPostfix.append(i);
				stateNodeStr.append("\t" + i + "[label=\"" + i + "\n");

				for (String varName : varObj.keySet()) {
					Object value = varObj.get(varName);
					stateNodeStr.append(varName + "=" + value.toString() + "\n");
					// idPostfix.append(varName + "=" + value.toString());
					/**
					 * adding local vPCA's abstract stateID in the state label
					 */
				}

				stateNodeStr.append("\"");
				graphvizSpec.append(stateNodeStr
						+ " shape = box, color = black, style = filled, fillcolor = cyan]; \n");
			} // else
		}

		int tranCount = 0;
		double sumProb = 0;

		Map<String, Object> locVars = null;

		for (int i : lts.keySet()) {
			Set<Transition> outTrans = lts.get(i);
			for (Transition t : outTrans) {
				ActionName label = t.getEventPCA();
				int fromState = t.getFrom();
				int toState = t.getTo();

				StringBuffer fromStateStr = new StringBuffer();
				fromStateStr.append(fromState);
				StringBuffer toStateStr = new StringBuffer();
				toStateStr.append(toState);

				double prob = label.getProbability();
				tranCount++;
				if (tranCount == outTrans.size())
					prob = 1 - sumProb;
				prob = GeneralUtil.round(prob, 4);

				graphvizSpec.append("\t " + fromState + " -> " + toState + "[label = \""
						+ label.getTypeString() + "<" + prob + ">" + t.getLabel() + "\" ];\n  ");

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

	// ************************* GETTER and SETTER *******************************

	public Map<Integer, String> getStateLabels() {
		return stateLabels;
	}

	public void setStateLabels(Map<Integer, String> stateLabels) {
		this.stateLabels = stateLabels;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<VariableType> getVariables() {
		return variables;
	}

	public void setVariables(Set<VariableType> variables) {
		/**
		 * partially fix the bug by setting the local variables with the same namespace
		 */
//		Set<VariableType> locVar = new HashSet<>();
//		for (VariableType var : variables) {
//			if (var.getNamespace().equals(this.getName())) {
//				locVar.add(var);
//			}
//		}
		this.variables = variables;
	}

	public VPCA getVpca() {
		return vpca;
	}

	public void setVpca(VPCA vpca) {
		this.vpca = vpca;
	}

	/**
	 * for testing
	 */
	public void print() {
		// explore the lts
		logger.info(this.name);
		for (int stateID : this.getLTS().keySet()) {
			logger.info("-------------------\t state \t --------------------");
			Map<String, Object> varLabel = this.getVarLabel().get(stateID);
			Set<Transition> outTrans = this.getLTS().get(stateID);
			logger.info("state ID: " + stateID + "\t with variables:");
			varLabel.forEach((a, b) -> {
				logger.info(a + " = " + b.toString());
			});
			logger.info("outgoing transitions from " + stateID);
			outTrans.forEach(t -> {
				logger.info(t.getEventPCA().getTypeString() + "<" + t.getEventPCA().getProbability()
						+ ">" + t.getEventPCA().getLabel());
				logger.info("toState \t" + t.getTo());
			});
		}
	}

	public Map<Integer, Map<String, Object>> getVarLabel() {
		return varLabel;
	}

	public void setVarLabel(Map<Integer, Map<String, Object>> varLabel) {
		this.varLabel = varLabel;
	}

}
