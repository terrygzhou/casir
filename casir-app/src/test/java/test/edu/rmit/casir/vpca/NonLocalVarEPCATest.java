package test.edu.rmit.casir.vpca;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
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
import edu.rmit.casir.epca.PFSP;
import edu.rmit.casir.epca.VariableType;

public class NonLocalVarEPCATest {
	Logger logger = Logger.getLogger(NonLocalVarEPCATest.class);
	VPCA epcaS = new VPCA("Shop");
	// assuming given the protocol
	String tempPFSPFile = "./casestudy/epca/ordercheck.pca";

	@Before
	public void setUp() throws Exception {
		CompactState tempShop = this.getTemplatePCA().get("Shop");
//		this.epcaS.setTemptlatePca(tempShop);
		epcaS.setAlphabet(tempShop.getAlphabet());
		epcaS.setStates(tempShop.states);
		epcaS.setVariables(this.createVariables4Shop());
		epcaS.setExtActions(this.createExtActions4Shop());
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
	
	
	@Test
	public void test() {
		String shop_unfold_fsp=epcaS.unfoldPfsp();
		logger.info(shop_unfold_fsp);
	}

}
