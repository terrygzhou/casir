package test.edu.rmit.casir.pca;

import java.util.Vector;
import lts.CompactState;
import lts.SymbolTable;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import api.LTSA_PCA_Tool;

public class PcaLtsaTest {
	

//	String pfspExpression1 = "pca A = (?<1.0> a -> A| ?<1.0> b -> <1.0>t -> END).";
//	String pfspExpression2 = "pca B = (!<0.8> a -> <1.0>c -> B | !<0.2> a -> END).";

	String mTravelAgent = "pca M_TravelAgent = (?<1.0>checkSeat -> !<1.0>ta_checkSeat -> S1)," +
			"S1= (?<1.0>bookSeat -> !<1.0>ta_bookSeat -> ?<1.0>pay -> !<1.0>ta_pay -> M_TravelAgent " +
			"|?<1.0>checkSeat -> !<1.0>ta_checkSeat -> S1).";
	
	String rTa="pca R_TA =   (?<1.0>ta_checkSeat -> !<1.0>al_checkSeat -> S1)," +
			"S1 = (	?<1.0>ta_bookSeat -> (!<0.9>al_bookSeat -> ?<1.0>ta_pay ->  " +
			"(!<0.8>bk_checkCredit -> !<1.0>bk_pay -> R_TA| <0.2> fail -> ERROR) " +
			"| <0.1>fail -> ERROR) | ?<1.0> ta_checkSeat -> (!<0.95> al_checkSeat -> S1 | <0.05>fail -> ERROR)).";
	
	String rBank="pca R_BANK=(?<1.0>bk_checkCredit-> (<0.9>ok-> ?<1.0>bk_pay->R_BANK | <0.1>fail2 ->ERROR)).";
	
	String rAL="pca R_AL = (?<1.0>al_checkSeat -> S1),S1 = (<1.0>al_selectSeat->?<1.0>al_bookSeat->R_AL|?<1.0>al_checkSeat->S1).";
	
	String rClient ="pca Client_TravelAgent = (!<1.0>checkSeat -> S1),S1 = (!<0.4>bookSeat -> !<1.0>pay -> Client_TravelAgent| !<0.6>checkSeat -> S1).";
	
	private static Logger logger = Logger.getLogger(PcaLtsaTest.class);
	LTSA_PCA_Tool pcaTool = new LTSA_PCA_Tool();

	@Before
	public void init() {

	}

	@Test
	public void test() {
		SymbolTable.init();

		CompactState mTravelAgent = pcaTool.compile("M_TravelAgent", this.mTravelAgent);
		
		
//		logger.info("mTravelAgent's DTMC "+mTravelAgent.convertToDTMC() +"\n");
//		CompactState rTA = pcaTool.compile("R_TA", this.rTa);
//		CompactState rAL = pcaTool.compile("R_AL", this.rAL);
//		CompactState rBank = pcaTool.compile("R_BANK", this.rBank);
//		CompactState rClient = pcaTool.compile("Client_TravelAgent", this.rClient);

		// CompactState pca2=pfsp.compile("B", this.pfspExpression2);

//		logger.info("\nPCA1's DTMC\n" + pca1.convertToDTMC());

		Vector machines = new Vector(5);
		machines.add(mTravelAgent);
//		machines.add(rTA);
//		machines.add(rAL);
//		machines.add(rBank);
//		machines.add(rClient);
		

		CompactState composite = pcaTool.compose("COMPONENT", machines);
		
//		logger.info("get input actions "+composite.getInputActions().toString());
		
		String dotStr = composite.convertToGraphviz();
//		logger.info("dot file is \n" + dotStr + "\n");
		String dtmcStr = composite.convertToDTMC();
//		logger.info("\nDislpay the DTMC \n" + dtmcStr);
	}
	
}
