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

import test.edu.rmit.casir.pca.PCATest;

import api.PFSPCompiler;

import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.ExtAction;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.Guard;
import edu.rmit.casir.epca.PFSP;
import edu.rmit.casir.epca.VarGuard;
import edu.rmit.casir.epca.VarUpdFunc;
import edu.rmit.casir.epca.VariableType;

public class EPCATest1 {
	Logger logger = Logger.getLogger(EPCATest1.class);
	VPCA epcaP = new VPCA("P");
	// assuming given the protocol
	String tempPFSPFile = "./casestudy/epca/test.pca";

	@Before
	public void setUp() throws Exception {
		CompactState tempPCA = this.getTemplatePCA();
		epcaP.setTemptlatePca(new CompactStateRef(tempPCA));
		epcaP.setAlphabet(tempPCA.getAlphabet());
		epcaP.setStates(tempPCA.states);
		epcaP.setVariables(this.createVariables());
		epcaP.setExtActions(this.createExtActions());
//		epcaP.testEPCA();
	}

	private CompactState getTemplatePCA() {
		PFSP pfsp = new PFSP(this.tempPFSPFile);
		String pProcess = "pca \n P=" + pfsp.getpFSPHash().get("P");
//		logger.info(pProcess);

		PFSPCompiler comp = new PFSPCompiler();
		CompactState pca1 = comp.compile("P", pProcess);
		
		return pca1;

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

		VariableType<Integer> v1 = new VariableType<>();
		v1.setNamespace("P");
		v1.setVarName("v1");
		Map<Integer, Double> pd3 = new TreeMap<>();
		pd3.put(0, 0.3);
		pd3.put(1, 0.7);
		v1.setProbDist(pd3);
		v1.setKind(VariableType.LOCAL_KIND);
		vars.add(v1);
		
//		VariableType<Integer> v2 = new VariableType<>();
//		v2.setNamespace("P");
//		v2.setVarName("v2");
//		Map<Integer, Double> pd4 = new TreeMap<>();
//		pd4.put(7, 0.1);
//		pd4.put(8, 0.9);
//		v2.setProbDist(pd4);
//		v2.setKind(VariableType.LOCA_VAR);
//		vars.add(v2);

		return vars;
	}

	private Set<ExtAction> createExtActions() {
		
		Set<ExtAction> extActions=new HashSet<>();
		// create ExtAction of ?<1.0>a
		ExtAction extAct = new ExtAction("a", ExtAction.TYPE_INPUT, 1.0);
		extAct.setGuardsList(this.createGuards4InputA());
		extActions.add(extAct);
		
		extAct = new ExtAction("a", ExtAction.TYPE_INPUT_FAIL, 1.0);
		extActions.add(extAct);
		
		extAct = new ExtAction("handle", ExtAction.TYPE_INTERNAL, 1.0);
		extAct.setGuardsList(this.createGuards4Handle());
		extActions.add(extAct);
		
		extAct = new ExtAction("reset", ExtAction.TYPE_INTERNAL, 0.85);
		extAct.setGuardsList(this.createGuards4Reset());
		extActions.add(extAct);
		
		extAct = new ExtAction("log", ExtAction.TYPE_INTERNAL, 0.15);
		extAct.setGuardsList(this.createGuards4Log());
		extActions.add(extAct);
		
		return extActions;
	}

	private List<Guard> createGuards4InputA() {
		List<Guard> guardSet=new ArrayList<>();
		Guard g1, g2, g3; 
		
		g1= new Guard("a::va1<1 & a::va2=true & P::v1=0");
		g1.addVarGuard(new VarGuard(epcaP.getVariable("va1"), "va1<1"));
		g1.addVarGuard(new VarGuard(epcaP.getVariable("va2"), "va2=true"));
		g1.addVarGuard(new VarGuard(epcaP.getVariable("v1"), "v1=0"));
		Set<VarUpdFunc> vu1, vu2, vu3;
		vu1=new HashSet<>();
		vu1.add(new VarUpdFunc("?a","P::v1","P::v1+1"));
		g1.setFuncs(vu1);
		guardSet.add(g1);
		
		g2=new Guard("a::va1=1 & a::va2=false & P::v1=1");
		g2.addVarGuard(new VarGuard(epcaP.getVariable("va1"), "va1=1"));
		g2.addVarGuard(new VarGuard(epcaP.getVariable("va2"), "va2=false"));
		g2.addVarGuard(new VarGuard(epcaP.getVariable("v1"), "v1=1"));
		vu2=new HashSet<>();
		vu2.add(new VarUpdFunc("?a","P::v1", "P::v1-1"));
		g2.setFuncs(vu2);
		guardSet.add(g2);
		
		g3=new Guard("else"); //"" for else 
		vu3=new HashSet<>();
		vu3.add(new VarUpdFunc("?a","P::v1", "P::v1"));
		g3.setFuncs(vu3);
		guardSet.add(g3);
		return guardSet;
	}
	
	private List<Guard> createGuards4Reset() {
		List<Guard> guardList=new ArrayList<>();
		Set<VarUpdFunc> vufSet=new HashSet<>();
		Guard g=new Guard("P::v1=0");
		g.addVarGuard(new VarGuard(epcaP.getVariable("v1"), "v1=0"));
		vufSet.add(new VarUpdFunc("reset", "P::v1", "P::v1+1"));
		g.setFuncs(vufSet);
		guardList.add(g);
		return guardList;
	}

	
	//can we assume that the actions' guardSet==null
	//means true
	private List<Guard> createGuards4Log() {
		List<Guard> guardList=new ArrayList<>();
		Set<VarUpdFunc> vufSet=new HashSet<>();
		Guard g=new Guard("true");
		vufSet.add(new VarUpdFunc("log", "P::v1", "P::v1"));
		g.setFuncs(vufSet);
		guardList.add(g);
		return guardList;
	}

	
	private List<Guard> createGuards4Handle() {
		List<Guard> guardList=new ArrayList<>();
		Set<VarUpdFunc> vuf1=new HashSet<>();
		Guard g1, g2; 
		g1=new Guard("P::v1=1");
		g1.addVarGuard(new VarGuard(this.epcaP.getVariable("v1"),"v1=1"));
		vuf1.add(new VarUpdFunc("handle", "P::v1", "P::v1-1"));
		g1.setFuncs(vuf1);
		guardList.add(g1);
		
		g2=new Guard("else");
//		g2.addVarGuard(null);
		Set<VarUpdFunc> vuf2=new HashSet<>();
		vuf2=new HashSet<>();
		vuf2.add(new VarUpdFunc("handle", "P::v1", "P::v1+1"));
		g2.setFuncs(vuf2);
		guardList.add(g2);
		return guardList;
	}
	
	
	public String getUnfoldPFSP(){
		return epcaP.unfoldPfsp();
	}

//	@Test
	public void testUnfoldfsp() {
		String epca_unfold_fsp=this.getUnfoldPFSP();
		epcaP.testExtStates();
		logger.info(epca_unfold_fsp);
		
//		String unfoldPFSP = "pca \n P=" + uPfsp.getpFSPHash().get("P");
//		PFSPCompiler comp = new PFSPCompiler();
//		CompactState uPca = comp.compile("P", epca_unfold_fsp);
//		CompactStateReflect.printPCA(uPca);
//		logger.info(uPca.convertToGraphviz());
//		CompactStateReflect csr=new CompactStateReflect(uPca);
//		csr.setEpca(epcaP);
//		csr.setExtLTS(epcaP.getExtLTS());
//		logger.info(csr.getStateMap());
	}

	@Test
	public void testCompactState(){
//		CompactState cs=epcaP.getUnfoldingPCA();
		CompactState cs=epcaP.getUnfoldedLabeledPCA().getPcaObj();
		
		logger.info(cs);
	}
	
//	@Test
	public void testStateMap(){
		logger.info(epcaP.unfoldPfsp());
//		Map<Integer, ExtState> sm=epcaP.getStateIDToExtStateMap();
		Map<Integer, ExtState> sm=epcaP.getStateIDToExtStateMap();
		for(int i: sm.keySet()){
			logger.info(i+"\t"+sm.get(i).getExtStateLabel());
		}
	}
	
	
	
	
	public VPCA getEpcaP() {
		return epcaP;
	}

	public void setEpcaP(VPCA epcaP) {
		this.epcaP = epcaP;
	}

	
	
}
