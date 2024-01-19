package edu.rmit.casir.architecture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.epca.ExtState;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.verification.Counterexample;
import edu.rmit.casir.verification.CounterexampleComicsImpl;
import edu.rmit.casir.verification.PctlProperty;

/**
 * Architecture configuration
 * <p>
 * Important: for simplicity, the thesis only considers one P-Gate for each Ken,
 * so that the P-Gate model is same as the component's model.
 * 
 * @author terryzhou
 *
 */
public class Configuration {

	Logger logger = Logger.getLogger(edu.rmit.casir.architecture.Configuration.class);
	// A set of components
	Set<Ken> kens;

	// the configuration name
	// String name;

	// A set of bindings
	Set<Binding> bindings;

	PctlProperty pctlProperty;

	// global model consisting of all Kens that is for verification and recovery
	CompositeLPCA gloModel;

	// ["time", <"request",4>,<"ack",0.5>],["dollar", <"request",24>,<"ack",0.5>]
	Map<String, Map<String, Double>> costMap = new HashMap<>();

	// ************************** Methods **********************************

	/**
	 * Constructor, the bindings specifying source and target Gates already
	 * determine the two bound Kens
	 * 
	 * @param kens
	 * @param bindings
	 */
	public Configuration(Set<Ken> kens, Set<Binding> bindings) {
		// this.name = name;
		this.kens = kens;
		this.bindings = bindings;
	}

	public Configuration(Vector<Ken> kens, Set<Binding> bindings) {
		// this.name = name;

		this.kens = new HashSet<Ken>();
		kens.forEach(k -> {
			this.kens.add(k);
		});

		this.bindings = bindings;
	}

	/**
	 * Find the formula relevant components (for the future recovery)
	 * 
	 * @param prop
	 * @return
	 */
	public Set<Ken> getFormulaRelevantKens(String propName) {
		PctlProperty prop = this.getPctlProperty();
		Set<Ken> relevantKens = new HashSet<>();
		Set<VariableType> formulaVars = prop.getVariables(propName);
		Set<String> varName = new HashSet<>();
		formulaVars.forEach(fv -> {
			varName.add(fv.getNamespace() + "_" + fv.getVarName());
		});
		for (Ken k : this.getKens()) {
			Set<VariableType> kenVars = k.getVariables();
			for (VariableType vt : kenVars) {
				String fullVarName = vt.getNamespace() + "_" + vt.getVarName();
				if (varName.contains(fullVarName))
					relevantKens.add(k);
			}
		}
		return relevantKens;
	}

	/**
	 * Reason about the global model by shuffling the bound Kens
	 * 
	 * @return
	 */
	private CompositeLPCA generateGlobalModel() {
		Vector<LabeledPCA> lpcaVec = new Vector<>();
		this.getAvailableKens().forEach(ken -> {
			KenBasic basicKen = (KenBasic) ken;
			LabeledPCA lpca = basicKen.getLpca();
			lpcaVec.add(lpca);
		});
		CompositeLPCA model = new CompositeLPCA(lpcaVec);
		this.setGloModel(model);
		return this.gloModel;
	}

	/**
	 * Find available Kens for composition based on the available bindings in the
	 * Configuration
	 * 
	 * @return
	 */
	public Set<Ken> getAvailableKens() {
		Set<Ken> availKens = new HashSet<Ken>();

		for (Binding b : this.bindings) {

			logger.debug(
					b.getName() + "\t" + b.getSource().getName() + "\t" + b.getTarget().getName());
			Ken k = b.getSource().getKen();
			logger.debug(k.getName());
			if (!this.isIncluded(availKens, k))
				availKens.add(k);
			k = b.getTarget().getKen();
			logger.debug(k.getName());
			if (!this.isIncluded(availKens, k))
				availKens.add(k);
		}
		// this.bindings.forEach(b -> {
		// availKens.add(b.getSource().getKen());
		// availKens.add(b.getTarget().getKen());
		// });
		availKens.forEach(k->{
			logger.debug(k.getName());
		});
		return availKens;
	}

	/**
	 * check if kk is already included in kenSet
	 * 
	 * @param kenSet
	 * @param kk
	 * @return
	 */
	private boolean isIncluded(Set<Ken> kenSet, Ken kk) {
		Set<String> kenNames = new HashSet<>();
		kenSet.forEach(k -> {
			kenNames.add(k.getName());
		});
		if (kenNames.contains(kk))
			return true;
		return false;
	}

	/**
	 * The integer in the strongest evidence is the unfolded state id output by
	 * PRISM, the state needs be mapped to the global state
	 * 
	 * @deprecated
	 * @return counterexample
	 */
	public Vector<Integer> getStrongestEvidence() {
		Vector<Integer> strongEvidence = new Vector<Integer>();
		String prismDtmc = null;
		try {
			// prismDtmc = this.getGloModel().getDTMC();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// *export the dtmc to the state-transition model .tra file

		// *convert the .tra to dtmc for the input of comics

		// *output the counter_example path, and pick up the most probable one
		// (Expensive!)

		// 0->440->441->429->308->294->117->8->9->35->36->37->38->39->40->41->47->48->27->26->25->460
		// (Prob: 2.72831e-05)

		// by mapping with the state model exported from PRISM, the corresponding states
		// are the below:
		// 440:(435,false,0,1,0,0,0,false,false)
		// 435:(436,false,0,1,0,0,0,false,false)
		// 427:(424,false,0,1,0,0,0,false,false)
		// 312:(303,false,0,1,0,0,0,false,false)
		// 294:(289,false,0,1,0,0,0,false,false)
		// 117:(113,false,0,1,0,0,0,false,false)
		// 8:(8,false,0,1,1,0,0,false,false) // order_placed
		// 9:(9,false,0,1,1,0,0,false,false) //fm_order_wood
		// 35:(33,false,0,1,1,0,0,false,false) //order_wood_order_wood__amount_1
		// 36:(34,false,0,1,0,0,0,false,false) //wood_confirm_wood_confirm__amount_1
		// 37:(35,false,0,1,0,1,0,false,false) // resupply_resupply__amount_1, resupply
		// makes it inconsistent again
		// 38:(36,false,0,1,1,1,0,false,false) //wood_succs
		// 39:(37,false,0,1,1,1,0,false,false) //fm_order_steel
		// 40:(38,false,0,1,1,1,0,false,false) //order_steel
		// 41:(39,false,0,1,1,1,0,false,false) //steel_confirm
		// 47:(45,false,0,1,1,1,1,false,false) //success
		// 48:(46,false,0,1,1,1,1,false,false) //fm_end
		// 27:(26,false,0,1,1,1,1,false,false) //make_furniture
		// 26:(25,false,0,1,1,1,1,false,false) //furniture_delivered
		// 25:(24,false,0,1,1,1,1,false,false)
		strongEvidence.add(0, 0);
		strongEvidence.add(1, 435);
		strongEvidence.add(2, 436);
		strongEvidence.add(3, 424);
		strongEvidence.add(4, 303);
		strongEvidence.add(5, 289);
		strongEvidence.add(6, 113);
		strongEvidence.add(7, 8);
		strongEvidence.add(8, 9);
		strongEvidence.add(9, 33);
		strongEvidence.add(10, 34);
		strongEvidence.add(11, 35);
		strongEvidence.add(12, 36);
		strongEvidence.add(13, 37);
		strongEvidence.add(14, 38);
		strongEvidence.add(15, 39);
		strongEvidence.add(16, 45);
		strongEvidence.add(17, 46);
		strongEvidence.add(18, 26);
		strongEvidence.add(19, 25);

		return strongEvidence;
	}

	/**
	 * This is to visualise this architecture Configuration
	 */
	public void print() {
		logger.info("PRINTING Configuration ......");
		for (Binding b : this.bindings) {

			Gate sourceGate = b.getSource();
			Gate targetGate = b.getTarget();
			logger.debug(b.getName());
			logger.debug(sourceGate.getName());
			Ken sKen = sourceGate.getKen();
			Ken tKen = targetGate.getKen();
			logger.info("Ken " + sKen.getName() + "::" + sourceGate.getName() + " is bound to Ken "
					+ tKen.getName() + "::" + targetGate.getName() + " via Binding " + b.getName());

			sKen.print();
			tKen.print();
			sourceGate.print();
			targetGate.print();
		}

		// Testing the behaviour as follows
		logger.info(this.kens.size());
		logger.info(this.getAvailableKens().size());

		// testing global model
		CompositeLPCA gloModel = this.getGloModel();
		try {
			logger.info("Printing the global model's interpreted DTMC as follows:");
			logger.info(gloModel.getDTMC());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Given a Ken (KenBasic), identify its client Kens
	 * 
	 * @param ken
	 * @return
	 */
	public Vector<Ken> getClientKen(Ken ken) {
		Vector<Ken> clients = new Vector<Ken>();
		this.bindings.forEach(b -> {
			Gate tGate = b.getTarget();
			if (ken.getpGates().contains(tGate)) {
				clients.add(tGate.getKen());
			}
		});
		return clients;
	}

	/**
	 * Given an initial and target Goal ExtState, planning recovery model (paths) on
	 * Ken between them.
	 * <p>
	 * Return a list of CompositeLPCA that will be concatenated in Ken's LabeledPCA
	 * between the init and goal ExtStates
	 * 
	 * @param initExtState
	 * @param ken
	 * @param goalExtState
	 */
	public Vector<CompositeLPCA> recoveryPlan(ExtState initExtState, Ken ken,
			ExtState goalExtState) {
		/**
		 * return what?
		 */
		return new Vector<CompositeLPCA>();
	}

	/**
	 * place the recovery CompositeLPCA generated by recoveryPlan() into ken's
	 * LabeledPCA between initState and goalState
	 * 
	 * @param rec
	 * @param initalState
	 * @param goalState
	 * @param ken
	 * @return
	 */
	public LabeledPCA getTransformedLPCA(CompositeLPCA rec, ExtState initState, ExtState goalState,
			Ken ken) {

		return null;
	}

	// *************** SETTER and GETTER *******************************
	/**
	 * @param gloModel
	 */
	public void setGloModel(CompositeLPCA gloModel) {
		this.gloModel = gloModel;
	}

	public CompositeLPCA getGloModel() {
		if (this.gloModel == null) {
			logger.debug("Building the global model");
			this.gloModel = this.generateGlobalModel();
		}
		return gloModel;
	}

	public Set<Ken> getKens() {
		return kens;
	}

	public void setKens(Set<Ken> kens) {
		this.kens = kens;
	}

	public Ken getKenByName(String name) {
		// this.getKens().stream().filter(k->k.getName().equals(name)).map(k::ken);
		Ken result = null;
		// this.getKens().forEach(k->{
		// if(k.getName().equals(name))
		// result=k;
		// });
		for (Ken k : this.getKens()) {
			if (k.getName().equals(name))
				result = k;
		}
		return result;
	}

	public Set<Binding> getBindings() {
		return bindings;
	}

	public void setBindings(Set<Binding> bindings) {
		this.bindings = bindings;
	}

	public PctlProperty getPctlProperty() {
		return pctlProperty;
	}

	public void setPctlProperty(PctlProperty pctlProperty) {
		this.pctlProperty = pctlProperty;
	}

	public Map<String, Map<String, Double>> getCostMap() {
		return costMap;
	}

	public void setCostMap(Map<String, Map<String, Double>> costMap) {
		this.costMap = costMap;
	}

}
