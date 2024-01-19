package edu.rmit.casir.util;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.apache.log4j.Logger;

public class Exp4JUtil {
	static Logger logger=Logger.getLogger("edu.rmit.casir.util.Exp4JUtil");
	
	/**
	 * single var update
	 * @param exp
	 * @param var
	 * @param value
	 * @return
	 */
	public static int compute(String exp, String var, int value) {
		logger.debug("var:"+var+"\t"+"value:\t"+value+"\t expression:"+exp);
		/**
		 * workaround for constant exp,
		 * using == for assertion instead of assignment
		 * e.g. xyz==1
		 */
		if(exp.contains("==")){
			String rightPart=exp.substring(exp.indexOf("=")+2);
			if(GeneralUtil.isInteger(rightPart))
				return Integer.parseInt(rightPart);
		}
		String nvar=var.replace("::", "__");
		String nexp=exp.replace("::", "__");
		logger.debug(nexp);
		if(nexp.equals("WD__owned=1"))
			logger.debug(".......");
		Expression e = new ExpressionBuilder(nexp).variables(nvar).build();
		e.setVariable(nvar, value);
		double r = e.evaluate();
		return (int)r;
	}
	
	
	/**
	 * logic operation
	 * @param exp
	 * @param var
	 * @param value
	 * @return
	 */
	public static boolean compute(String exp, String var, boolean value){
		/**
		 * workaround when exp is a constant boolean value
		 */
		if(GeneralUtil.isBoolean(exp))
			return Boolean.parseBoolean(exp);
		/**
		 * == in algebra language is assertion replacing assignment
		 */
		if(exp.contains("==")){
			//cater for: exp=Eshop::avail=true
			String rightPart=exp.substring(exp.indexOf("==")+2);
			if(GeneralUtil.isBoolean(rightPart))
				return Boolean.parseBoolean(rightPart);
		}
		return false;
		
	}
	
	
}
