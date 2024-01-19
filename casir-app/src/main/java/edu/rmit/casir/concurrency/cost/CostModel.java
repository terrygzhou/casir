package edu.rmit.casir.concurrency.cost;

public class CostModel {
	public static  final String	CONCURRENCY_OBLIVIOUS	= "MONEY";
	public static  final String	prob	= "Probability";
	public static  final String	CONCURRENCY_AWARE	= "TIME";
	public static  final String	MAX		= "MAX";
	public static final String	MIN		= "MIN";
	public static final String	PLUS	= "+";
	public static final String	TIMES	= "*";

	String				resourceType;				// time? dollar? energy? probability?
	String				preferenceType;			// min or max? best or worst?
	String				seqOperator;
	String				parOperator;
	String				altOperator;
	
	public CostModel(String resource, String preference){
		this.resourceType=resource;
		this.preferenceType=preference;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getPreferenceType() {
		return preferenceType;
	}

	public void setPreferenceType(String preferenceType) {
		this.preferenceType = preferenceType;
	}

	public String getSeqOperator() {
		if (this.resourceType.equals(prob))
			return TIMES;
		return PLUS; // by default
	}

	public void setSeqOperator(String seqOperator) {
		this.seqOperator = seqOperator;
	}

	public String getParOperator() {
		if (this.resourceType.equals(CONCURRENCY_OBLIVIOUS))
			return PLUS;
		if (this.resourceType.equals(CONCURRENCY_AWARE))
			return MAX;
		return PLUS; // by deafult, plus
	}

	public void setParOperator(String parOperator) {
		this.parOperator = parOperator;
	}

	public String getAltOperator() {
		if (this.preferenceType.equals(MAX))
			return MAX;
		else
			return MIN;
	}

	public void setAltOperator(String altOperator) {
		this.altOperator = altOperator;
	}

}
