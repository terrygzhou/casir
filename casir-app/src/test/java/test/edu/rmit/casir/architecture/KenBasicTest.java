package test.edu.rmit.casir.architecture;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import edu.rmit.casir.architecture.GateP;
import edu.rmit.casir.architecture.GateR;
import edu.rmit.casir.architecture.KenBasic;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.util.FileHandler;
import test.edu.rmit.casir.epca.parser.VPCACompilerTest;

/**
 * 
 * @author terryzhou
 *
 */
public class KenBasicTest {
	KenBasic bKen;
	VPCA cachedEpca = null;
	String kenName = "HA";
	Logger logger = Logger.getLogger(VPCACompilerTest.class);

	VPCACompiler compiler;
	String epcaFilePath = "./casestudy/epca/hotel_agency.epca";

	@Before
	public void setUp() throws Exception {
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(this.epcaFilePath).toString();
			// logger.info(epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.compiler = new VPCACompiler(epfspStr);
		Enumeration enu = compiler.getVpcaTable().elements();
		while (enu.hasMoreElements()) {
			cachedEpca = (VPCA) enu.nextElement();
			if (cachedEpca.getProcessName().equals("HA")) {
				this.bKen = new KenBasic(this.kenName);
				bKen.setVpca(cachedEpca);
				bKen.setpGates(this.createPGates());
				bKen.setrGates(this.createRGates());
			}
		}
	}

	private Set<GateR> createRGates() {
		return new HashSet<GateR>();
	}

	private Set<GateP> createPGates() {
		Set<GateP> pGates = new HashSet<GateP>();
		GateP hotelPGate = new GateP("HotelGate");
		hotelPGate.setKen(this.bKen);
		// hotelPGate.setKen(k);
		pGates.add(hotelPGate);
		return pGates;
	}

	// *******************************************************************************************

	@Test
	public void testPrint() {
		this.bKen.print();
	}

}
