package test.edu.rmit.casir.vpca;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import edu.rmit.casir.epca.VariableType;

public class VariableTypeTest {
	VariableType<Integer> v = new VariableType<>();

	@Before
	public void setUp() throws Exception {
		v.setNamespace("P");
		Map<Integer, Double> probDist = new HashMap<>();
		probDist.put(0, 0.3);
		probDist.put(1, 0.7);
		v.setProbDist(probDist);

	}

	@Test
	public void test() {
	}

}
