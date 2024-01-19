package edu.rmit.casir.architecture;

import java.util.Set;
import edu.rmit.casir.architecture.Ken;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.pca.CompositePCA;

public class KenComposite extends Ken {

	Set<Ken> subKens;

	Set<Binding> bindings;

	// The compositeKen's behaviour composed from SubKens behaviour
	CompositeLPCA compositeLPCA;

	// ********************* Methods *******************************************

	public KenComposite(String name, Set<GateP> pGates, Set<GateR> rGates) {
		super(name, pGates, rGates);
	}

	@Override
	public void checkBehaviours() {
		// TODO Auto-generated method stub
	}

	@Override
	public void print() {
		// TODO Auto-generated method stub
	}

	@Override
	public int getAbsGoalState(int locStateID, Gate g) {
		// TODO Auto-generated method stub
		return 0;
	}

	// ***********************GETTER and SETTER*********************************

	public void setSubKens(Set<Ken> subKens) {

	}

	public Set<Binding> getBindings() {
		return null;
	}

	public void addSubKen(Ken ken) {

	};

	public void addBinding(Binding b) {

	}

}
