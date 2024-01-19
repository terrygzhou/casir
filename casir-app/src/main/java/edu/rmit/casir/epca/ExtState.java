package edu.rmit.casir.epca;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.javatuples.Triplet;

/**
 * An extended EPCA state consists of a template PCA stateID and local
 * variable-value pairs
 * 
 * @author terryzhou
 * 
 */
public class ExtState {
	Logger logger = Logger.getLogger(ExtState.class);

	private int stateID; // PCA state ID

	// the ID related to the template PCA's state
	private int absStateID;

	String processLabel;

	// e.g. <VarName, <[value1,p1][value2,p2][value3, p3]>...
	private TreeMap<String, Map<Object, Double>> locVarValueMap;

	String extStateLabel; // label the extstate: "S+stateID:v1=obj1_v2=obj2..."

	boolean explicit = true;

	Set<ExtTransition> inTrans = new HashSet<>();

	Set<ExtTransition> outTrans = new HashSet<>();

	// ************************* Methods **********************************

	/**
	 * construct a local variable extended state, vars are local variables
	 * 
	 * @param absStateID
	 * @param vars
	 *            with probabilities
	 */
	public ExtState(String processLabel, int absStateID, Map<String, Map<Object, Double>> vars) {
		this.absStateID = absStateID;
		this.processLabel = processLabel;
		

		if (absStateID == -1) {
			this.setExtStateLabel("ERROR");
			this.extStateLabel = "ERROR";
			this.setOutTrans(null);
			this.locVarValueMap = new TreeMap<>();
			return;
		}
		
		if (vars == null) {
			this.locVarValueMap = null;
			this.extStateLabel = absStateID + "";
			return;
		}

		this.locVarValueMap = new TreeMap<String, Map<Object, Double>>();
		for (Entry<String, Map<Object, Double>> v : vars.entrySet()) {
			String vLabel = v.getKey();
			// to skip the interface variables
			if (vLabel.contains("::")) {
				if (Character.isLowerCase(vLabel.charAt(0)))
					continue;
			}
			this.locVarValueMap.put(v.getKey(), v.getValue());
		}

		String label = this.processLabel + absStateID;
		if (absStateID == -2) // for superstart state, the label is distinct
			label = this.processLabel + "_ss" + absStateID;
		this.extStateLabel = label + this.locVarValueMap.toString();
		// this.extStateLabel = label;
	}

	/**
	 * convert the extended label to fsp state label
	 * 
	 * @return
	 */
	public String getFSPLabel() {
		/**
		 * if this is super final state, then return END
		 */
		if (this.getExtStateLabel().equals("END"))
			return "END";

		/**
		 * for ERROR state, reture it
		 */
		if (this.getExtStateLabel().equals("ERROR"))
			return "ERROR";

		String str = this.processLabel + this.absStateID;
		// str = this.getExtStateLabel();
		// String fspStr1 = str.replaceAll("::", "__");
		// String fsp2 = fspStr1.replaceAll("=", "_");
		/**
		 * for debugging .................
		 */
		if (this.locVarValueMap == null)
			logger.debug(this.getExtStateLabel());
		
		for (String varName : this.locVarValueMap.keySet()) {
			if (varName.contains("::") && Character.isLowerCase(varName.charAt(0)))
				continue;
			Map<Object, Double> valuePk = this.locVarValueMap.get(varName);
			for (Object v : valuePk.keySet()) {
				str = str + "_" + varName + "_" + v;
			}
		}
		String fspStr = str.replaceAll("::", "__");
		//replace -2 with _2
		String fspStr2=fspStr.replaceAll("-", "_");
		return fspStr2;
	}

	// ************************* GETTER and SETTER ************************

	public int getAbsStateID() {
		return absStateID;
	}

	public void setAbsStateID(int stateID) {
		this.absStateID = stateID;
	}

	public TreeMap<String, Map<Object, Double>> getLocVarValueMap() {
		return locVarValueMap;
	}

	@Override
	public String toString() {
		return this.extStateLabel;
	}

	public String getExtStateLabel() {
		return extStateLabel;
	}

	public void setExtStateLabel(String extStateLabel) {
		this.extStateLabel = extStateLabel;
	}

	public boolean isExplicit() {
		return explicit;
	}

	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}

	public String getProcessLabel() {
		return processLabel;
	}

	public void setProcessLabel(String processLabel) {
		this.processLabel = processLabel;
	}

	public int getStateID() {
		return stateID;
	}

	public void setStateID(int stateID) {
		this.stateID = stateID;
	}

	public Set<ExtTransition> getInTrans() {
		return inTrans;
	}

	public void setInTrans(Set<ExtTransition> inTrans) {
		this.inTrans = inTrans;
	}

	public Set<ExtTransition> getOutTrans() {
		return outTrans;
	}

	public void setOutTrans(Set<ExtTransition> outTrans) {
		this.outTrans = outTrans;
	}

	
	
}
