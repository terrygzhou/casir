package edu.rmit.casir.architecture;

import edu.rmit.casir.lpca.LabeledPCA;

public class GateP extends Gate {

//	public GateP(String name, LabeledPCA lpca) {
//		super(name, lpca);
//		// TODO Auto-generated constructor stub
//	}

	public GateP(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Integer getKind() {
		return Gate.kind_provided;
	}

}
