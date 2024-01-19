package edu.rmit.casir.epca;

import java.util.List;

import org.javatuples.Triplet;

public class ExtInputTransition extends ExtTransition {

	/**
	 * As this is an inputTransition, the combVars contains all interface vars
	 * and local vars
	 * 
	 * @param extAction
	 * @param combVars
	 */
	public ExtInputTransition(ExtAction extAction, List<Triplet<String, Object, Double>> combVars) {
		super(extAction, combVars);
		// set the probability
		// double p = 1.0;
		double p = extAction.getProbability();
		double probV;
		String extLabel = "";
		if (combVars != null)
			for (Triplet<String, Object, Double> var : combVars) {
				String fullName = var.getValue0();
				probV = var.getValue2();

				// if it is local var
				if (Character.isUpperCase(fullName.charAt(0))) {
					p = p * probV;
				} else { // var is an interface variable
					String ns = fullName.substring(0, fullName.indexOf(":"));
					String actLabel = extAction.getActionLabel();
					// double intprob = var.getValue2();
					if (ns.equals(actLabel)) {
						// if relevant interface var e.g. a::va for the
						// extAction a
						extLabel = extLabel + "_" + var.getValue0() + "=" + var.getValue1();
						// in case an interface var been initalised Zero
						if (probV == 0)
							p = 0;
					} else {
						p = p * probV;
					}
				}
			}
		this.setProbability(Math.round(p * 1000.0) / 1000.0);
		this.setExtEventLabel(extAction.getActionLabel() + extLabel);
	}

}
