package edu.rmit.casir.util;

import org.apache.log4j.Logger;

public class GeneralUtil {

	private static Logger logger = Logger.getLogger(GeneralUtil.class);

	public static double[] bubbleSort(double[] dou) {
		boolean swap = true;
		int j = 0;
		double tmp = 0.0;
		while (swap) {
			swap = false;
			j++;
			for (int i = 0; i < dou.length - j; i++) {
				if (dou[i] > dou[i + 1]) {
					tmp = dou[i];
					dou[i] = dou[i + 1];
					dou[i + 1] = tmp;
					swap = true;
				}
			}
		}
		return dou;
	}

	public static String getLeftPart(String word, String partition) {
		if (!word.contains(partition))
			return null;
		int index = word.indexOf(partition);
		return word.substring(0, index).trim();
	}

	public static String getRightPart(String word, String partition) {
		if (!word.contains(partition))
			return null;
		int index = word.indexOf(partition);
		return word.substring(index + partition.length()).trim();
	}

	public static String getTimeRandomSeqId() {
		Long time = System.currentTimeMillis();
		String t = time + "";
		String id = t.substring(8);
		// logger.debug(time);
		return id;
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public static boolean isBoolean(String str){
		if(str.equalsIgnoreCase("true")||str.equalsIgnoreCase("false"))
			return true;
		return false;
	}
	
	public static double round(double value, int places){
		/**
		 * set place in the code centrally
		 */
//		places=6;
		if (places < 0) throw new IllegalArgumentException();
	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
}
