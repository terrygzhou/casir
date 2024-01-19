package edu.rmit.casir.verification;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import au.edu.rmit.dsea.openradl.ADNs.PropertyContainerImpl;
import edu.rmit.casir.epca.VariableType;

public class PctlProperty extends PropertyContainerImpl {

	// <propName, Set<VariableType>>
	Map<String, Set<VariableType>> propVariables;

	// *************************** Methods ***************************************

	public PctlProperty(String name, String property, Set<VariableType> vars) {
		super();
		try {
			this.setProperty(name, property);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.propVariables = new HashMap<>();
		this.propVariables.put(name, vars);
		// this.setVariables(vars);
	}

	// ************************** GETTER and SETTER *******************************

	public Set<VariableType> getVariables(String name) {
		return this.propVariables.get(name);
	}

	public void setVariables(String name, Set<VariableType> variables) {
		this.propVariables.put(name, variables);
	}

	public Map<String, Set<VariableType>> getPropVariables() {
		return propVariables;
	}

	public void setPropVariables(Map<String, Set<VariableType>> propVariables) {
		this.propVariables = propVariables;
	}

}
