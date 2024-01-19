package test.edu.rmit.casir.vpca;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import lts.CompactState;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.PFSPCompiler;

import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.ExtAction;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.Guard;
import edu.rmit.casir.epca.PFSP;
import edu.rmit.casir.epca.VarGuard;
import edu.rmit.casir.epca.VarUpdFunc;
import edu.rmit.casir.epca.VariableType;

public class EPCATest2 {
	Logger logger = Logger.getLogger(EPCATest2.class);
	VPCA epcaQ = new VPCA("Q");
	// assuming given the protocol
	String tempPFSPFile = "./casestudy/epca/test.pca";

	@Before
	public void setUp() throws Exception {
		CompactState tempPCA = this.getTemplatePCA();
		epcaQ.setTemptlatePca(new CompactStateRef(tempPCA));
		epcaQ.setAlphabet(tempPCA.getAlphabet());
		epcaQ.setStates(tempPCA.states);
		epcaQ.setVariables(this.createVariables());
		epcaQ.setExtActions(this.createExtActions());
	}

	private CompactState getTemplatePCA() {
		PFSP pfsp = new PFSP(this.tempPFSPFile);
		String pProcess = "pca \n P=" + pfsp.getpFSPHash().get("P");
		// logger.info(pProcess);
		String qProcess = "pca \n Q=" + pfsp.getpFSPHash().get("Q");
		// logger.info(qProcess);

		PFSPCompiler comp = new PFSPCompiler();
		// CompactState pca1 = comp.compile("P", pProcess);
		CompactState pca2 = comp.compile("Q", qProcess); // skip

		return pca2;
	}

	private Set<VariableType> createVariables() {
		Set<VariableType> vars = new HashSet<>();
		VariableType<Integer> va1 = new VariableType<>();
		va1.setNamespace("a");
		va1.setVarName("va1");
		Map<Integer, Double> pd = new TreeMap<>();
		pd.put(0, 0.3);
		pd.put(1, 0.7);
		va1.setProbDist(pd);
		va1.setKind(VariableType.INTERFACE_KIND);
		vars.add(va1);

		VariableType<Boolean> va2 = new VariableType<>();
		va2.setNamespace("a");
		va2.setVarName("va2");
		Map<Boolean, Double> pd2 = new TreeMap<>();
		pd2.put(true, 0.2);
		pd2.put(false, 0.8);
		va2.setProbDist(pd2);
		va2.setKind(VariableType.INTERFACE_KIND);
		vars.add(va2);

		VariableType<Integer> v2 = new VariableType<>();
		v2.setNamespace("Q");
		v2.setVarName("v2");
		Map<Integer, Double> pd3 = new TreeMap<>();
		pd3.put(0, 0.2);
		pd3.put(1, 0.6);
		pd3.put(2, 0.2);
		v2.setProbDist(pd3);
		v2.setKind(VariableType.LOCAL_KIND);
		vars.add(v2);

		return vars;
	}

	private Set<ExtAction> createExtActions() {

		Set<ExtAction> extActions = new HashSet<>();

		ExtAction extAct = new ExtAction("a", ExtAction.TYPE_ONPUT, 0.2);
		extAct.setGuardsList(this.createGuards4OutputA1());
		extActions.add(extAct);

		extAct = new ExtAction("a", ExtAction.TYPE_ONPUT, 0.7);
		extAct.setGuardsList(this.createGuards4OutputA2());
		extActions.add(extAct);

		extAct = new ExtAction("a", ExtAction.TYPE_ONPUT_FAIL, 0.1);
		extActions.add(extAct);

		extAct = new ExtAction("b", ExtAction.TYPE_INTERNAL, 1.0);
		extAct.setGuardsList(this.createGuards4b());
		extActions.add(extAct);

		return extActions;
	}

	// !<0.7>a
	private List<Guard> createGuards4OutputA2() {
		return null;
	}

	// !<0.2>a, Q::v2
	private List<Guard> createGuards4OutputA1() {
		List<Guard> guardSet = new ArrayList<>();
		Guard g1, g3;

		g1 = new Guard("a::va1=0 & Q::v2>0");
		g1.addVarGuard(new VarGuard(epcaQ.getVariable("va1"), "va1=0"));
		g1.addVarGuard(new VarGuard(epcaQ.getVariable("v2"), "v2>0"));
		Set<VarUpdFunc> vu1, vu3;
		vu1 = new HashSet<>();
		vu1.add(new VarUpdFunc("!a", "Q::v2", "Q::v2-1"));
		g1.setFuncs(vu1);
		guardSet.add(g1);

		g3 = new Guard("else"); // "" for else
		vu3 = new HashSet<>();
		vu3.add(new VarUpdFunc("!a", "Q::v2", "Q::v2"));
		g3.setFuncs(vu3);
		guardSet.add(g3);

		return guardSet;
	}

	// can we assume that the actions' guardSet==null
	// means true
	private List<Guard> createGuards4b() {
		List<Guard> guardList = new ArrayList<>();
		Set<VarUpdFunc> vufSet, vu;
		vufSet = new HashSet<>();
		Guard g, g2;
		g = new Guard("Q::v2<2");
		g.addVarGuard(new VarGuard(epcaQ.getVariable("v2"), "v2<2"));
		vufSet.add(new VarUpdFunc("b", "Q::v2", "Q::v2+1"));
		g.setFuncs(vufSet);
		guardList.add(g);

		g2 = new Guard("else"); // "" for else
		vu = new HashSet<>();
		vu.add(new VarUpdFunc("!a", "Q::v2", "Q::v2"));
		g2.setFuncs(vu);
		guardList.add(g2);
		return guardList;
	}


	public String getUnfoldPFSP(){
		return epcaQ.unfoldPfsp();
	}
	
	@Test
	public void test() {
		 String epca_fsp = this.getUnfoldPFSP();
		 logger.info(epca_fsp);
//		 epcaQ.testExtStates();
	}

//	@Test
	public void testStateMap(){
		logger.info(epcaQ.unfoldPfsp());
//		Map<Integer, ExtState> sm=epcaQ.getStateIDToExtStateMap();
		Map<Integer, ExtState> sm=epcaQ.getStateIDToExtStateMap();

		for(int i: sm.keySet()){
			logger.info(i+"\t"+sm.get(i).getExtStateLabel());
		}
	}
	
}
