package edu.rmit.casir.epca;

import java.util.Map;

/**
 * 
 * @author terryzhou This is the variable instance class that carries the
 *         assigned value.
 * @param <T>
 */
public class VariableInstance<T> extends VariableType<T> {

	 Map<T, Double> instValueDist;
	
	public VariableInstance(VariableType<T> vt){
		super();
		this.varName = vt.getVarName();
		this.kind = vt.getKind();
		this.domain = vt.getDomain();
		this.namespace = vt.getNamespace();
		this.type = vt.type;
		this.setProbDist(vt.getProbDist());

	}

	public Map<T, Double> getInstValueDist() {
		return instValueDist;
	}

	public void setInstValueDist(Map<T, Double> instValueDist) {
		this.instValueDist = instValueDist;
	}

	@Override
	public String toString() {
		String str=this.namespace+"::"+this.getVarName()+"\n";
		str=str+this.getInstValueDist().toString();
		return str;
	}
	
	

}
