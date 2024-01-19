package edu.rmit.casir.epca.parser;

import java.util.Hashtable;

public class EpcaSymbolTable {

	private static Hashtable keyword;

	public static void init() {
		keyword = new Hashtable();
		keyword.put("const", new Integer(1));
		keyword.put("property", new Integer(2));
		keyword.put("range", new Integer(3));
		keyword.put("if", new Integer(4));
		keyword.put("then", new Integer(5));
		keyword.put("else", new Integer(6));
		keyword.put("forall", new Integer(7));
		keyword.put("when", new Integer(8));
		keyword.put("set", new Integer(9));
		keyword.put("progress", new Integer(10));
		keyword.put("menu", new Integer(11));
		keyword.put("animation", new Integer(12));
		keyword.put("actions", new Integer(13));
		keyword.put("controls", new Integer(14));
		keyword.put("deterministic", new Integer(15));
		keyword.put("minimal", new Integer(16));
		keyword.put("compose", new Integer(17));
		keyword.put("target", new Integer(18));
		keyword.put("import", new Integer(19));
		keyword.put("assert", new Integer(21));
		keyword.put("fluent", new Integer(22));
		keyword.put("exists", new Integer(24));
		keyword.put("rigid", new Integer(25));
		keyword.put("fluent", new Integer(22));
		keyword.put("constraint", new Integer(26));
		keyword.put("ltl_property", new Integer(27));
		keyword.put("safe", new Integer(28));
		keyword.put("initially", new Integer(29));

		keyword.put("pca", new Integer(201));
		keyword.put("IO", new Integer(200));

		// -----new added kind for EPCA, starting from 3xx------------------
		keyword.put("EPCA", new Integer(301));
		keyword.put("VAR", new Integer(302));
		keyword.put("elseif", new Integer(306));
		
		// ------complete the EPCA kind extension-----------------------

	}

	public static Object get(String s) {
		return keyword.get(s);
	}

}
