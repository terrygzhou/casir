package test.edu.rmit.casir.comics;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.rmit.casir.verification.CounterexampleComicsImpl;

public class CounterexampleComicsImplTest {

	String counterexampleFile = "./casestudy/thesis/furniture_maker/scenario_2/output/counter_example.path";
	String stateMapFile = "./casestudy/thesis/furniture_maker/scenario_2/output/fm_complex__reliablewood_with_err.sta";
	CounterexampleComicsImpl counterExam;
	Logger logger = Logger.getLogger(CounterexampleComicsImplTest.class);

	@Before
	public void setUp() throws Exception {
		counterExam = new CounterexampleComicsImpl(this.counterexampleFile, this.stateMapFile);
		logger.info(counterExam.getSePath());
	}

	@Test
	public void testCounterexampleComicsImpl() {
		logger.info(counterExam.getSePath());
	}

}
