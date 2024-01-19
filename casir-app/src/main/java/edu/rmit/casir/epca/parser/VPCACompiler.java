package edu.rmit.casir.epca.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import lts.ActionName;
import lts.CompactState;
import lts.EventState;
import lts.SymbolTable;

import org.apache.log4j.Logger;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import api.APITest;
import api.PFSPCompiler;
import api.PFSPExpressionEncoder;
import edu.rmit.casir.epca.VPCA;
import edu.rmit.casir.epca.CompactStateRef;
import edu.rmit.casir.epca.ExtAction;
import edu.rmit.casir.epca.Guard;
import edu.rmit.casir.epca.PFSP;
import edu.rmit.casir.epca.VarGuard;
import edu.rmit.casir.epca.VarUpdFunc;
import edu.rmit.casir.epca.VariableInstance;
import edu.rmit.casir.epca.VariableType;
import edu.rmit.casir.pca.PCAUtil;
import edu.rmit.casir.util.GeneralUtil;

public class VPCACompiler {

	EpcaLex lex;

	private EpcaSymbol current;

	// Map<namespace::variableLabel, VariableType> vars
	Map<String, VariableType> vars = new HashMap<>();

	// Map<actiontype<varprob>actionLabel, List<VariableInstance>>
	Map<String, List<VariableInstance>> varInstances = new HashMap<>();

	// capture user specified var-value prob distribution
	// Map<key="actType+<prob>+actLabel", value=List<VariableInstance>>
	// Map<String, List<VariableInstance>> instnaceValues = new HashMap<>();

	// * for caching instance variables values when parsing EPCA actions
	Map<String, Map<String, Vector<Pair<Object, Double>>>> cachedValue = new HashMap<>();

	private Hashtable<String, VPCA> vpcaTable = new Hashtable<>();

	// Map<processID, pcafsp> templatePCAMap
	Map<String, String> templatePCAMap = new HashMap<>();

	Logger logger = Logger.getLogger(VPCACompiler.class);

	// **************************** Method *************************

	public VPCACompiler(String expression) {
		EpcaSymbolTable.init();
		this.compile(expression);
	}

	/**
	 * Given a string of pfsp, compile to an EPCA object
	 * 
	 * @param processName
	 * @param pfspExpression
	 * @return
	 */
	public void compile(String epfspExpression) {
		PFSPExpressionEncoder encoder = new PFSPExpressionEncoder(epfspExpression);
		this.lex = new EpcaLex(encoder);
		try {
			this.doparse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Lexer based parsing
	 * 
	 * @throws Exception
	 */
	private void doparse() throws Exception {
		next_symbol();
		if (this.current.kind == 301) {
			next_symbol();
		}

		while (this.current.kind != 99) {
			// ----------EPCA element extension begins---------
			if (this.current.kind == 302) {
				VariableType vt = this.variableDefinition();
				this.vars.put(vt.getNamespace() + "::" + vt.getVarName(), vt);
				logger.debug(vt.toString());
			} else if (this.current.kind == EpcaSymbol.UPPERIDENT) {
				String processName = this.current.toString();
				VPCA ep = this.parseEPCA(processName);
				vpcaTable.put(processName, ep);
				// associate vars with all epca processes.
			}
			// ----------EPCA element extension completes---------

			if (this.current.kind == 1) {
				next_symbol();
				// constantDefinition(Expression.constants);
			} else if (this.current.kind == 3) {
				next_symbol();
				// rangeDefinition();
			} else if (this.current.kind == 9) {
				next_symbol();
				// setDefinition();
			} else if (this.current.kind == 10) {
				next_symbol();
				// progressDefinition();
			} else if ((this.current.kind == 45) || (this.current.kind == 15)
					|| (this.current.kind == 16) || (this.current.kind == 2)
					|| (this.current.kind == 17) || (this.current.kind == 28)) {
				boolean makeDet = false;
				boolean makeMin = false;
				boolean makeProp = false;
				boolean makeComp = false;
				boolean makeControl = false;
				if (this.current.kind == 15) {
					makeDet = true;
					next_symbol();
				}
				if (this.current.kind == 16) {
					makeMin = true;
					next_symbol();
				}
				if (this.current.kind == 17) {
					makeComp = true;
					next_symbol();
				}
				if (this.current.kind == 2) {
					makeProp = true;
					next_symbol();
				}
				if (this.current.kind == 28) {
					makeControl = true;
					next_symbol();
				}
				if (this.current.kind != 45) { // if not ||
				} else if (this.current.kind == 45) { // if '||'
				}
			}
			next_symbol();
		}
	}

	/**
	 * Define an EPCA process, including extract/parse the template PCA
	 * 
	 * @throws Exception
	 */
	private VPCA parseEPCA(String processName) throws Exception {

		String processID = processName;
		VPCA vpca = new VPCA(processID);
		Set<ExtAction> extActions = new HashSet<>();
		// p.setExtActions(extActions);
		this.setExtActions(extActions, vpca);

		LinkedList<String> pcaBuff = new LinkedList<>();
		pcaBuff.addLast("pca\n");
		pcaBuff.addLast(processID);

		StringBuffer buff = new StringBuffer();
		boolean failFlag = false;
		boolean skip = true;
		boolean trueCondition = false;
		ExtAction previousExtAct = null;
		Guard preGuard = null;
		while (this.current.kind != EpcaSymbol.DOT) {
			if (skip)
				next_symbol();
			skip = true;
			String sym = this.current.toString();
			// logger.info(sym);
			buff.append(sym);

			switch (this.current.kind) {
			case EpcaSymbol.SINE:// if ~, failure transition
				failFlag = true;
				// pcaBuff.addLast(this.current.toString());
				trueCondition = false;
				break;
			case EpcaSymbol.QUESTION: // if ? action
				ExtAction act = null;
				Triplet<ExtAction, Double, Boolean> triple = null;
				double vProb = 1;
				double prodProb = 1;
				if (failFlag) {
					failFlag = false;
					// sort out the ~? action
					triple = this.createExtAction(ExtAction.TYPE_INPUT_FAIL, skip);

				} else {
					// sort out ? action
					triple = this.createExtAction(ExtAction.TYPE_INPUT, skip);
				}
				act = triple.getValue0();
				skip = triple.getValue2();
				vProb = triple.getValue1();

				prodProb = vProb * act.getProbability();
				// prodProb = act.getProbability();

				previousExtAct = act;
				vpca.getExtActions().add(act);

				// pcaBuff.addLast(this.current.toString());
				pcaBuff.addLast(act.getActionType());
				pcaBuff.addLast("<" + prodProb + ">");
				pcaBuff.addLast(act.getActionLabel());
				trueCondition = false;
				break;
			case EpcaSymbol.PLING: // if ! action
				act = null;
				triple = null;
				if (failFlag) {
					failFlag = false;
					// sort out the ~! action
					triple = this.createExtAction(ExtAction.TYPE_ONPUT_FAIL, skip);

				} else {
					// sort out ! action
					triple = this.createExtAction(ExtAction.TYPE_ONPUT, skip);

				}
				act = triple.getValue0();
				skip = triple.getValue2();
				vProb = triple.getValue1();
				previousExtAct = act;
				vpca.getExtActions().add(act);
				prodProb = vProb * act.getProbability();
				// prodProb = act.getProbability();

				// pcaBuff.addLast(this.current.toString());
				pcaBuff.addLast(act.getActionType());
				pcaBuff.addLast("<" + prodProb + ">");
				pcaBuff.addLast(act.getActionLabel());
				trueCondition = false;
				break;

			case EpcaSymbol.LESS_THAN: // for <
				// this should be the internal action
				act = null;
				triple = null;
				if (failFlag) {
					failFlag = false;
					triple = this.createExtAction(ExtAction.TYPE_INTERNAL_FAIL, skip);

				} else {
					triple = this.createExtAction(ExtAction.TYPE_INTERNAL, skip);
				}
				act = triple.getValue0();
				vProb = triple.getValue1();
				skip = triple.getValue2();
				prodProb = vProb * act.getProbability();
				// prodProb = act.getProbability();

				previousExtAct = act;
				vpca.getExtActions().add(act);
				pcaBuff.addLast(act.getActionType());
				// the probability may be recalculated for variables
				pcaBuff.addLast("<" + prodProb + ">");
				pcaBuff.addLast(act.getActionLabel());
				trueCondition = false;
				break;

			case EpcaSymbol.IF:
			case EpcaSymbol.ELSE_IF:
				ExtAction currentExtAct = previousExtAct;
				// set guard list for the currentExtAct;...
				String varGuardLabel = "";
				String guardLabel = "";
				String fullname = "";
				Guard g = new Guard(guardLabel);

				while (this.current.kind != EpcaSymbol.THEN) {
					if (this.current.kind == 123 || this.current.kind == 124) { // variable
						fullname = this.current.toString();
						next_symbol();
						fullname = fullname + this.current.toString();
						next_symbol();
						fullname = fullname + this.current.toString();
						varGuardLabel = "";
						// guardLabel=this.current.toString();
						while (this.current.kind != EpcaSymbol.RROUND
								&& this.current.kind != EpcaSymbol.BITWISE_AND) {
							varGuardLabel = varGuardLabel + this.current.toString();
							next_symbol();
						}
						if (guardLabel.length() > 0)
							guardLabel = guardLabel + " & " + varGuardLabel;
						else
							guardLabel = varGuardLabel;
						logger.debug(guardLabel);
						// g.setGuardLabel(varGuardLabel);
						VarGuard vg = new VarGuard(this.vars.get(fullname), varGuardLabel);
						logger.debug(vg.getgLabel() + "\t" + vg.getVar().getVarName() + "\t"
								+ vg.getSubDomain());
						g.addVarGuard(vg);

					}
					next_symbol();
					// grab all guards
				}
				g.setGuardLabel(guardLabel);

				if (currentExtAct.getGuardsList() == null) {
					List<Guard> guardList = new LinkedList<>();
					currentExtAct.setGuardsList(guardList);
				}
				currentExtAct.getGuardsList().add(g);
				preGuard = g;
				skip = false;
				trueCondition = false;
				break;

			case EpcaSymbol.THEN:
				currentExtAct = previousExtAct;
				String actTypeLabel = currentExtAct.getActionType()
						+ currentExtAct.getActionLabel();
				Guard currentGuard = preGuard;
				Set<VarUpdFunc> vu = new HashSet<>();
				next_symbol(); // skip "then"
				Triplet<String, Boolean, Map<String, String>> tgtExtState = this
						.readTargetExtState(skip);
				String stateLabel = tgtExtState.getValue0();
				logger.debug(stateLabel);
				skip = tgtExtState.getValue1();
				Map<String, String> stateVar = tgtExtState.getValue2();
				logger.debug(stateVar);
				if (stateVar != null) {
					for (String key : stateVar.keySet()) {
						// should be action type name
						vu.add(new VarUpdFunc(actTypeLabel, key, stateVar.get(key)));
					}
					currentGuard.setFuncs(vu);
				}
				String last = pcaBuff.getLast();
				if (!last.equals(stateLabel))
					pcaBuff.addLast(stateLabel);
				trueCondition = false;
				break;

			case EpcaSymbol.ELSE:// ELSE consider Guard
				currentExtAct = previousExtAct;
				actTypeLabel = currentExtAct.getActionType() + currentExtAct.getActionLabel();
				// Guard currentGuard = preGuard;
				Guard elseGuard = new Guard("else");
				currentExtAct.getGuardsList().add(elseGuard);
				next_symbol(); // skip "else"
				tgtExtState = this.readTargetExtState(skip);
				vu = new HashSet<>();
				stateLabel = tgtExtState.getValue0();
				skip = tgtExtState.getValue1();
				stateVar = tgtExtState.getValue2();
				if (stateVar != null) {
					for (String key : stateVar.keySet()) {
						vu.add(new VarUpdFunc(actTypeLabel, key, stateVar.get(key)));
					}
					elseGuard.setFuncs(vu);
				}
				trueCondition = false;
				break;

			default:
				sym = this.current.toString();
				// ---------------------
				if (sym.equals("->")) {
					logger.debug(previousExtAct.getActionType() + previousExtAct.getActionLabel());
					trueCondition = true;
				}
				// ---------------------
				if (this.current.kind == 123 || this.current.kind == 124) {
					if (Character.isUpperCase(sym.charAt(0)) && trueCondition
							&& !this.current.toString().equals("ERROR")) {
						String actTypeStr = previousExtAct.getActionType()
								+ previousExtAct.getActionLabel();
						logger.debug(
								previousExtAct.getActionType() + previousExtAct.getActionLabel());
						trueCondition = false;
						Guard trueGuard = new Guard("true");
						if (previousExtAct.getGuardsList() == null)
							previousExtAct.setGuardsList(new LinkedList<Guard>());
						previousExtAct.getGuardsList().add(trueGuard);
						tgtExtState = this.readTargetExtState(skip);
						pcaBuff.addLast(sym);
						vu = new HashSet<>();
						stateLabel = tgtExtState.getValue0();
						skip = tgtExtState.getValue1();
						stateVar = tgtExtState.getValue2();
						if (stateVar == null)
							break;
						for (String key : stateVar.keySet()) {
							vu.add(new VarUpdFunc(actTypeStr, key, stateVar.get(key)));
						}
						trueGuard.setFuncs(vu);
						trueCondition = false;
						break;
					}
					pcaBuff.addLast(sym);
					break;
				}

				if (sym.equals("[")) {
					while (this.current.kind != EpcaSymbol.RSQUARE)
						next_symbol();
				} else
					pcaBuff.addLast(sym);
				break;
			}// switch the symbol

		} // while not end .
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < pcaBuff.size(); i++)
			sb.append(pcaBuff.get(i));
		logger.debug(sb);
		/**
		 * Alternatively, normalize the pca-fsp to merge same transitions
		 */
		templatePCAMap.put(processID, sb.toString());
		/**
		 * Set up EPCA
		 */
		CompactState templatePca = this.getTemplatePCA(processID, sb);
		
		//set the abstract/template FSP to the vpca
		vpca.setAbstractFSP(sb.toString());
		
		/**
		 * for debugging...
		 * 
		 * if (templatePca.name.contains("FC")) { APITest.printPCA(templatePca); // *
		 * Adding self-composition here to test the difference Vector<CompactState>
		 * machines = new Vector<>(1); machines.add(templatePca); PFSPCompiler comp =
		 * new PFSPCompiler(); CompactState com = comp.compose(templatePca.name,
		 * machines); APITest.printPCA(com); }
		 */
		/**
		 * it needs normalisation by merging same transitions between common states
		 */
		// p.setTemptlatePca(templatePca);
		vpca.setTemptlatePca(new CompactStateRef(templatePca));
		// logger.info(templatePca.convertToGraphviz());
		vpca.setAlphabet(templatePca.getAlphabet());
		vpca.setStates(templatePca.states);
		for (String nsLabel : this.vars.keySet()) {
			String namespace = nsLabel.substring(0, nsLabel.indexOf(":"));
			if (vpca.getProcessName().equals(namespace)
					|| Character.isLowerCase(namespace.charAt(0))) {
				// p.addVariable(this.vars.get(nsLabel));
				this.addVariable(this.vars.get(nsLabel), vpca);
			}
		}
		// logger.debug(this.cachedValue);
		logger.debug(this.varInstances);
		// p.setInstnaceValues(this.converInstanceValue());
		// p.testInstanceValues();
		vpca.setInstnaceValues(this.varInstances);
		// re-initiate the cachedValue
		this.cachedValue = new HashMap<>();
		this.varInstances = new HashMap<>();
		return vpca;
	}

	/**
	 * Convert this.cachedValue into InstanceValue objects
	 * 
	 * @return
	 */
	private Map<String, List<VariableInstance>> converInstanceValue() {
		Map<String, List<VariableInstance>> instnaceValues = new HashMap<>();
		for (String actionKey : this.cachedValue.keySet()) {
			Map<String, Vector<Pair<Object, Double>>> varMap = this.cachedValue.get(actionKey);
			List<VariableInstance> viList = new LinkedList<>();
			for (String varName : varMap.keySet()) {// for each variable
				String namespace = actionKey.substring(actionKey.indexOf(">") + 1);
				VariableType vt = this.vars.get(namespace + "::" + varName);
				VariableInstance vi = new VariableInstance(vt);
				Map<Object, Double> value = new HashMap<>();
				Vector<Pair<Object, Double>> valueProbPairs = varMap.get(varName);
				for (Pair<Object, Double> pair : valueProbPairs) {
					value.put(pair.getValue0(), pair.getValue1());
				}
				vi.setInstValueDist(value);
				viList.add(vi);
			} // for each var
			instnaceValues.put(actionKey, viList);
		}
		return instnaceValues;

	}

	/**
	 * Read the string after "ELSE" and "THEN" e.g. Q[Q::v1+2]...[Q::vn-23]
	 * 
	 * @param skip
	 * @return
	 */
	private Triplet<String, Boolean, Map<String, String>> readTargetExtState(boolean skip) {
		boolean stateFlag = true;
		String stateLabel = "";
		Map<String, String> varExpMap = new TreeMap<>();
		if (stateFlag) {
			stateLabel = this.current.toString();
			stateFlag = false;
		}
		next_symbol();// [
		// if there is no "[.."
		if (!this.current.toString().equals("[")) {
			skip = false;
			return Triplet.with(stateLabel, skip, null);
		}

		String nsVar = "";
		String exp = "";
		while (true) {
			if (this.current.kind == 123 || this.current.kind == 124
					|| this.current.kind == EpcaSymbol.COLON_COLON) {
				nsVar += this.current.toString();
				exp = nsVar;
			}
			next_symbol();
			if (this.current.kind != EpcaSymbol.RSQUARE)
				exp += this.current.toString();
			else if (this.current.kind == EpcaSymbol.RSQUARE) {
				logger.debug(nsVar);
				logger.debug(exp);
				// vu.add(new VarUpdFunc(actTypeLabel, nsVar, exp));
				// currentGuard.setFuncs(vu);
				varExpMap.put(nsVar, exp);

				next_symbol();
				if (this.current.kind != EpcaSymbol.LSQUARE) {
					skip = false;
					break;
				} else { // ]
					nsVar = "";
					exp = nsVar;
					skip = true;
				}
				logger.debug("skip " + skip);
			}
		}
		return Triplet.with(stateLabel, skip, varExpMap);
	}

	/**
	 * this method caters for the new syntax of extended interface actions in forms
	 * of: !<0.6>query[<0.4>query::amount==1, <0.6>query::amount==0][...]...[...].
	 * 
	 * @param actionType
	 * @param skip
	 * @return triplet of <ExtAction, 1.0, Boolean>, also update/insert value
	 *         instnace prob distribution
	 * @throws Exception
	 */
	private Triplet<ExtAction, Double, Boolean> createExtAction(String actionType, boolean skip)
			throws Exception {
		while (this.current.kind != EpcaSymbol.LESS_THAN) {
			this.next_symbol(); // <
		}
		this.next_symbol(); // probability
		Double prob = this.current.doubleValue();
		this.next_symbol(); // >
		this.next_symbol(); // action label
		String actLabel = this.current.toString();
		ExtAction act = new ExtAction(actLabel, actionType, prob);

		/**
		 * check if this action has interVar associated Condition:Variables must have
		 * been parsed.
		 */
		double vProb = 1, varProb = 1;
		boolean isVarAnnotated = false;
		for (String key : this.vars.keySet()) {
			if (key.contains(actLabel)) {
				isVarAnnotated = true;
				break;
			}
		}
		if (isVarAnnotated) {
			// process [<0.2>amount==1,<0.8>amount==0]..[..] ->
			while (this.current.kind != EpcaSymbol.ARROW) { // .ARROW is "->"
				// [<0.2>amount==1,<0.8>amount==0]..[..]
				switch (this.current.kind) {
				case EpcaSymbol.LSQUARE: // [
					/**
					 * one variable instance creation
					 */
					// VariableInstance vi=new VariableInstance();
					this.next_symbol();
					break;

				case EpcaSymbol.LESS_THAN:
					/**
					 * retrieve variable instance object and set probability and value
					 */
					this.next_symbol(); // skip "<"
					if (this.current.kind != EpcaSymbol.PROBABILITY)
						throw new Exception("bad probability");
					double varprob = this.current.doubleValue;
					this.next_symbol();// skip the value
					this.next_symbol(); // skip ">"
					if (this.current.kind != EpcaSymbol.IDENTIFIER)
						throw new Exception("bad variable name");

					this.next_symbol(); // skip the namespace
					if (this.current.kind != EpcaSymbol.COLON_COLON)
						throw new Exception("variable namespace is missing.");
					this.next_symbol(); // skip "::"
					String varname = this.current.toString();
					this.next_symbol();
					if (this.current.kind != EpcaSymbol.EQUALS)
						throw new Exception("expecting == after variable label");

					this.next_symbol(); // skip "=="
					Object value = null;
					if (this.current.kind == EpcaSymbol.INT_VALUE) {
						value = this.current.intValue();
					} else if (this.current.kind == EpcaSymbol.BOOL_VALUE) {
						value = this.current.toString();
					} else if (this.current.kind == EpcaSymbol.STRING_TYPE) {
						value = this.current.toString();
					} else if (this.current.kind == EpcaSymbol.DOUBLE_VALUE) {
						value = this.current.doubleValue;
					}

					String actKey = actionType + "<" + prob + ">" + actLabel;
					List<VariableInstance> viList = this.varInstances.get(actKey);
					if (viList == null || viList.isEmpty())
						viList = new LinkedList<>();
					VariableInstance vi = null;
					for (VariableInstance v : viList) {
						if (v.getVarName().equals(varname)) {
							vi = v;
							break;
						}
					}
					if (vi == null) {
						/**
						 * new vi and put <value, varprob>
						 */
						vi = new VariableInstance(this.vars.get(actLabel + "::" + varname));
						viList.add(vi);
						this.varInstances.put(actKey, viList);
						Map<Object, Double> instDist = new HashMap<>();
						instDist.put(value, varprob);
						vi.setInstValueDist(instDist);
					} else {
						vi.getInstValueDist().put(value, varprob);
					}
					break;
				default:
					// for chars such as ",", "]"
					this.next_symbol();
					break;
				}// switch
				skip = false;
			} // while
		}
		return Triplet.with(act, 1.0, skip);
	}

	/**
	 * extract the template PCA-FSP
	 * 
	 * @param processName
	 * @param pfsp
	 * @return
	 */
	private CompactState getTemplatePCA(String processName, StringBuffer pfsp) {
		SymbolTable.init();
		String pProcess = pfsp.toString();
		PFSPCompiler comp = new PFSPCompiler();
		CompactState pca = comp.compile(processName, pProcess);
		/**
		 * normalisation to merge probability for same transitions
		 */
		// return PCAUtil.normalisePCA(pca);
		return pca;
	}

	/**
	 * Create one variable object
	 * 
	 * @return
	 */
	private VariableType variableDefinition() {
		VariableType<Object> v = new VariableType<>();
		String varName = "";
		String namespace = "";

		next_symbol();
		if (this.current.kind == 124 || this.current.kind == 123)
			namespace = this.current.toString();
		if (Character.isLowerCase(namespace.charAt(0)))
			v.setKind(VariableType.INTERFACE_KIND);
		else
			v.setKind(VariableType.LOCAL_KIND);
		next_symbol(); // skip "::"
		next_symbol();
		varName = this.current.toString();
		v.setNamespace(namespace);
		v.setVarName(varName);
		String sym = this.current.toString();
		Double p = 0.0;
		Object value = null;
		Map<Object, Double> pd = new TreeMap<>();
		// v.setProbDist(pd);

		while (!sym.equals(";")) {
			next_symbol();
			sym = this.current.toString();
			// logger.info(this.current.kind);
			logger.debug(sym);
			if (this.current.kind == EpcaSymbol.INT_VALUE) {
				value = this.current.intValue();
				pd.put(value, null);
				v.type = VariableType.TYPE_INT;
			} else if (this.current.kind == EpcaSymbol.BOOL_VALUE) {
				// boolean value is converted to string type?
				value = this.current.toString();
				pd.put(value, null);
				v.type = VariableType.TYPE_BOOLEAN;
			} else if (this.current.kind == EpcaSymbol.STRING_TYPE) {
				value = this.current.toString();
				pd.put(value, null);
				v.type = VariableType.TYPE_STRING;
			} else if (this.current.kind == EpcaSymbol.IDENTIFIER) {
				value = this.current.toString();
				// pd.put(value, null);
				// v.type = VariableType.TYPE_STRING;
			} else if (this.current.kind == EpcaSymbol.PROBABILITY) {
				p = Double.parseDouble(sym);
				pd.put(value, p);
			}
		} // while not finish

		v.setProbDist(pd);

		push_symbol();
		return v;
	}

	private void current_is(int or, String string) {
	}

	private void push_symbol() {
		this.lex.push_symbol();
	}

	public Hashtable<String, VPCA> getVpcaTable() {
		return vpcaTable;
	}

	public void setVpcaTable(Hashtable<String, VPCA> epcaTable) {
		this.vpcaTable = epcaTable;
	}

	private EpcaSymbol next_symbol() {
		this.current = this.lex.next_symbol();
		return this.current;
	}

	public Map<String, String> getTemplatePCAMap() {
		return templatePCAMap;
	}

	public void setTemplatePCAMap(Map<String, String> templatePCAMap) {
		this.templatePCAMap = templatePCAMap;
	}

	/**
	 * moved from EPCA's method
	 * 
	 * @param var
	 * @param p
	 */
	private void addVariable(VariableType var, VPCA p) {
		if (p.getVariables() == null)
			p.setVariables(new HashSet<>());
		p.getVariables().add(var);
	}

	private void setExtActions(Set<ExtAction> actions, VPCA p) {
		p.setExtActions(actions);
	}

}
