package test.edu.rmit.casir.lpca;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;


import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.FileHandler;

public class CompositeLPCATest {

	// LabeledPCA are generated from EPCA
	Hashtable<String, VPCA> epcaTable;
	Vector<VPCA> epcas = new Vector<>();
	Logger logger=Logger.getLogger(CompositeLPCATest.class);
	Vector<LabeledPCA> lpcaVec=new Vector<>();
	CompositeVPCA comEPCA = null;
//	String name = "bookshop2";
	String name = "error_unhandling";

	
	String epcaFilePath = "./casestudy/epca/" + name + ".epca";

	CompositeLPCA compositeLPCA;

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
		epcaTable = compiler.getVpcaTable();
		epcaTable.forEach((k, v) -> {
			this.epcas.add(v);
			this.lpcaVec.add(v.getUnfoldedLabeledPCA());
		});
//		this.compositeLPCA = new CompositeLPCA();
	}

	
	
	
	@Test
	public void testPrint() {
		for(LabeledPCA lpca:lpcaVec) {
			logger.info(lpca.getGraphvizDot());
		}
		this.compositeLPCA = new CompositeLPCA(this.lpcaVec);
		this.compositeLPCA.print();
		
	}
	
	

}
