package edu.rmit.casir.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import net.sourceforge.jpowergraph.Edge;
import net.sourceforge.jpowergraph.Node;
import net.sourceforge.jpowergraph.defaults.DefaultGraph;
import net.sourceforge.jpowergraph.defaults.TextEdge;

import org.apache.log4j.Logger;

import pipe.dataLayer.Arc;
import pipe.dataLayer.DataLayer;
import pipe.dataLayer.InhibitorArc;
import pipe.dataLayer.Place;
import pipe.dataLayer.PlaceTransitionObject;
import pipe.dataLayer.Transition;
import edu.rmit.casir.lts.FSP;
//import pipe.modules.reachability.ReachGraphArc;
//import rmit.debug.Debug;

public class GraphUtil {

	static int index = 0;
	static FSP fsp = new FSP();// null;
	private static Logger logger=Logger.getLogger(GraphUtil.class);


	public static void display(DefaultGraph dg) {
		List<Node> nodes = dg.getAllNodes();
		List<Edge> edges = dg.getAllEdges();

		for (Edge e : edges) {
			Node from = e.getFrom();
			Node to = e.getTo();
			TextEdge te = (TextEdge) e;
			logger.debug("Node " + from.getNodeType() + "\t" + from.getLabel());
			logger.debug("Edge " + te.getText());
			logger.debug("Node " + to.getNodeType() + "\t" + to.getLabel());
		}
	}

	
	public static void outputDotGraph(DefaultGraph dg, String dotfile) throws IOException{
		StringBuffer sb=generateDot(dg);
		outputStringBuffer(sb,dotfile);
	}
	
	
	private static StringBuffer generateDot(DefaultGraph dg) {
		StringBuffer dotBuffer=new StringBuffer();
		List<Node> nodes = dg.getAllNodes();
		List<Edge> edges = dg.getAllEdges();
		dotBuffer.append("digraph reachability_graph {\nrankdir=TB;\nnode[shape=circle];\n");
		for (Edge e : edges) {
			Node from = e.getFrom();
			Node to = e.getTo();
			String label = ((TextEdge) e).getText();
			logger.debug("label "+label);
			dotBuffer.append(from.getLabel() + "->" + to.getLabel() + " [label=" + label + "]\n");
		}
		dotBuffer.append("}");
		return dotBuffer;
	}

	
	private static HashMap findFirstInconsistentStates(HashMap<String, ArrayList> allResults)
			throws FileNotFoundException {
		HashMap inconsistentStateHash = new HashMap();
		if (allResults == null || allResults.size() == 0) return null;
		for (Entry oneProcessResult : allResults.entrySet()) {
			logger.debug("process name " + oneProcessResult.getKey());
			HashMap<String, List> singleProcessResult = (HashMap) oneProcessResult.getValue();
			for (Entry<String, List> e : singleProcessResult.entrySet()) {
				logger.debug("Process Name " + e.getKey());
				List<HashMap> assertErrors = e.getValue();
				// logger.debug("value " + assertErrors.toString());
				for (HashMap<String, Vector> trace : assertErrors) {
					for (Entry<String, Vector> errorTrace : trace.entrySet()) {
						logger.debug("Assert " + errorTrace.getKey());
						Vector v = errorTrace.getValue();
						if (v.size() > 0) {
							// out.print(oneProcessResult.getKey() + ":\t");
							String lastState = getLastElemInTrace(v);
							// out.println(lastState);
							inconsistentStateHash.put(oneProcessResult.getKey().toString(), lastState);
							logger.debug("Error trace " + errorTrace.getValue().toString());
						}
					}
				}
			}
		}
		return inconsistentStateHash;

	}

	private static String getLastElemInTrace(Vector trace) {
		if (trace == null || trace.size() == 0) return null;
		return (String) trace.get(trace.size() - 1);
	}

	private static StringBuffer migrateTrace(HashMap<String, Vector> paths,
			HashMap<String, String> inconsistentStateHash) throws Exception {
		StringBuffer sb = new StringBuffer();
		if (inconsistentStateHash==null || inconsistentStateHash.size() == 0) return sb;
		int maxCost=0;
		int minCost=99999;
		int remedyNum=0;
		for (Entry<String, Vector> process : paths.entrySet()) { // for each path
			String p1 = process.getKey();
			Vector<String> actions = process.getValue();
			boolean processMatched = false;
			for (Entry<String, String> error : inconsistentStateHash.entrySet()) { // for each error
																					// cases
				String p2 = error.getKey();
				String firstErrorState = error.getValue();
				boolean actionMatched = false;
				if (p1.equals(p2)) {
					remedyNum++;
					processMatched = true;
					sb.append(p1 + " = ");
					for (int i = 0; i < actions.size(); i++) {
						int steps = 0;
						int cost=0;
						if (actions.get(i).equalsIgnoreCase(firstErrorState)) {
							steps = actions.size() - i - 1;
							
							for (int j = i; j < actions.size(); j++) {
								sb.append("(" + actions.get(j) + ") -> ");
								cost=cost+actions.get(j).length();
							}
							if(cost>maxCost) maxCost=cost;
							if(cost<minCost) minCost=cost;
							sb.append("END" + " (cost taken = " + cost + ")"+"\n\n");
							actionMatched = true;
							break;
						}
						sb.append(actions.get(i) + " -> ");
					} // for actions
					if (!actionMatched) sb.append("END"+"\n\n");
				} // if process matched
			} // for error trace

			// output the whole process without changing
			if (!processMatched) {
				sb.append(p1 + " = ");
				for (String a : actions) {
					sb.append(a + " -> ");
				}
				sb.append("END \n");
			}
			
		} // for normal process
		sb.append("remedy paths number "+remedyNum+"\n");
		double rate=100*remedyNum/paths.size();
		sb.append("the probability of inconsistency is "+rate+"%\n");
		sb.append("max cost "+maxCost+"\n min cost "+minCost+"\n");
	
		return sb;
	}

	private static void outputStringBuffer(StringBuffer sb, String filename) throws IOException {
		PrintWriter out = new PrintWriter(filename);
		if (sb != null && sb.length() > 0) {
			out.print(sb.toString());
			out.flush();
			out.close();
		}
	}

	/**
	 * printing the input petrin net to a dot file which name and path are passed
	 * as the parameter dotFilePath.
	 * @param pn
	 * @param dotFilePath
	 */
	public static void convertPNToDot(DataLayer pn, String dotFilePath){
//		String filePath=path+"/"+filename;
		StringBuffer dotString=new StringBuffer("digraph "+pn.pnmlName+" {\n");
		Place[] places=pn.getPlaces();
		Transition[] transitions=pn.getTransitions();
		Arc[] arcs=pn.getArcs();
		InhibitorArc[] inArcs=pn.getInhibitors();
		int[] m0 = pn.getInitialMarkingVector();
		for(int i=0;i<places.length;i++){
			Place p=places[i];
//		for(Place p:places){
//			dotString.append(p.getId()+" [shape=circle,"+"label="+p.getName()+"];\n");
			dotString.append(p.getId()+" [shape=circle,"+"label="+i+"];\n");

		}
		for(Transition t:transitions){
			dotString.append(t.getId()+" [shape=box,"+"label="+t.getName()+"];\n");
		}
		for(Arc a:arcs){
			PlaceTransitionObject ptoSource=a.getSource();
			PlaceTransitionObject ptoTarget=a.getTarget();
			String source=ptoSource.getId();
			String target=ptoTarget.getId();
			if(ptoSource instanceof Transition)  
				source=ptoSource.getId(); 
			if(ptoTarget instanceof Transition)
				target=ptoTarget.getId();
			dotString.append(source+" -> "+target+"\n");
		}
		dotString.append("}");
		try {
			outputStringBuffer(dotString,dotFilePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public static void processResults(HashMap paths, HashMap results, String reportfile) throws Exception {
		HashMap firstErrorStates=findFirstInconsistentStates(results);
		StringBuffer sb=GraphUtil.migrateTrace(paths,firstErrorStates);
		outputStringBuffer(sb, reportfile);
	}

}
