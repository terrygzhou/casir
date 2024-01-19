package test.edu.rmit.casir.vpca;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.javatuples.Triplet;
import org.junit.Before;
import org.junit.Test;

import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.VariableType;

public class ExtStateTest {
	Logger logger = Logger.getLogger(ExtStateTest.class);
	ExtState es = null;
	VariableType va1, va2, v1;

	@Before
	public void setUp() throws Exception {
		Map<String, Map<Object, Double>> vars = new TreeMap<String, Map<Object, Double>>();
		this.addVariable();
		vars.put(this.va1.getVarName(), this.va1.getProbDist());
		vars.put(this.va2.getVarName(), this.va2.getProbDist());
		vars.put(this.v1.getVarName(), this.v1.getProbDist());
		this.es = new ExtState("MyState", 0, vars);
	}

	private void addVariable() {
		VariableType<Integer> va1 = new VariableType<>();
		va1.setNamespace("a");
		va1.setVarName("va1");
		Map<Integer, Double> pd = new TreeMap<Integer, Double>();
		pd.put(0, 0.4);
		pd.put(1, 0.6);
		va1.setProbDist(pd);
		va1.setKind(VariableType.INTERFACE_KIND);
		this.va1 = va1;

		VariableType<Boolean> va2 = new VariableType<>();
		va2.setNamespace("a");
		va2.setVarName("va2");
		Map<Boolean, Double> pd2 = new TreeMap<>();
		pd2.put(true, 0.8);
		pd2.put(false, 0.2);
		va2.setProbDist(pd2);
		va2.setKind(VariableType.INTERFACE_KIND);
		this.va2 = va2;

		VariableType<Integer> v1 = new VariableType<>();
		v1.setNamespace("P");
		v1.setVarName("v1");
		Map<Integer, Double> pd3 = new TreeMap<>();
		pd3.put(0, 0.3);
		pd3.put(1, 0.7);
		v1.setProbDist(pd3);
		v1.setKind(VariableType.LOCAL_KIND);
		this.v1 = v1;

	}

	@Test
	public void testGetVarValues() {
		List<List<Triplet<String, Object, Double>>> allVarValues = VPCA.cartesianProductdValues(this.es.getLocVarValueMap());
		allVarValues.forEach(list -> {
			logger.debug("____________________________");
			list.forEach(tri -> {
				logger.debug(".......................");
				logger.debug(tri.getValue0());
				logger.debug(tri.getValue1().toString());
				// logger.debug(tri.getValue2());
			});
		});
		// logger.info(allVarValues);

	}

}
