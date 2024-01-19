package edu.rmit.casir.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class DotUtil {

	/**
	 * convert a LTSA transition txt file to a dot file in the same directory of txt file
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public static boolean convertFSADot(String tranFile, String dotfile) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(dotfile);
		BufferedReader tranInput = null;
		try {
			tranInput = new BufferedReader(new FileReader(tranFile));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return false;
		}
		String line;
		try {
			line = tranInput.readLine();
			String currentState = null;
			String label = null;
			String targetState = null;
			if (line.equalsIgnoreCase("Process:")) {
				line = tranInput.readLine();
				
				out.println("digraph " + line + "{");
				out.println("node [shape = circle];");
				line = tranInput.readLine();
			}
			while (line != null) {
				if (line.contains("=") && line.contains("->")) {
					currentState = line.substring(0, line.indexOf("=")).trim();
					label = line.substring(line.indexOf("(")+1, line.indexOf("->")).trim().replace(".", "_");
					if(!line.contains(","))
					targetState = line.substring(line.indexOf(">")+1).trim();
					else 
					targetState = line.substring(line.indexOf(">")+1,line.indexOf(")")).trim();
					out.println(currentState + "->" + targetState + " [ label=" + label + "];");
				} else if (line.contains("|")) {
					label = line.substring(line.indexOf("|")+1, line.indexOf("->")).trim().replace(".", "_");
					if(line.endsWith(",") || line.endsWith(".")){
						targetState = line.substring(line.indexOf(">")+1,line.indexOf(")")).trim();
					}else 
						targetState = line.substring(line.indexOf(">")+1).trim();
					out.println(currentState + "->" + targetState + " [ label=" + label + "];");
				}
				line = tranInput.readLine();
			} // while
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		out.println("}");
		out.flush();
		out.close();
		return true;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public static void main(String[] args) throws Exception {
		String tranFile = "/Users/terryzhou/phd/projects/eclipse/workplace/ConsistencyChecking/SEQ_TRAN.txt";
		String dotfile = "/Users/terryzhou/phd/projects/eclipse/workplace/ConsistencyChecking/SEQ_TRAN.dot";
		DotUtil.convertFSADot(tranFile, dotfile);
	}

}
