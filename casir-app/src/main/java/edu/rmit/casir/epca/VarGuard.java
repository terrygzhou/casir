package edu.rmit.casir.epca;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.rmit.casir.util.GeneralUtil;

/**
 * This a guard of one variable
 * 
 * @author terryzhou
 */
public class VarGuard {

	Logger logger = Logger.getLogger("edu.rmit.casir.epca.VarGuard");

	VariableType var;

	String gLabel = null;

	// the probability of this varGuard's satisfaction
	// maybe should be defined in transition
	double probability;

	Set subDomain; // reason from the label and domain

	public VarGuard(VariableType var, String conditionExp) {
		this.var = var;
		subDomain = new HashSet<>();
		this.gLabel = conditionExp;
		if (conditionExp != null) {
			this.setSubDomain(conditionExp);
		} else {
			// unconditional guard, its subdomain= domain
			this.gLabel = "else";
			// For simplicity, set var.getDomain instead of the complementary
			// items of previous "if's domains", as previously satisfied
			// condition would not come to this decision branch.
			this.setSubDomain(var.getDomain());
		}
	}

	/**
	 * Set the sub domain for the guard based on the expression of the condition
	 * 
	 * @param conditionExp
	 */
	private void setSubDomain(String conditionExp) {

		logger.debug(conditionExp);

		// formated in x==a, not x=a.
		if (conditionExp.contains("==")) {
			String value = conditionExp.substring(conditionExp.indexOf("==") + 2);
			if (GeneralUtil.isBoolean(value)) { // Boolean
				subDomain.add(new Boolean(value));
			} else if (GeneralUtil.isInteger(value)) { // Integer type
				subDomain.add(Integer.parseInt(value));
			} else {// String type
				subDomain.add(value);
			}
			if (subDomain.size() == 0) {
				logger.fatal("Condition expression error");
				System.exit(-1);
			}
		} else { // must be integer
			// Use v<a2 and v>a1 format instead of a1<v<a2
			Set<Integer> domain = this.var.getDomain();
			if (conditionExp.contains(">=")) {
				String value = conditionExp.substring(conditionExp.indexOf("=") + 1);
				int v = Integer.parseInt(value.trim());
				for (int d : domain) {
					if (d >= v)
						subDomain.add(d);
				}
			} else if (conditionExp.contains(">") && !conditionExp.contains("=")) {
				String value = conditionExp.substring(conditionExp.indexOf(">") + 1);
				int v = Integer.parseInt(value.trim());
				for (int d : domain) {
					if (d > v)
						subDomain.add(d);
				}
			} else if (conditionExp.contains("<") && !conditionExp.contains("=")) {
				String value = conditionExp.substring(conditionExp.indexOf("<") + 1);
				int v = Integer.parseInt(value.trim());
				for (int d : domain) {
					if (d < v)
						subDomain.add(d);
				}
			} else if (conditionExp.contains("<=")) {
				String value = conditionExp.substring(conditionExp.indexOf("=") + 1);
				int v = Integer.parseInt(value.trim());
				for (int d : domain) {
					if (d <= v)
						subDomain.add(d);
				}
			}
		} // integer
		
		logger.debug(subDomain);
		
	}

	/**
	 * check if value is in the domain of VarGuard
	 * 
	 * @param value
	 * @return
	 */
	public boolean evaluate(Object value) {
		Set<String> domainStr = new HashSet<String>();
		for (Object o : this.subDomain) {
			domainStr.add(o.toString());
		}
		// if(subDomain.contains(value))
		if (domainStr.contains(value.toString()))
			return true;
		return false;
	}

	public Set getSubDomain() {
		return subDomain;
	}

	public void setSubDomain(Set subDomain) {
		this.subDomain = subDomain;
	}

	public VariableType getVar() {
		return var;
	}

	public void setVar(VariableType var) {
		this.var = var;
	}

	public String getgLabel() {
		return gLabel;
	}

	public void setgLabel(String gLabel) {
		this.gLabel = gLabel;
	}

	@Override
	public String toString() {
		String str = this.var.getVarName();
		return str;
	}

}
