package edu.rmit.casir.epca;

import java.util.List;

import org.javatuples.Triplet;

public class ExtInternalTransition extends ExtTransition {

	public ExtInternalTransition(ExtAction extAction,
			List<Triplet<String, Object, Double>> combVars) {
		super(extAction, combVars);
		double p = extAction.getProbability();
		double probV;
		String extLabel = extAction.getActionLabel();
		if (combVars != null)
			for (Triplet<String, Object, Double> var : combVars) {
				String fullVarName = var.getValue0();
				// for local var
				if (Character.isUpperCase(fullVarName.charAt(0))) {
					probV = var.getValue2();
					p = p * probV;
					extLabel = extLabel + "_" + fullVarName + "=" + var.getValue1();
				}
			}
		// this.setProbability(p);
		this.setProbability(Math.round(p * 1000.0) / 1000.0);
		this.setExtEventLabel(extLabel);
	}
}
