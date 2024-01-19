package test.edu.rmit.casir.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.util.CollectionUtil;

public class CollectionUtilTest {
	Logger logger = Logger.getLogger(CollectionUtilTest.class);
	VariableType va1, va2, v1;

	@Before
	public void setUp() throws Exception {
	}

//	@Test
	public void test() {
		List<List<Object>> lists = new ArrayList<>();
		List<Object> names = new ArrayList<>();
		names.add("TERRY");
		names.add("Jimmy");
		names.add("C");
		lists.add(names);
		List<Object> prob = new ArrayList<>();
		prob.add(0.32);
		prob.add(0.45);

		lists.add(prob);
		lists.add(names);
		List<List<Object>> result = CollectionUtil.cartesianProduct(lists);
		logger.info(result);
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

//	@Test
	public void testExtStateVar() {
		Map<String, Map<Object, Double>> vars = new TreeMap<String, Map<Object, Double>>();
		this.addVariable();
		Map value=new HashMap<>();
		value.put("HA", 0.99);
		vars.put("HA", value);
	}

}
