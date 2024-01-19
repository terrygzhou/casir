package edu.rmit.casir.epca;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class VariableType<T> {
	// private T t;
	public int type; // type e.g. Integer, Boolean, String etc.

	// the namespace can be an interface action label or the process ID
	String namespace;

	private Map<T, Double> probDist;

	// without namespace
	String varName;

	int kind;

	// * Types
	public final static int TYPE_INT = 0;
	public final static int TYPE_BOOLEAN = 1;
	public final static int TYPE_STRING = 2;

	// * Kinds
	public final static int INTERFACE_KIND = 0;
	public final static int LOCAL_KIND = 1;

	// a set of values as the domain
	SortedSet<T> domain;

	// ************************* Methods *****************************
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Map<T, Double> getProbDist() {
		return probDist;
	}

	public void setProbDist(Map<T, Double> probDist) {
		this.probDist = probDist;
		this.domain = new TreeSet<T>();
		this.setDomain(probDist.keySet());
	}

	public SortedSet<T> getDomain() {
		return domain;
	}

	private void setDomain(Set<T> domain) {
		TreeSet<T> mydomain = new TreeSet<>();
		for (Object o : domain) {
			mydomain.add((T) o);
		}
		this.domain = mydomain;
	}

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public Object getInitValue() {
		// TBD
		return null;
	}

	public int getKind() {
		return kind;
	}
	
	

	public void setKind(int kind) {
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "VariableType [type=" + type + ", namespace=" + namespace + ", probDist=" + probDist
				+ ", varName=" + varName + ", kind=" + kind + ", domain=" + domain + "]";
	}

}
