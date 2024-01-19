package test.edu.rmit.casir.pca;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import lts.ActionName;
import lts.CompactState;
import lts.EventState;
import lts.Pair;
import lts.Relation;
import lts.SymbolTable;

import org.apache.log4j.Logger;
import org.javatuples.Triplet;
import org.junit.Before;
import org.junit.Test;

import api.PFSPCompiler;
import edu.rmit.casir.epca.CompactStateRef;

public class PCATest {
	static Logger logger = Logger.getLogger("test.edu.rmit.casir.pca.PcaTest");
	CompactState composite = null;
	CompactState pca1, pca2;
	PFSPCompiler comp = new PFSPCompiler();

	@Before
	public void setUp() {
		SymbolTable.init();

		// String pfspExpression1 = "pca \n\nA=(!<0.8>a-><1.0>do->A |~!<0.2>a
		// ->ERROR).";
		String pfspExpression1 = "pca P=(?<1.0>a->P1|~?<1.0>a-><1.0>handle->P),P1=(<0.85>reset->P|<0.15>log->P1).";
		String pfspExpression2 = "pca \n\n B=(?<1.0>a -> <1.0>ok->B|~?<1.0>a -> <1.0>handle ->END).";

		this.pca1 = comp.compile("P", pfspExpression1);
		this.pca1.getAlphabet();
		pca2 = comp.compile("B", pfspExpression2);
//		Vector<CompactState> machines = new Vector<>(2);
//		machines.add(pca1);
//		machines.add(pca2);
//		composite = comp.compose("C", machines);
	}

	@Test
	public void test() {
		// logger.info(composite.convertToGraphviz());
		// rebuildLocalPCA(this.composite, this.pca1, this.pca2);
		 this.printPCA(this.pca2);
//		CompactStateRef csr=new CompactStateRef(this.pca1);
//		logger.info(csr.convertPCAToDTMC());
	}
	
	

	 @Test
	public void testDisplayComposite() {
		printPCA(composite);
	}

	public static void printPCA(CompactState pca) {
		StringBuffer testSB = new StringBuffer();
		PrintStream output = System.out;
		output.println("Process:");
		output.println("\t" + pca.name);

		output.println("States:");
		output.println("\t" + pca.maxStates);
		output.println("Transitions:");
		output.println("\t" + pca.name + " = Q0,");
		for (int i = 0; i < pca.maxStates; ++i) {
			output.print("\tQ" + i + "\t= ");
			EventState current = pca.states[i];
			if (current == null) {
				if (i == pca.getEndseq())
					output.print("END");
				else
					output.print("STOP");
				if (i < pca.maxStates - 1)
					output.println(",");
				else
					output.println(".");
			} else {
				output.print("(");
				while (current != null) {
					ActionName action = current.getEventPCA();
					String label = action.getLabel();
					String type = action.getTypeString();
					Pair<Integer, Integer> machine = action.getSynchronisedMachines();
					boolean syn = action.isSynchronised();
					boolean inter = action.isInterfaceAction();
					boolean loc = action.isLocalAction();
					testSB.append(label + ":" + loc + "\t");

					output.print(action.getTypeString() + "<" + action.getProbability() + "> "
							+ action.getLabel() + " -> ");

					if (current.getNext() < 0)
						output.print("ERROR");
					else
						output.print("Q" + current.getNext());
					current = current.getList();
					if (current == null)
						if (i < pca.maxStates - 1)
							output.println("),");
						else
							output.println(").");
					else
						output.print("\n\t\t  |");
				}
			}
		}
		logger.info(testSB);
	}

	// @Test
	public void testRelabel() {
		Relation oldtonew = new Relation();
		oldtonew.put("c", "b");
		pca1.relabel(oldtonew);
		logger.info(pca1.convertToGraphviz());
		Vector machines = new Vector(2);
		machines.add(pca1);
		machines.add(pca2);
		PFSPCompiler comp = new PFSPCompiler();
		composite = comp.compose("C", machines);
		logger.info(composite.convertToGraphviz());
		composite.relabel(oldtonew);
		logger.info(composite.convertToGraphviz());
	}

	/**
	 * associate the global state in a composite with its local components'
	 * local states
	 * @deprecated
	 * @param pca
	 * @param p1
	 * @param p2
	 */
	private void rebuildLocalPCA(CompactState pca, CompactState p1, CompactState p2) {
		Map<Integer, Vector> combinedStates = new HashMap<>();
		Vector<Integer> initComState = new Vector();
		PrintStream output = System.out;
		output.println("Process: \t" + pca.name);
		output.println("States: \t" + pca.maxStates);
		output.println("Transitions: \t" + pca.name + " = Q0,");
		// assuming the initial states are always from 0
		initComState.add(0);
		initComState.add(0);
		initComState.add(0);
		combinedStates.put(0, initComState);

		Vector<CompactState> pcas = new Vector();
		pcas.add(p1);
		pcas.add(p2);

		for (int i = 0; i < pca.maxStates; ++i) {
			output.print("\tQ" + i + "\t= ");

			EventState current = pca.states[i];
			// get global state by "i" from the combinedStates set
			// triple currentCombinedState=this.getCombinedState(i);
			// current = currentCombinedState.getValue0.index;

			Vector<Integer> currentComState = combinedStates.get(i);
			int locState = 0;
			Vector<Integer> locStateIDs = new Vector();
			for (int j = 0; j < 2; j++) {
				locState = currentComState.get(i);
				locStateIDs.add(locState);
			}

			if (current == null) {
				if (i == pca.getEndseq())
					output.print("END");
				else
					output.print("STOP");
				if (i < pca.maxStates - 1)
					output.println(",");
				else
					output.println(".");
			} else {
				output.print("(");

				// for each output states from state[i]
				while (current != null) {
					ActionName action = current.getEventPCA();
					int tgtLocState;
					// find out the component where label is from.
					// based on the triple, to locate the target local state of
					// that component
					// update the states and create a new combinedState, save it
					// in the set
					Pair<Integer, ActionName> matchedPcaAction = this.findMatchedPcaAction(locStateIDs, action, pcas);
					int index = matchedPcaAction.getFirst();
					ActionName ma = matchedPcaAction.getSecond();

					tgtLocState = this.getTargetState(locState, ma, pcas.get(index));

					output.print(action.getTypeString() + "<" + action.getProbability() + "> "
							+ action.getLabel() + " -> ");

					Vector<Integer> tgtComState = null; //Triplet.with(current.getNext(), tgtLocState);

					combinedStates.put(current.getNext(), tgtComState);
					if (current.getNext() < 0)
						output.print("ERROR");
					else
						output.print("Q" + current.getNext());
					current = current.getList();
					if (current == null)
						if (i < pca.maxStates - 1)
							output.println("),");
						else
							output.println(").");
					else
						output.print("\n\t\t  |");
				} // while for one source state
			} // else
			logger.info(combinedStates);
		}
	}

	/**
	 * Given a source state and action and PCA
	 * 
	 * @return the target state ID
	 */
	private int getTargetState(int locState, ActionName action, CompactState pca) {
		String actionLabel = action.getLabel();
		EventState current = null;
		for (int i = 0; i < pca.maxStates; ++i) {
			if (locState == i) {
				current = pca.states[i];
				while (current != null) {
					ActionName itAction = current.getEventPCA();
					if (actionLabel.equals(itAction.getLabel())
							&& action.getTypeString().equals(itAction.getTypeString()))
						return current.getNext();
					current = current.getList();
				}
			}
		}
		return -3;

	}

	/**
	 * @deprecated
	 * @param states
	 * @param globalID
	 * @return
	 */
	private Triplet<String, String, String> getCombinedState(Set<Triplet> states, String globalID) {
		for (Triplet<String, String, String> t : states) {
			String gid = t.getValue0();
			if (globalID.equals(gid))
				return t;
		}
		return null;
	}

	/**
	 * Given a source state ID, check if the action is the output action from
	 * that state
	 * 
	 * @param stateID
	 * @param pcaObj
	 * @return Map<processID, action>
	 */
	private Pair<Integer, ActionName> findMatchedPcaAction(Vector<Integer> locStateIDs,
			ActionName globalAction, Vector<CompactState> pcas) {
		String label = globalAction.getLabel();
		String type = globalAction.getTypeString();
		double prob = globalAction.getProbability();

		// if not close, then exit
		if (!type.equals("")) {
			// that would be !, ?, ~!, ~?
			if (globalAction.isFailureAction()) {
				// would be ~! or ~?
			}
			// only need find one action from pcas
			// not closed PCA, and exit exceptionally.
			System.exit(-1);
		}

		// for a local action that can be: 1)a local action, 2) a
		// composition from ?a and !a or 3) a composition from ~!a and ~?a
		Map<Integer, Set<ActionName>> locActMap = new HashMap<>();

		// for each local state
		for (int i = 0; i < locStateIDs.size(); i++) {
			int locStateID = locStateIDs.get(i);
			CompactState pca = pcas.get(i);
			EventState locEventState = pca.states[locStateID];
			Set<ActionName> locActions = new HashSet<>();

			while (locEventState != null) {
				ActionName locAct = locEventState.getEventPCA();
				if (!locAct.getLabel().equals(globalAction.getLabel()))
					continue;
				String locActType = locAct.getTypeString();
				if (locActType.equals("")) {
					// lock this action and this pca, leaving others unchanged,
					// and return

				} else { // if this local action is synchronised
					locActions.add(locAct);
					// need find the paired action from another PCA
				}
				locEventState = locEventState.getList();
			} // while

			locActMap.put(i, locActions);
		} // for

		// sort out the locActMap
		logger.info(locActMap);

		for (int i = 0; i < locActMap.size(); i++) {
			Set<ActionName> acts = locActMap.get(i);
			for (ActionName a : acts) {
				Pair<Integer, ActionName> pair = this.findCorrespondAction(i, a, locActMap);
				ActionName matchAct = pair.getSecond();
				double p = matchAct.getProbability();
				double pp = p * a.getProbability();
				if (pp == prob) {
					return pair;
				}
			}
		}

		return null;
	}

	private Pair<Integer, ActionName> findCorrespondAction(int excludeID, ActionName matchAct,
			Map<Integer, Set<ActionName>> actMap) {
		String type = matchAct.getTypeString();
		String label = matchAct.getLabel();
		for (int i = 0; i < actMap.size(); i++) {
			if (i == excludeID)
				continue;
			Set<ActionName> restActs = actMap.get(i);
			for (ActionName a : restActs) {
				String restActLabel = a.getLabel();
				String restType = a.getTypeString();
				if (label.equals(restActLabel) && (isTypeMatch(type, restType)))
					return new Pair(i, a);
			}
		}
		return null;
	}

	private boolean isTypeMatch(String type1, String type2) {
		if (type1.equals("?") && type2.equals("!"))
			return true;
		if (type1.equals("!") && type2.equals("?"))
			return true;
		if (type1.equals("~?") && type2.equals("~!"))
			return true;
		if (type1.equals("~!") && type2.equals("~?"))
			return true;
		return false;

	}

}
