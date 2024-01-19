package edu.rmit.casir.architecture;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.pca.CompositePCA;

public abstract class Ken {

	String name;
	Set<GateP> pGates = new HashSet<GateP>();
	Set<GateR> rGates = new HashSet<GateR>();

	Set<VariableType> variables;

	

	Logger logger = Logger.getLogger(edu.rmit.casir.architecture.Ken.class);

	// all gates models' composition which is ultimately a LabeledPCA
//	CompositeLPCA composedLPCA;
	
	LabeledPCA lpca;
	

	// ************************** Methods ***********************************

	public Ken(String name) {
		this.name = name;
	}

	public Ken(String name, Set<GateP> pGates, Set<GateR> rGates) {
		this.pGates = pGates;
		this.name = name;
		this.rGates = rGates;
	}

	/**
	 * From a locStateID, identify the goal state where the Ken interacts with its
	 * client via its P-Gate
	 * 
	 * @param locStateID
	 * @return
	 */
	public abstract int getAbsGoalState(int locStateID, Gate g);

	// ************** GETTER and SETTER **************************

	public String getName() {
		return this.name;
	}

	public void addPGate(GateP pGate) {
		this.pGates.add(pGate);
	}

	public void addRGate(GateR rGate) {
		if(this.getrGates()==null)
			this.setrGates(new HashSet<GateR>());
		this.rGates.add(rGate);
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<GateP> getpGates() {
		return pGates;
	}

	public void setpGates(Set<GateP> pGates) {
		this.pGates = pGates;
	}

	public Set<GateR> getrGates() {
		return rGates;
	}

	public void setrGates(Set<GateR> rGates) {
		this.rGates = rGates;
	}

//	public CompositeLPCA getComposedLPCA() {
//		return composedLPCA;
//	}
//
//	public void setComposedLPCA(CompositeLPCA composedLPCA) {
//		this.composedLPCA = composedLPCA;
//	}
	
	

	public LabeledPCA getLpca() {
		return lpca;
	}

	public Set<VariableType> getVariables() {
		return variables;
	}

	public void setVariables(Set<VariableType> variables) {
		this.variables = variables;
	}

	public void setLpca(LabeledPCA lpca) {
		this.lpca = lpca;
	}

	// ******************* Abstract Methods *******************
	// Get behavious ...
	public abstract void checkBehaviours();

	public abstract void print();

}
