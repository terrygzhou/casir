package edu.rmit.casir.concurrency.trace;

import java.util.Set;

import lts.Relation;

/**
 * 
 * @author terryzhou
 * 
 */
public class Alphabet {

	Set<Symbol>	sigma;
	

	Set<Relation>	dependenceRelation;

	Set<Relation>	independenceRelation;

	public Alphabet(Set<Symbol> letters, Set<Relation> dependence) {
		this.sigma = letters;
		this.dependenceRelation = dependence;
	}
	


	public void addSymbol(Symbol s) {
		this.sigma.add(s);
	}

	public Set<Symbol> getSigma() {
		return sigma;
	}

	public void setSigma(Set<Symbol> sigma) {
		this.sigma = sigma;
	}

	public Set<Relation> getDependenceRelation() {
		return dependenceRelation;
	}

	public void setDependenceRelation(Set<Relation> dependenceRelation) {
		this.dependenceRelation = dependenceRelation;
	}

	public Set<Relation> getIndependenceRelation() {
		return independenceRelation;
	}

	public void setIndependenceRelation(Set<Relation> independenceRelation) {
		this.independenceRelation = independenceRelation;
	}

}
