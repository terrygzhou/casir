package test.edu.rmit.casir.vpca;

import java.util.ArrayList;
import java.util.HashMap;
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
import edu.rmit.casir.epca.ExtAction;
import edu.rmit.casir.epca.Guard;
import edu.rmit.casir.epca.PFSP;
import edu.rmit.casir.epca.VarGuard;
import edu.rmit.casir.epca.VarUpdFunc;
import edu.rmit.casir.epca.VariableType;

public class OrderCheckCaseTest {
	Logger logger = Logger.getLogger(OrderCheckCaseTest.class);
	VPCA epcaP = new VPCA("Client");
	VPCA epcaS = new VPCA("Shop");
	// assuming given the protocol
	String tempPFSPFile = "./casestudy/epca/ordercheck.pca";

	@Before
	public void setUp() throws Exception {
		CompactState tempClient = this.getTemplatePCA().get("Client");
//		epcaP.setTemptlatePca(tempClient);
		epcaP.setAlphabet(tempClient.getAlphabet());
		epcaP.setStates(tempClient.states);
		epcaP.setVariables(this.createVariables4Client());
		epcaP.setExtActions(this.createExtActions());
		
		CompactState tempShop = this.getTemplatePCA().get("Shop");
//		this.epcaS.setTemptlatePca(tempShop);
		epcaS.setAlphabet(tempShop.getAlphabet());
		epcaS.setStates(tempShop.states);
		epcaS.setVariables(this.createVariables4Shop());
		epcaS.setExtActions(this.createExtActions4Shop());
		
	}
	
	
private Set<ExtAction> createExtActions4Shop() {
		
		Set<ExtAction> extActions=new HashSet<>();
		// create ExtAction of ?<1.0>a
		ExtAction extAct = new ExtAction("check", ExtAction.TYPE_INPUT, 1.0);
//		extAct.setGuardsList(this.createGuards4OutputCheck());
		extActions.add(extAct);
		
		extAct = new ExtAction("order", ExtAction.TYPE_INPUT, 1.0);
//		extAct.setGuardsList(this.createGuards4OutputOrder());
		extActions.add(extAct);
		
		return extActions;
	}

	private Set<VariableType> createVariables4Shop(){
		Set<VariableType> vars = new HashSet<>();
		VariableType<Integer> item = new VariableType<>();
		item.setNamespace("check");
		item.setVarName("item");
		Map<Integer, Double> pd = new TreeMap<>();
		pd.put(0, 0.2);
		pd.put(1, 0.3);
		pd.put(2, 0.5);
		item.setProbDist(pd);
		item.setKind(VariableType.INTERFACE_KIND);
		vars.add(item);
		return vars;
	}

	private Map<String, CompactState> getTemplatePCA() {
		Map<String, CompactState> tempPCAMap=new HashMap<>();
		PFSP pfsp = new PFSP(this.tempPFSPFile);
		String pProcess = "pca \n Client=" + pfsp.getpFSPHash().get("Client");
		String shopProcess = "pca \n Shop=" + pfsp.getpFSPHash().get("Shop");

		PFSPCompiler comp = new PFSPCompiler();
		CompactState pca1 = comp.compile("Client", pProcess);
		tempPCAMap.put("Client", pca1);
		CompactState pca2 = comp.compile("Shop", shopProcess);
		tempPCAMap.put("Shop", pca2);
		
		return tempPCAMap;

	}

	private Set<VariableType> createVariables4Client() {
		Set<VariableType> vars = new HashSet<>();
		VariableType<Integer> item = new VariableType<>();
		item.setNamespace("check");
		item.setVarName("item");
		Map<Integer, Double> pd = new TreeMap<>();
		pd.put(0, 0.2);
		pd.put(1, 0.3);
		pd.put(2, 0.5);
		item.setProbDist(pd);
		item.setKind(VariableType.INTERFACE_KIND);
		vars.add(item);

		VariableType<Integer> avail = new VariableType<>();
		avail.setNamespace("Client");
		avail.setVarName("avail");
		Map<Integer, Double> pd2 = new TreeMap<>();
		pd2.put(0, 1.0);
		pd2.put(1, 0.0);
		avail.setProbDist(pd2);
		avail.setKind(VariableType.LOCAL_KIND);
		vars.add(avail);

		VariableType<Integer> succ = new VariableType<>();
		succ.setNamespace("Client");
		succ.setVarName("succ");
		Map<Integer, Double> pd3 = new TreeMap<>();
		pd3.put(0, 1.0);
		pd3.put(1, 0.0);
		succ.setProbDist(pd3);
		succ.setKind(VariableType.LOCAL_KIND);
		vars.add(succ);
		
		return vars;
	}

	private Set<ExtAction> createExtActions() {
		
		Set<ExtAction> extActions=new HashSet<>();
		// create ExtAction of ?<1.0>a
		ExtAction extAct = new ExtAction("check", ExtAction.TYPE_ONPUT, 1.0);
		extAct.setGuardsList(this.createGuards4OutputCheck());
		extActions.add(extAct);
		
		extAct = new ExtAction("back", ExtAction.TYPE_INTERNAL, 0.5);
		extActions.add(extAct);
		
		extAct = new ExtAction("order", ExtAction.TYPE_ONPUT, 0.5);
		extAct.setGuardsList(this.createGuards4OutputOrder());
		extActions.add(extAct);
		
		extAct = new ExtAction("done", ExtAction.TYPE_INTERNAL, 1.0);
		extAct.setGuardsList(this.createGuards4Done());
		extActions.add(extAct);
		
		return extActions;
	}

	private List<Guard> createGuards4OutputCheck() {
		List<Guard> guardSet=new ArrayList<>();
		Guard g1, g3; 
		Set<VarUpdFunc> vu1, vu3;
		
		g1= new Guard("check::item>0");
		g1.addVarGuard(this.createVarGuard(epcaP.getVariable("item"), "item>0"));
		vu1=new HashSet<>();
		vu1.add(new VarUpdFunc("!check","Client::avail","1"));
		g1.setFuncs(vu1);
		guardSet.add(g1);
		
		g3=new Guard("else"); //"" for else 
		g3.addVarGuard(this.createVarGuard(epcaP.getVariable("item"), "item=0"));
		vu3=new HashSet<>();
		vu3.add(new VarUpdFunc("!check","Client::avail", "0"));
		g3.setFuncs(vu3);
		guardSet.add(g3);
		
		return guardSet;
	}
	
	private List<Guard> createGuards4Done() {
		List<Guard> guardList=new ArrayList<>();
		Set<VarUpdFunc> vufSet=new HashSet<>();
		Guard g=new Guard("avail>0");
		g.addVarGuard(new VarGuard(this.epcaP.getVariable("avail"), "avail=1"));
		vufSet.add(new VarUpdFunc("done", "Client::succ", "1"));
		g.setFuncs(vufSet);
		guardList.add(g);

		Guard g3;
		Set<VarUpdFunc> vu3;
		g3=new Guard("else"); //"" for else
		g3.addVarGuard(new VarGuard(this.epcaP.getVariable("avail"), "avail=0"));
		vu3=new HashSet<>();
		vu3.add(new VarUpdFunc("done","Client::succ", "0"));
		g3.setFuncs(vu3);
		guardList.add(g3);
		
		return guardList;
	}
	
	
	private List<Guard> createGuards4OutputOrder() {
		List<Guard> guardList=new ArrayList<>();
		Set<VarUpdFunc> vuf1=new HashSet<>();
		Guard g1, g2; 
		g1=new Guard("Client::avail>0");
		g1.addVarGuard(new VarGuard(this.epcaP.getVariable("avail"),"avail=0"));
		vuf1.add(new VarUpdFunc("!order", "Client::avail", "0"));
		g1.setFuncs(vuf1);
		guardList.add(g1);
		
		g2=new Guard("else");
		g2.addVarGuard(new VarGuard(this.epcaP.getVariable("avail"),"avail=1"));
		Set<VarUpdFunc> vuf2=new HashSet<>();
		vuf2=new HashSet<>();
		vuf2.add(new VarUpdFunc("!order", "Client::avail", "1"));
		g2.setFuncs(vuf2);
		guardList.add(g2);
		return guardList;
	}

	

	private VarGuard createVarGuard(VariableType v, String expression) {
		VarGuard vg = new VarGuard(v, expression);
		return vg;
	}
	
	
	@Test
	public void test() {
		String client_unfold_fsp=epcaP.unfoldPfsp();
		String shop_unfold_fsp=epcaS.unfoldPfsp();
		logger.info(client_unfold_fsp);
		logger.info(shop_unfold_fsp);
		
//		PFSPCompiler comp = new PFSPCompiler();
//		CompactState uPca = comp.compile("Client", client_unfold_fsp);
//		logger.info(uPca.convertToGraphviz());
	}

}
