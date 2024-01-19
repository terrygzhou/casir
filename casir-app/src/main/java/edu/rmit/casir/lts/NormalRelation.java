package edu.rmit.casir.lts;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import lts.Relation;

public class NormalRelation extends Relation implements Cloneable {

	public static Relation normalisation(Relation ra) {
		Relation r = new Relation();
		Set<Object> keys = ra.keySet();
		for (Object key : keys) {
			Set<String> related = new HashSet<String>();
			Object v = ra.get(key);

			if (v instanceof Vector) {
				Vector vprime = (Vector) v;
				Object[] velem = vprime.toArray();
				for (int i = 0; i < velem.length; i++) {
					if (velem[i] instanceof String) {
						String strElem = (String) velem[i];
						related.add(strElem);
					}
					if (velem[i] instanceof Set) {
						Set setElem = (Set) velem[i];
						related.addAll(setElem);
					}
				}
			} // if vector
			if (v instanceof String) {
				String vprime = (String) v;
				related.add(vprime);
			}// if String

			Vector<String> normV = new Vector<String>();
			for (String s : related) {
				normV.add(s);
			}

			r.put(key, normV);

		}
		return r;
	}

	@Override
	public synchronized String toString() {
		StringBuffer sb = new StringBuffer();
		for (Object key : this.keySet()) {
			sb.append(key.toString());
			sb.append(this.get(key).toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public synchronized Object clone() {
		// TODO Auto-generated method stub
		return super.clone();
	}

}
