package test.edu.rmit.casir.radl;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import au.edu.rmit.dsea.openradl.Model;
import au.edu.rmit.dsea.openradl.ADNs.Ken;
import au.edu.rmit.dsea.openradl.ADNs.KenBasicImpl;
import au.edu.rmit.dsea.openradl.sca.ImporterImpl;

public class TestRadlScaImporter {

	Set<Ken> mainKens=null;
	Logger logger = Logger.getLogger(TestRadlScaImporter.class);
	private String scaPath = "./casestudy/AbstractSample";
	private String scaName = "RootA.composite";
	ImporterImpl scaImport = new ImporterImpl();
	
	
	@Before
	public void setUp() {
		Model radl=null;
		try {
			radl = scaImport.generate(scaPath, scaName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.mainKens=radl.getKens();
	}

	
	/**
	 * test a composite ken that has SCA implemantation based components,
	 * and associate the composites together via Kens models
	 */
	@Test
	public void testCompositeKenExplore(){
		
	}
	
	
	
	@Test
	public void testBasicKen() {
		for (Ken k: this.mainKens){
			logger.info(k.getName());
			logger.info("k is a basicKen? "+ (k instanceof KenBasicImpl));
		}
	}

	
	
	@After
	public void settle() {

	}

}
