package edu.rmit.casir.epca;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.javatuples.Triplet;

import edu.rmit.casir.util.Exp4JUtil;
import edu.rmit.casir.util.GeneralUtil;

/**
 * For an interface transition, it consists of transition label and interface
 * variable-value pairs presented in order; for an internal transition, it
 * consists of transition label and local variable-value pairs presented in
 * order.
 * 
 * @author terryzhou
 * 
 */
public abstract class ExtTransition {
	Logger logger = Logger.getLogger(ExtTransition.class);
	ExtAction extAction;
	ExtState fromState, toState;
	double probability;
	String label;
	// transition carried variables (for interface transition only)
	List<Triplet<String, Object, Double>> tranAssVars;
	Guard satisfiedGuard;

	/**
	 * Pass in the ExtAction and assigned values of all local variables from
	 * source extState
	 * 
	 * @param extAction
	 * @param assValues
	 */
	public ExtTransition(ExtAction extAction, List<Triplet<String, Object, Double>> oneChoiceVars) {
		this.extAction = extAction;
		this.tranAssVars = oneChoiceVars;
	}

	/**
	 * depending on what type of transitions/actions
	 * 
	 * @return
	 */
	public double getProbability() {
		return this.probability;
	}

	public String getExtEventLabel() {
		return this.label;
	}

	protected void setExtEventLabel(String label) {
		this.label = label;
	}

	protected void setProbability(double p) {
		this.probability = p;
	}

	/**
	 * Given a combination of transition values and fromState's local variables,
	 * based on the action's guard-update function, compute and return the target
	 * local variables.
	 * 
	 * @return
	 */
	public Map<String, Map<Object, Double>> getTargetValues() {
		// need get its fromState to find the local vars
		ExtState fromState = this.getFromState();
		Map<String, Map<Object, Double>> locVars = fromState.getLocVarValueMap();

		// for input/output actions, localAssVars are all vars
		List<Triplet<String, Object, Double>> tranVarValues = this.tranAssVars;

		// copy the source values
		// Map<String, Map<Object, Double>> targetValues =
		// this.convertTreeMapValues(assValues);
		Map<String, Map<Object, Double>> targetLocValues = new HashMap<>();

		/**
		 * merge all localVars and its interface vars if any.
		 */
		targetLocValues.putAll(locVars);
		Map<String, Map<Object, Double>> allVarMap = new HashMap<>();
		allVarMap.putAll(locVars);
		allVarMap.putAll(this.convertTreeMapValues(this.tranAssVars));

		List<Triplet<String, Object, Double>> allVarList = new LinkedList<>();
		for (String key : allVarMap.keySet()) {
			Triplet tr = null;
			Map<Object, Double> m = allVarMap.get(key);
			for (Object value : m.keySet()) {
				Double prob = m.get(value);
				tr = Triplet.with(key, value, prob);
				break;
			}
			allVarList.add(tr);
		}

		// targetValues = new TreeMap<String, Map<Object, Double>>();
		ExtAction extAction = this.extAction;
		// copy the source values in cases of some values not update
		Map<String, Object> currentValues = new TreeMap<>();
		// assValues maybe all variables incl. interface var and local var
		for (Triplet<String, Object, Double> singleVar : allVarList) {
			String varName = singleVar.getValue0();
			// logger.info(varName);
			Object value = singleVar.getValue1();
			currentValues.put(varName, value);
		}
		// for each guard defined in extAction...
		List<Guard> gList = extAction.getGuardsList();// .getGuardSet();
		// if no guards defined, then return the source state's local vars
		// the target ExtState is implicit state
		if (gList == null || gList.size() == 0)
			return targetLocValues;

		for (Guard g : gList) { // for each guard
			if (g.evaluate(currentValues)) {
				// need to evaluate all variables incl. internal, interface
				// logger.info(this.getExtEventLabel());
				logger.debug("debugging.........");
				logger.debug(currentValues);
				logger.debug(g);
				if (g.getFuncs() == null)
					continue;
				for (VarUpdFunc f : g.getFuncs()) {
					String var = f.getVarName();
					Object currentVal = currentValues.get(var);
					logger.debug(var + "\t" + currentVal);
					String exp = f.getFuncExp();
					Object val, targetValue = null;
					if (GeneralUtil.isInteger(currentVal.toString())) {
						val = Integer.parseInt(currentVal.toString());
						targetValue = (Integer) Exp4JUtil.compute(exp, var, (int) val);
					} else {
						if (GeneralUtil.isBoolean(currentVal.toString())) {
							val = Boolean.parseBoolean(currentVal.toString());
							targetValue = Exp4JUtil.compute(exp, var, (boolean) val);
						}
					}
					// need to qulify the targetValue making sure it is in the
					// domain
					logger.debug(var + "\t" + currentVal + "\t" + exp + "\t"
							+ targetValue.toString() + "\t" + currentValues.toString());

					Map<Object, Double> valObj = new HashMap<>();
					// assuming computed variables always be probability=1
					// for other values with probability=0 are not added
					valObj.put(targetValue, 1.0);
					targetLocValues.put(var, valObj);
				} // for each var update value
				//new added for exclusively chose one guard
				break;
			} // if evaluate true
			else
				continue;
			// break; // only one g can be satisfied.
		} // for each guard
			// logger.info(targetValues);
		return targetLocValues;
	}

	/**
	 * convert/merge List<Triplet<VarName, value, prob>> to Map<VarName,<value,
	 * probability>>
	 * 
	 * @param assValues
	 * @return
	 */
	protected Map<String, Map<Object, Double>> convertTreeMapValues(
			List<Triplet<String, Object, Double>> assValues) {
		Map<String, Map<Object, Double>> mergedValues = new TreeMap<String, Map<Object, Double>>();
		for (Triplet<String, Object, Double> triple : assValues) {
			if (mergedValues.keySet().contains(triple.getValue0())) {
				Map<Object, Double> existValueMap = mergedValues.get(triple.getValue0());
				existValueMap.put(triple.getValue1(), 1.0); // set prob=1.0
			} else {
				Map<Object, Double> newValueMap = new HashMap<>();
				newValueMap.put(triple.getValue1(), 1.0); // set the prob=1.0
				mergedValues.put(triple.getValue0(), newValueMap);
			}
		}
		return mergedValues;
	}

	/**
	 * get the variable's related guard by var name.
	 * 
	 * @param varName
	 * @return
	 */
	public VarGuard getGuardbyVar(String varName) {
		for (VarGuard vg : this.satisfiedGuard.getGuard()) {
			if (vg.getVar().getVarName().equals(varName)) {
				return vg;
			}
		}
		return null;
	}

	public ExtState getFromState() {
		return fromState;
	}

	public void setFromState(ExtState fromState) {
		this.fromState = fromState;
	}

	public ExtState getToState() {
		return toState;
	}

	public void setToState(ExtState toState) {
		this.toState = toState;
	}

	public ExtAction getExtAction() {
		return extAction;
	}

	public void setExtAction(ExtAction extAction) {
		this.extAction = extAction;
	}

	public Guard getSatisfiedGuard() {
		return satisfiedGuard;
	}

	public void setSatisfiedGuard(Guard satisfiedGuard) {
		this.satisfiedGuard = satisfiedGuard;
	}

	public String getFSPLabel() {
//		logger.debug(this.getExtEventLabel());
		String label = this.getExtEventLabel().replace("::", "__");
		String label2 = label.replace("=", "_");
		return label2;
	}

	@Override
	public String toString() {
		return extAction.getActionType() + "<" + this.getProbability() + ">"
				+ this.getExtEventLabel();
	}

}
