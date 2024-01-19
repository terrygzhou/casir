package edu.rmit.casir.epca;

import java.util.List;

import org.javatuples.Triplet;


public class ExtInitTransition extends ExtTransition {

	
	public ExtInitTransition(ExtAction extAction,
			List<Triplet<String, Object, Double>> combVars) {
		super(extAction, combVars);
		double p = extAction.getProbability();
		for (Triplet<String, Object, Double> var : combVars) {
				p=p* var.getValue2();
		}
		//		this.setProbability(p);
		this.setProbability(Math.round(p*1000.0)/1000.0);
	}

}
