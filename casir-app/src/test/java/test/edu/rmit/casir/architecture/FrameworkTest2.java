package test.edu.rmit.casir.architecture;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import edu.rmit.casir.architecture.Binding;
import edu.rmit.casir.architecture.Configuration;
import edu.rmit.casir.architecture.Framework;
import edu.rmit.casir.architecture.Gate;
import edu.rmit.casir.architecture.GateP;
import edu.rmit.casir.architecture.GateR;
import edu.rmit.casir.architecture.Ken;
import edu.rmit.casir.architecture.KenBasic;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.CompositeVPCA;
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.FileHandler;

public class FrameworkTest2 {

	Framework framework;

	Configuration conf;
	String name = "TravelAgent_6";
	String epcaFilePath = "./casestudy/epca/" + name + ".epca";
	// debug
	CompositeVPCA comVPCA;

	Logger logger = Logger.getLogger(FrameworkTest2.class);

	@Before
	public void setUp() throws Exception {
		Hashtable<String, VPCA> epcaTable;
		VPCACompiler vPCACompiler;
		String epfspStr = null;
		try {
			epfspStr = FileHandler.readFileToSB(epcaFilePath).toString();
			// logger.info("reading EPCA-FSP:" + "\n" + epfspStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		vPCACompiler = new VPCACompiler(epfspStr);
		epcaTable = vPCACompiler.getVpcaTable();
		Set<Ken> kens = new HashSet<>();
		Set<Binding> bindings = new HashSet<>();

		/**
		 * for debugging..
		 */
		Vector<VPCA> vpcaVec = new Vector<>();

		epcaTable.forEach((k, epca) -> {
			KenBasic component = new KenBasic(k);
			component.setVpca(epca);
			kens.add(component);
			Vector<LabeledPCA> localLPCAVec = new Vector<>();
			localLPCAVec.add(epca.getUnfoldedLabeledPCA());
			component.setLpca(new CompositeLPCA(localLPCAVec).getGloLPCA());
			vpcaVec.add(epca);
		});

		/**
		 * debugining...
		 */
		comVPCA = new CompositeVPCA("ALL", vpcaVec);
		logger.debug(comVPCA.getComposedLPCA().getDTMC());

		Map<String, Gate> gateMap = new HashMap<>();
		for (Ken ken : kens) {
			logger.info(ken.getName());
			switch (ken.getName()) {

			case "CLIENT":
				GateR r = new GateR("RGate_CLIENT");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("RGate_CLIENT", r);
				break;

			case "FC":
				GateP p = new GateP("PGate_FC");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("PGate_FC", p);
				GateR r1 = new GateR("RGate_FC_DELTA");
				r1.setKen(ken);
				GateR r2 = new GateR("RGATE_FC_QANTAS");
				r2.setKen(ken);
				ken.addRGate(r1);
				ken.addRGate(r2);
				gateMap.put("RGate_FC_DELTA", r1);
				gateMap.put("RGATE_FC_QANTAS", r2);

				break;

			case "DELTA":
				p = new GateP("PGate_DELTA");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("PGate_DELTA", p);
				break;

			case "QANTAS":
				p = new GateP("PGate_QANTAS");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("PGate_QANTAS", p);
				break;
			}
		}

		Binding client_binding = new Binding("client_flight", gateMap.get("RGate_CLIENT"),
				gateMap.get("PGate_FC"));

		Binding delta_binding = new Binding("delta_flight", gateMap.get("RGate_FC_DELTA"),
				gateMap.get("PGate_DELTA"));

		Binding qantas_binding = new Binding("qantas_flight", gateMap.get("RGATE_FC_QANTAS"),
				gateMap.get("PGate_QANTAS"));

		bindings.add(client_binding);
		bindings.add(delta_binding);
		bindings.add(qantas_binding);

		this.conf = new Configuration(kens, bindings);
		this.framework=new Framework();
	}

//	@Test
	public void testLocaliseInconsistency() {
		this.conf.print();
		Map<String, Integer> localisedStatesID = this.framework.localiseInconsistency(this.conf, epcaFilePath, 0);
		
		localisedStatesID.forEach((name, localStateID) -> {
			logger.info(name);
			logger.info(localStateID);
			// logger.info(eState.getAbsStateID());
			// logger.info(eState.getExtStateLabel());
		});
	}

	 @Test
	public void testLocalRecovery() {
		Ken k = this.conf.getKenByName("FC");
		this.framework.recovery(conf, k, 4);
	}

}
