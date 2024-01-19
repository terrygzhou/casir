package test.edu.rmit.casir.exp4j;

import junit.framework.Assert;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExpressionBuilderTest {
	Logger logger=Logger.getLogger(ExpressionBuilderTest.class);
	String exp="3 * sin(y) - 2 / (item - 2)";
	Expression e=null;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		this.e=new ExpressionBuilder(this.exp).variables("item","y").build();
		e.setVariable("item", 2.3);
		e.setVariable("y", 3.14);
		double result=e.evaluate();
		logger.info(result);
		Assert.assertNotNull(result);
	}

}
