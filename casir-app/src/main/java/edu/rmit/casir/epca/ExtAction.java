package edu.rmit.casir.epca;

import java.util.List;

/**
 * ExtAction includes a set of mutual exclusive guards and update functions.
 * Such function is from domain to domain product: V -> V
 * 
 * @author terryzhou
 * 
 */
public class ExtAction {

	private String actionLabel;
	private double probability; // transition probability
	private String actionType; // from PCA: input, output, internal
	protected List<Guard> guardsList;

	public static final String TYPE_INPUT = "?";
	public static final String TYPE_ONPUT = "!";
	public static final String TYPE_ONPUT_FAIL = "~!";
	public static final String TYPE_INPUT_FAIL = "~?";
	public static final String TYPE_INTERNAL = "";
	public static final String TYPE_INTERNAL_FAIL = "~";
	


	public ExtAction(String actLabel, String actionType, double probability) {
		this.actionLabel = actLabel;
		this.actionType = actionType;
		this.probability=probability;
	}

	public String getActionLabel() {
		return actionLabel;
	}

	public void setActionLabel(String actionLabel) {
		this.actionLabel = actionLabel;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public List<Guard> getGuardsList() {
		return guardsList;
	}

	public void setGuardsList(List<Guard> guardsList) {
		this.guardsList = guardsList;
	}
	

}