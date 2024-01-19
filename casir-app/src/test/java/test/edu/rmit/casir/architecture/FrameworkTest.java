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
import edu.rmit.casir.epca.parser.VPCACompiler;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.FileHandler;

public class FrameworkTest {

	Framework framework;

	Configuration conf;
	String name = "bookshop2";
	String epcaFilePath = "./casestudy/epca/" + name + ".epca";
	Logger logger = Logger.getLogger(FrameworkTest.class);

	@Before
	public void setUp() throws Exception {
		Hashtable<String, VPCA> epcaTable;
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
		Set<Ken> kens = new HashSet<>();
		Set<Binding> bindings = new HashSet<>();
		epcaTable.forEach((k, epca) -> {
			KenBasic component = new KenBasic(k);
			component.setVpca(epca);
			kens.add(component);
			Vector<LabeledPCA> localLPCAVec = new Vector<>();
			localLPCAVec.add(epca.getUnfoldedLabeledPCA());
			component.setLpca(new CompositeLPCA(localLPCAVec).getGloLPCA());
		});
		Map<String, Gate> gateMap = new HashMap<>();
		kens.forEach(ken -> {
			logger.info(ken.getName());
			if (ken.getName().equals("BUYER")) {
				GateR r = new GateR("RGate_BUYER");
				r.setKen(ken);
				ken.addRGate(r);
				gateMap.put("RGate_BUYER", r);
			} else {
				GateP p = new GateP("PGate_SHOP");
				p.setKen(ken);
				ken.addPGate(p);
				gateMap.put("PGate_SHOP", p);
			}
		});

		Binding b = new Binding("binding_shopping", gateMap.get("RGate_BUYER"),
				gateMap.get("PGate_SHOP"));
		bindings.add(b);
		this.conf = new Configuration(kens, bindings);
		this.framework = new Framework();
	}

	@Test
	public void testLocaliseInconsistency() {
		Map<String, Integer> localisedStatesID = this.framework.localiseInconsistency(this.conf, epcaFilePath, 0);
		localisedStatesID.forEach((name, localStateID) -> {
			logger.info(name);
			logger.info(localStateID);
			// logger.info(eState.getAbsStateID());
			// logger.info(eState.getExtStateLabel());
		});
	}

//	@Test
	public void testLocalRecovery() {
		Ken k = this.conf.getKenByName("SHOP");
		this.framework.recovery(conf, k, 4);

	}

}
