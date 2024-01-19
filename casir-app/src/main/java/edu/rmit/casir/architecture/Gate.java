package edu.rmit.casir.architecture;

import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;


/**
 * It is simplified that Gate does not have a vPCA
 * 
 * @author terryzhou
 *
 */
public abstract class Gate {

	Logger logger = Logger.getLogger(edu.rmit.casir.architecture.Gate.class);

	String name;

	// the ken where Gate is located
	Ken ken;

	// The symbols are the transitions (possibly with variables) associated with the
	// gate, these symbols are members of the ken.
	// initially defined as String type, later can be changed to Symbol type
	Set<String> symbols = new HashSet<String>();

	// Gate's behaviour model represented by LPCA, the model is part of
	// the ken's PCA model; Not sure if a Ken should have a LabeledPCA,
	// LabeledPCA model;

	Integer kind;

	public static final Integer kind_provided = 1;
	public static final Integer kind_required = 0;

	// ***********************************************************************************

	public Gate(String name) {
		this.setName(name);
	}

	// public Gate(String name, LabeledPCA lpca) {
	// this.setName(name);
	// this.model = lpca;
	// }

	public Set<String> getSymbols() {
		return this.symbols;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public Ken getKen() {
		return this.ken;

	}

	public void setKen(Ken k) {
		this.ken = k;
	}

	public void setSymbols(Set<String> symbols) {
		this.symbols = symbols;
	}

	// public LabeledPCA getModel() {
	// return model;
	// }
	//
	// public void setModel(LabeledPCA model) {
	// this.model = model;
	// }

	public abstract Integer getKind();

	public void print() {
		logger.info("PRINTING GATE");
		logger.info(this.getName());
		logger.info(this.getKind());
		logger.info(this.getKen().getName());
		// logger.info(this.getSymbols());

	}

}
