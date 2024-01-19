package test.edu.rmit.casir.pca;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import api.PFSPCompiler;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.pca.CompositePCA;
import lts.CompactState;
import lts.EventState;
import lts.SymbolTable;

public class CompositePCATest {
	CompositePCA composite;
	static Logger logger=Logger.getLogger(CompositePCATest.class); 
	CompactState pca1, pca2;

	@Before
	public void setUp() throws Exception {
		SymbolTable.init();
		PFSPCompiler comp = new PFSPCompiler();
		String pfspExpression1 = "pca \n\nA=(!<0.8>a-><1.0>do->A |~!<0.2>a ->ERROR).";
		String pfspExpression2 = "pca \n\n B=(?<1.0>a -> <1.0>ok->B|~?<1.0>a -> <1.0>handle ->B).";
		this.pca1 = comp.compile("A", pfspExpression1);
		this.pca2 = comp.compile("B", pfspExpression2);
		Vector<CompactState> machines = new Vector<>(2);
		machines.add(pca1);
		machines.add(pca2);
		CompactState com = comp.compose("C", machines);
		CompactStateRef comRef = new CompactStateRef(com);
		Vector<CompactStateRef> locPcaRefs = new Vector<>(2);
		locPcaRefs.addElement(new CompactStateRef(pca1));
		locPcaRefs.addElement(new CompactStateRef(pca2));
		composite = new CompositePCA(comRef, locPcaRefs);
	}
	
	
	@Test
	public void testStates() {
		Vector<CompactState> machines = new Vector<>(1);
		machines.add(this.pca2);
		PFSPCompiler comp = new PFSPCompiler();

		CompactState localPCA=comp.compose("A", machines);
		APITest.printPCA(localPCA);
		EventState[] states=localPCA.states;
		for(int i=0; i<states.length;i++) {
			logger.info(states[i]);
		}
	}
	

	@Test
	public void testDotGraph(){
		logger.info(composite.getComposite().getPcaObj().convertToGraphviz());
	}
	
	
//	@Test
	public void testDTMC(){
		logger.info(composite.convertPCAToDTMC());
	}
	
	
	@Test
	public void test() {
		logger.info(composite.buildGloLocStateMap());
	}

}
