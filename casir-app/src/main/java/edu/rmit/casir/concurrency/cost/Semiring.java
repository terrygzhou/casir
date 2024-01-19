package edu.rmit.casir.concurrency.cost;

import java.util.Set;

import lts.Relation;


public interface Semiring {

	Set<Object>	weights			= null;
	Relation	oplus			= null;
	Relation	otimes			= null;

	Object		oplusIdentity	= null;
	Object		otimesIdentity	= null;

}
