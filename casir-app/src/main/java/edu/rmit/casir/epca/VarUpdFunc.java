package edu.rmit.casir.epca;

/**
 * The VarUpdateFunction is the update function over a single variable
 * @author terryzhou
 * 
 */
public class VarUpdFunc {
	String funcExp;		//String expression
	String varName;	//local variable only
	String actTypeLabel;	//need it??

	public VarUpdFunc(String actTypeName, String varName, String funcExp) {
		this.actTypeLabel = actTypeName;
		this.funcExp = funcExp;
		this.varName = varName;
	}

	public String getFuncExp() {
		return funcExp;
	}

	public void setFuncExp(String funcExp) {
		this.funcExp = funcExp;
	}

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}


	@Override
	public String toString() {
		String str=this.actTypeLabel+this.varName+",";
		str=str+this.funcExp;
		return str;
	}
	
}
