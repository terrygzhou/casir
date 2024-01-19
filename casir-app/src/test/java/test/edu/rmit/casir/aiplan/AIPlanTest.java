package test.edu.rmit.casir.aiplan;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ai.planning.propositional.Problem;
import ai.planning.propositional.SequentialPlan;
import ai.planning.propositional.ssp.ForwardSearchPlanner;

public class AIPlanTest {
	
	ForwardSearchPlanner planner;
	Problem problem;
	@Before
	public void setUp() throws Exception {
		this.planner=new ForwardSearchPlanner(this.problem);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		SequentialPlan plan=this.planner.doPlanSearch();
	}

}
