package edu.rmit.casir.epca;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Guard maps epfsp's guard i.e. [(v1>2 & v2="f" & ... & vn >=3)], The given
 * text is translated to: variable, [/(subdomain_low, subdomain_high)/].
 * <P>
 * Guard is the conjunction of all VarGuards (objects).
 * 
 * @author terryzhou
 * 
 */
public class Guard {
	Logger logger = Logger.getLogger("edu.rmit.casir.epca.Guard");
	// this guardLabel is included in explicit transition relabelling
	String guardLabel;

	// A set of conjunction VarGuard objects
	// need change to LinkedList
	protected Set<VarGuard> vGuardSet;

	// the variables in Set<VarUpdFunc> are not necessary identical to the
	// variables in this.guard
	protected Set<VarUpdFunc> funcs;

	public Guard(String gLable) {
		this.vGuardSet = new HashSet<VarGuard>();
		this.guardLabel = gLable;
	}

	public void addVarGuard(VarGuard vg) {
		this.vGuardSet.add(vg);
	}

	/**
	 * varGuards=null if action a has no guards
	 * 
	 * @param varGuards
	 */
	public Guard(Set<VarGuard> varGuards) {
		String label = "";
		int i = 0;
		// for null
		if (varGuards == null) {
			this.guardLabel = "";
			this.vGuardSet = null;
			return;
		}
		for (VarGuard vg : varGuards) {
			label = label + vg.getgLabel();
			i++;
			if (i < varGuards.size())
				label = label + "_";
		}
		this.guardLabel = label;
		this.vGuardSet = varGuards;
	}

	public Guard(Set<VarGuard> varGuards, String gLabel) {
		String label = "";
		int i = 0;
		// for null
		if (varGuards == null) {
			this.guardLabel = "";
			this.vGuardSet = null;
			return;
		}
		for (VarGuard vg : varGuards) {
			label = label + vg.getgLabel();
			i++;
			if (i < varGuards.size())
				label = label + "_";
		}
		if (gLabel==null || gLabel.equals(""))
			this.guardLabel = label;
		else
			this.guardLabel = gLabel;
		this.vGuardSet = varGuards;
	}

	/**
	 * Given values, evaluate the values against the guard that consists of a
	 * set of varGuards.
	 * 
	 * @param values
	 * @return
	 */
	public boolean evaluate(Map<String, Object> values) {
		if (this.guardLabel.equals("else") || this.guardLabel.equals("true"))
			// for "else" guard that has no guardSet
			return true;
		logger.debug(this.getGuardLabel());
		for (String fullName : values.keySet()) {
			// String varName=fullName.substring(fullName.indexOf("::")+2);
			for (VarGuard vg : this.vGuardSet) {
				logger.debug(vg.getSubDomain());
				logger.debug(vg.getVar().getNamespace() + "::" + vg.getVar().getVarName());
				logger.debug(fullName);
				if ((vg.getVar().getNamespace() + "::" + vg.getVar().getVarName())
						.equals(fullName)) {
					boolean isMember = vg.evaluate(values.get(fullName));
					if (!isMember)
						return false;
					// else
					// break;
				} // if
			} // for each vg
		} // for each var
		return true;
	}

	@Override
	public String toString() {
		String str = this.guardLabel;
		return str;
	}

	public String getGuardLabel() {
		return guardLabel;
	}

	public void setGuardLabel(String guardLabel) {
		this.guardLabel = guardLabel;
	}

	public Set<VarGuard> getGuard() {
		return vGuardSet;
	}

	public void setGuard(Set<VarGuard> guard) {
		this.vGuardSet = guard;
	}

	public Set<VarUpdFunc> getFuncs() {
		return funcs;
	}

	public void setFuncs(Set<VarUpdFunc> funcs) {
		this.funcs = funcs;
	}

}
