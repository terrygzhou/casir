package edu.rmit.casir.architecture;

public class Binding {

	String name;

	// should the source must always be the GateR
	// and the target must always be GateP?
	Gate source;
	Gate target;

	// *************************************************************************

	public Binding(String name, Gate s, Gate t) {
		this.name = name;
		this.source = s;
		this.target = t;
	}

	// ************************** GETTER and SETTER ***************************

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Gate getSource() {
		return source;
	}

	public void setSource(Gate source) {
		this.source = source;
	}

	public Gate getTarget() {
		return target;
	}

	public void setTarget(Gate target) {
		this.target = target;
	}

}
