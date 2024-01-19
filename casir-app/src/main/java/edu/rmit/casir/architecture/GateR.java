package edu.rmit.casir.architecture;

import edu.rmit.casir.lpca.LabeledPCA;

public class GateR extends Gate {

	public GateR(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	// public GateR(String name, LabeledPCA lpca) {
	// super(name, lpca);
	// // TODO Auto-generated constructor stub
	// }

	@Override
	public Integer getKind() {

		return Gate.kind_required;
	}

}
