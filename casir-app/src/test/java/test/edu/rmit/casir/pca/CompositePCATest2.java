package test.edu.rmit.casir.pca;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.APITest;
import api.PFSPCompiler;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.pca.CompositePCA;
import edu.rmit.casir.util.FileHandler;
import lts.CompactState;
import lts.SymbolTable;

public class CompositePCATest2 {

	CompositePCA composite;
	static Logger logger = Logger.getLogger(CompositePCATest2.class);

	Vector<CompactState> machines = new Vector<>();

//	String name = "TravelAgent_6";
	 String name ="eshop_hs5";

	String epcaFilePath = "./casestudy/epca/" + name + ".epca";
	String unfoldedPcaPath = "./casestudy/epca/output/" + name + ".pca";
	String composteDotPath = "./casestudy/epca/output/" + name + ".dot";

	@Before
	public void setUp() throws Exception {

		VPCACompiler compiler;
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(epcaFilePath).toString();
			// logger.info("reading EPCA-FSP:" + "\n" + epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		compiler = new VPCACompiler(epfspStr);

		Hashtable<String, VPCA> epcaTable = compiler.getVpcaTable();
		Vector<CompactStateRef> locPcaRefs = new Vector<>();
		
		Vector<VPCA> vpcaVec=new Vector<>();
		FileHandler.delFile(unfoldedPcaPath);
		epcaTable.forEach((k, v) -> {
			vpcaVec.add(v);
			CompactState pca = v.getUnfoldedLabeledPCA().getPcaObj();
			CompactStateRef csr = new CompactStateRef(pca);

			String vpfspStr = v.unfoldPfsp();
			logger.info(vpfspStr);
			this.machines.addElement(pca);
			locPcaRefs.addElement(csr);

			// logger.info(csr.outputLTS());
			String dotPath = "./casestudy/epca/output/" + v.getProcessName() + ".dot";

			// logger.info(csr.getPcaObj().convertToGraphviz());
			FileHandler.delFile(dotPath);

			try {
				FileHandler.appendPathFile(dotPath, new StringBuffer(csr.convertToGraphviz()));
				FileHandler.appendPathFile(unfoldedPcaPath, new StringBuffer("\n"+vpfspStr));

			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		CompositeVPCA comVPCA=new CompositeVPCA("ALL",vpcaVec);
		
		logger.info(comVPCA.getComposedLPCA().getDTMC());

		SymbolTable.init();
		PFSPCompiler comp = new PFSPCompiler();

		CompactState com = comp.compose("ALL", machines);
		
//		APITest.printPCA(com);
//		logger.info(com.convertToDTMC());
//		logger.info(com.convertToGraphviz());
		
		CompactStateRef comRef = new CompactStateRef(com);

		/**
		 * debugging...
		 */
		logger.debug(comRef.getLTS());
		composite = new CompositePCA(comRef, locPcaRefs);
		
		FileHandler.delFile(composteDotPath);
		
		try {
			FileHandler.appendPathFile(composteDotPath,
					new StringBuffer(comRef.convertToGraphviz()));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	
	
	
	@Test
	public void testComposition() {
		this.composite.getComposite().getLTS().forEach((k, v) -> {
			logger.info(k);
		});

		logger.info(composite.buildGloLocStateMap());
//		logger.info(composite.buildGloLocStateMap());
		
	}

//	 @Test
	public void testConvertPCAToDTMC() {
		logger.info(composite.convertPCAToDTMC());
	}

}
