package edu.rmit.casir.architecture;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.ExtTransition;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.pca.CompositePCA;
import lts.Transition;

public class KenBasic extends Ken {

	/**
	 * The behaviour is initially given by a vPFSP file Do we need vPCA to describe
	 * the behaviour or just relying on the lpca?
	 */
	VPCA vpca;

	/**
	 * composition of all gates, yet to be implemented
	 */

	Logger logger = Logger.getLogger(edu.rmit.casir.architecture.KenBasic.class);

	// ***************************** methods *************************************

	public KenBasic(String name) {
		super(name);
	}

	public KenBasic(String name, Set<GateP> pGates, Set<GateR> rGates) {
		super(name, pGates, rGates);
	}

	@Override
	public void checkBehaviours() {

	}

	/**
	 * Given a localAbsStateID, find the goal abstract state of the Ken from which
	 * next interface transitions in PGate g start. For testing, it should return an
	 * abstract state.
	 */
	@Override
	public int getAbsGoalState(int locAbsStateID, Gate g) {

		// * should this state be part of the strongest evidence path?

		VPCA vpca = this.getVpca();
		LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();
		CompactStateRef templatePCA = vpca.getTemptlatePca();
		logger.debug(templatePCA.getLTS());

		/**
		 * find next state in the template PCA where the interface transitions within
		 * the P-Gate after locStateID
		 */

		Map<Integer, Set<Transition>> lts = templatePCA.getLTS();
		int currentState = locAbsStateID;
		int absGoal = locAbsStateID;
		boolean found = false;
		LinkedList<Integer> stack = new LinkedList<>();
		stack.addFirst(currentState);

//		while (!stack.isEmpty()) {
//			currentState = stack.removeFirst();
//			Set<Transition> outTrans = lts.get(currentState);
//			/**
//			 * if any outTran belongs to the Ken's PGate (or R-Gate), then the outTran's
//			 * source state is the goal-state, advanced algorithm is to to be developed
//			 */
//			for (Transition tran : outTrans) {
//				String label = tran.getLabel();
//				// if the (P)gate has the label
//				if (g.getSymbols().contains(label)) {
//					absGoal = currentState;
//					found = true;
//					break;
//				}
//				stack.addFirst(tran.getTo());
//			}
//			if (found)
//				break;
//		}
//		if (!found) {
//			// throw new Exception("no found the goal state");
//			logger.fatal("No found the goal state!");
//		}
		// Gate pGate = (Gate) this.getpGates().toArray()[0];
		// Set<ExtTransition> nextTrans =
		// this.findNextTransitions(estate.getExtStateLabel(), g);
		// Set<ExtTransition> nextTrans = null;
		return absGoal;
		// return this.getCommAbsGoalState(nextTrans);
	}

	/**
	 * Find next immediate interface transitions (in Gate g) after the state
	 * represented by locStateLabel.
	 * 
	 * @param locStateLabel
	 * @param g
	 * @return
	 */
	private Set<ExtTransition> findNextTransitions(String locStateLabel, Gate g) {
		Map<String, Set<ExtTransition>> extLTS = this.vpca.getExtLTS();
		// ExtState initExtState=this.epca.getExtStateByStateID(localState);
		extLTS.forEach((locLabel, trans) -> {
			trans.forEach(t -> {
				if (this.isMemberOfTransition(t.getExtEventLabel(), (GateP) g))
					logger.info(t.getExtEventLabel());
			});
		});

		return null;
	}

	/**
	 * return the closest common abstract state of a set of transitions
	 * 
	 * @param trans
	 * @return
	 */
	private int getCommAbsGoalState(Set<ExtTransition> trans) {
		return 0;
	}

	/**
	 * check if the given transition is a member of the pGate's symbol
	 * 
	 * @param tranLabel
	 * @param pGate
	 * @return
	 */
	private boolean isMemberOfTransition(String tranLabel, GateP pGate) {

		return false;
	}

	// ***************************** GETTER and SETTER *************************

	public VPCA getVpca() {
		return vpca;
	}

	public void setVpca(VPCA vpca) {
		this.vpca = vpca;
	}

	// public CompositeLPCA getComposedLPCA() {
	// return composedLPCA;
	// }
	//
	// public void setComposedLPCA(CompositeLPCA composedLPCA) {
	// this.composedLPCA = composedLPCA;
	// }

	@Override
	public void print() {
		logger.info(
				"******************** PRINT BasicKen " + this.getName() + "  *****************");
		logger.info("Printing the ken's unfolded PFSP: \n" + this.getVpca().unfoldPfsp());

		logger.info("Printing the ken's dot figure: \n" + this.getLpca().getGraphvizDot());

		for (GateP p : this.getpGates()) {
			p.print();
			logger.info(p.getName() + "\t" + p.getKind());
		}
		for (GateR r : this.getrGates()) {
			r.print();
			logger.info(r.getName() + " type is \t" + r.getKind());
		}
		logger.info("*********************** END PRINT	***************");

	}

}
