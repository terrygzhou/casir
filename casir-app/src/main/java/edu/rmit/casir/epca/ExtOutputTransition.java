package edu.rmit.casir.epca;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.javatuples.Triplet;

import edu.rmit.casir.util.GeneralUtil;

public class ExtOutputTransition extends ExtTransition {

	public ExtOutputTransition(ExtAction extAction,
			List<Triplet<String, Object, Double>> combVars) {
		super(extAction, combVars);

		double p = extAction.getProbability();
		double probV;

		String extLabel = extAction.getActionLabel();

		/**
		 * refactoring Java 8 BiPredicate
		 */
		Predicate<String> pred_lowcase = (str) -> Character.isLowerCase(str.charAt(0));
		BiPredicate<String, String> pred_identical = (str1, str2) -> str1.equals(str2);

		if (combVars != null)
			/**
			 * Java 8 
			 */
			p = combVars.stream()
				.mapToDouble(var -> var.getValue2())
				.reduce(p,(v1, v2) -> v1 * v2);
		
//		combVars.stream()
//		.map(var -> var.getValue0())
//		.filter(pred_lowcase)
//		.map(fullVarName -> fullVarName.substring(0, fullVarName.indexOf(":")))
//		.filter(item -> item.equals(extAction.getActionLabel()))
//		.map(elabel -> elabel + "_" + fullVarName + "=" + var.getValue1());
		
//		p = extAction.getProbability();
		for (Triplet<String, Object, Double> var : combVars) {
			String fullVarName = var.getValue0();
//			probV = var.getValue2();
			/**
			 * All variable values' probability are taken into account
			 */
//			p = p * probV;
			
			// if (Character.isLowerCase(fullVarName.charAt(0)))
			/**
			 * Java 8 Predicate
			 */
			if (pred_lowcase.test(fullVarName)) {
				String ns = fullVarName.substring(0, fullVarName.indexOf(":"));
				String actLabel = extAction.getActionLabel();
				
				/**
				 * only record interface vars on the transition,
				 */
				
				/**
				 * Java 8 BiPredicate
				 */
				if (pred_identical.test(ns, actLabel))
					// if (ns.equals(actLabel))
					extLabel = extLabel + "_" + fullVarName + "=" + var.getValue1();
			}
		}
		// this.setProbability(Math.round(p * 10000.0) / 10000.0);
//		logger.debug(p);
		this.setProbability(GeneralUtil.round(p, 4));
		this.setExtEventLabel(extLabel);
	}
}
