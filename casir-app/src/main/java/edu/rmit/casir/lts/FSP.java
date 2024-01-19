package edu.rmit.casir.lts;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jpowergraph.PIPENode;
import net.sourceforge.jpowergraph.Edge;
import net.sourceforge.jpowergraph.Node;
import net.sourceforge.jpowergraph.defaults.DefaultGraph;
import net.sourceforge.jpowergraph.defaults.TextEdge;

import org.apache.log4j.Logger;

import pipe.dataLayer.Transition;

/**
 * Generate FSP from automata/lts/state transition systems 
 * for LTSA verification 
 * @author terryzhou created in Mar.2011
 */
public class FSP {
	
	private static Logger logger=Logger.getLogger(FSP.class);

	Set<String>						observableActions;
	Map								fluents, asserts;
	static int						index	= 0;

	StringBuffer					pathFileBuffer;
	StringBuffer					fspFileBuffer;
	static Node						initial;
	static List<Node>				ats;

	public HashMap<String, Vector>	paths	= new HashMap<String, Vector>();

	ArrayList<Map>					allParallelProcesses;

	public FSP() {
		super();
		this.observableActions = new HashSet<String>();
		this.allParallelProcesses = new ArrayList();
		this.pathFileBuffer = new StringBuffer();
		this.fspFileBuffer = new StringBuffer();
	}
	
	
	

	/**
	 * add a single process to the process list
	 * 
	 * @param p
	 */
	public void addOneParallelProcess(Map p) {
		this.allParallelProcesses.add(p);

	}

	/**
	 * add the total action set (interfaces) for the lts file
	 */
	public void addTotalObservableActionSet(Set<Transition> obsTransitions) {
		for (Transition t : obsTransitions) {
			addObservableAction(t.getName());
		}

	}

	public void addObservableAction(String actionName) {
		this.observableActions.add(actionName);
	}

	/**
	 * get all actions from the known process
	 * 
	 * @param processName
	 * @return
	 */
	public Set<String> getActionsFromProcess(String processName) {
		Set<String> actionResult = new HashSet();
		for (Map p : this.allParallelProcesses) {
			if (p.containsKey(processName)) {
				LinkedList<Action> actions = (LinkedList) p.get(processName);
				for (Action a : actions) {
					actionResult.add(a.getName());
				}
			}
		}
		return actionResult;
	}

	/**
	 * initialize the observable actions for the fsp file
	 * 
	 * @param dg
	 */
	public void setObservableActions(DefaultGraph dg) {
		List<Edge> edges = dg.getAllEdges();
		for (Edge e : edges) {
			String label = ((TextEdge) e).getText().toLowerCase();
			if (label.contains("in") || label.contains("out"))
				addObservableAction(label);
		}
	}

	public Set<String> getObservableActions() {
		return observableActions;
	}

	public void setObservableActions(Set<String> observableActions) {
		this.observableActions = observableActions;
	}

	/**
	 * set observable action set from PIPE model
	 * 
	 * @param observableTransitions
	 */
	public void setObservaleActions(Set<Transition> observableTransitions) {
		Iterator it = observableTransitions.iterator();
		while (it.hasNext()) {
			Transition t = (Transition) it.next();
			this.addObservableAction(t.getName().toLowerCase());
		}
	}

	public ArrayList<Map> getAllParallelProcesses() {
		return allParallelProcesses;
	}

	public void setAllParallelProcesses(ArrayList<Map> allParallelProcesses) {
		this.allParallelProcesses = allParallelProcesses;
	}

	/**
	 * get all unspecified but unintended (observable) actions for a process by using all observable
	 * set minus the actions included in the processName
	 * 
	 * @param processName
	 * @return
	 */
	private Set<String> getUnintendedAlphabets(String processName) {
		Set<String> actionexplicited = this.getActionsFromProcess(processName);
		Set<String> total = new HashSet<String>();
		for (String s : this.getObservableActions()) {
			total.add(s);
		}

		total.removeAll(actionexplicited);
		// this.removeList(total, actionexplicited);

		return total;
	}

	private void removeList(Set total, Set remove) {
		Iterator ir = remove.iterator();
		while (ir.hasNext()) {
			Object o = ir.next();
			if (total.contains(o))
				remove.remove(o);
		}

	}

	/**
	 * find all paths in DefaultGraph from the initial point S0, to any states in ATS record the
	 * paths in terms of processes FSP expression, and stored in ltsfile
	 * 
	 * @param dg
	 * @param ltsfile
	 * @throws FileNotFoundException
	 */
	public void outputAllPathsToFSP(DefaultGraph dg) {
		List<Node> allNodes = dg.getAllNodes();
		LinkedList<Node> visited = new LinkedList<Node>();
		initial = null;
		Node end = null;
		for (Node n : allNodes) {
			if (n.getLabel().equals("S0"))
				initial = n;
		}
		visited.add(initial);
		ats = getATS(dg);
		breadthFirst(dg, visited);
	}

	private void breadthFirst(DefaultGraph graph, LinkedList<Node> visited) {
		LinkedList<Node> nodes = getSuccssors(visited.getLast());
		for (Node node : nodes) {
			if (visited.contains(node))
				continue;
			if (ats.contains(node)) {
				visited.add(node);
				index++;
				printPath(visited);
				visited.removeLast();
				break;
			}
		}
		for (Node node : nodes) {
			if (visited.contains(node) || ats.contains(node)) {
				continue;
			}
			visited.addLast(node);
			breadthFirst(graph, visited);
			visited.removeLast();
		}
	}

	private List<Node> getATS(DefaultGraph dg) {
		List<Node> allnodes = dg.getAllNodes();
		ats = new ArrayList();
		for (Node n : allnodes) {
			PIPENode pipenode = (PIPENode) n;
			String marking = pipenode.getMarking();
			/**
			 * if the marking contains a token at the acceptable terminal place, then put this
			 * marking int he ATS Currently for testing, we assume that: the node without output
			 * edge is ats the node point back to the initial node is ats
			 */
			List<Edge> edges = n.getEdgesFrom();
			if (edges.isEmpty()) {
				ats.add(n);
			} else {
				for (Edge e : edges) {
					Node next = e.getTo();
					if (next.getLabel().equals("S0")) {
						ats.add(n);
					}
				}
			}
		}
		return ats;
	}

	/**
	 * get all successors for the node passed in as parameter
	 * 
	 * @param node
	 * @return
	 */
	private LinkedList<Node> getSuccssors(Node node) {
		LinkedList<Node> succs = new LinkedList<Node>();
		List<Edge> edges = node.getEdgesFrom();
		for (Edge e : edges) {
			Node next = e.getTo();
			succs.add(next);
		}
		return succs;
	}

	/**
	 * generate lts fsp file for ltsa
	 * 
	 * @param visited
	 */
	private void printPath(LinkedList<Node> visited) {
		String processName = "P" + index;
		LinkedList<Action> actionList = new LinkedList<Action>();
		Map<String, LinkedList> process = new HashMap();
		String processStr = processName + "=(";
		fspFileBuffer.append(processStr);
		pathFileBuffer.append("P" + processName + "\t");
		Vector<String> actions = new Vector<String>();
		int length = visited.size();
		for (int i = 0; i < length; i++) {
			Node node = visited.get(i);
			if (i >= length - 1)
				break;
			List<Edge> outEdges = node.getEdgesFrom();
			for (Edge e : outEdges) {
				String label = ((TextEdge) e).getText().toLowerCase();
				Node nextNode = e.getTo();
				Node iPlus = visited.get(i + 1);
				if (nextNode.getLabel().equals(iPlus.getLabel())) {
					Action a = new Action();
					a.setName(label);
					actionList.add(a);
					fspFileBuffer.append(label + " -> ");
					pathFileBuffer.append(label + " -> ");
					actions.add(label);
				}
			}
		}
		process.put(processName, actionList);
		addOneParallelProcess(process);
		fspFileBuffer.append(processName + ")");
		pathFileBuffer.append("END" + "\n");
		paths.put("P" + processName, actions);
		Set<String> unintendedActions = getUnintendedAlphabets(processName);
		if (unintendedActions != null && unintendedActions.size() != 0) {
			fspFileBuffer.append("+{");
			Iterator it = unintendedActions.iterator();
			while (it.hasNext()) {
				String a = (String) it.next();
				fspFileBuffer.append(a);
				if (it.hasNext()) {
					fspFileBuffer.append(",");
				}
			}
			fspFileBuffer.append("}");
		}
		fspFileBuffer.append(".\n");
		fspFileBuffer.append("||PP" + index + "=P" + index + "." + "\n");
	}

	/**
	 * Append the asserts to the existing FSP file for model checking,
	 * without changing any fsp processes.
	 * 
	 * @param assertFile
	 * @return
	 */
	public boolean attachAsserts(String assertFile) {
		logger.debug("ATTACH SPECIFICATION");
		BufferedReader assInput = null;
		try {
			assInput = new BufferedReader(new FileReader(assertFile));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return false;
		}
		String line;
		try {
			line = assInput.readLine();
			this.fspFileBuffer.append("\n");
			this.fspFileBuffer.append("//-----------------CONSISTENCY SPECIFICATION---------------------\n");
			while (line != null) {
				this.fspFileBuffer.append(line);
				this.fspFileBuffer.append("\n");
				line = assInput.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;

		}
		return true;
	}
	
	
	/**
	 * Import the asserts to the existing fsp file by addinging 
	 * the extension actions to the corresponding processes according
	 * to the assert names and the process names.
	 * @param assertFileFullPath
	 */
//	to be completed
	public void importAsserts(String assertFileFullPath){
		this.attachAsserts(assertFileFullPath);
		// read assert file into a hashmap
		// process the hashmap to extract the actions for each assert
		// go thru each fsp process and compaire the actions by adding 
		// new actions in assert to the extension part.
		// output the revised fsp 
	}

	
	/**
	 * add the fluents and asserts into fsp lts expressions
	 * 
	 */
	public void attachAsserts() {
		Iterator it = this.getFluents().keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String body = (String) this.getFluents().get(key);
			this.fspFileBuffer.append("fluent " + key + " = " + body + "\n");
		}
		it = this.getAsserts().keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String body = (String) this.getAsserts().get(key);
			this.fspFileBuffer.append("assert " + key + " = " + body + "\n");
		}
	}

	
	public StringBuffer getPathFileBuffer() {
		return pathFileBuffer;
	}

	
	public void setPathFileBuffer(StringBuffer pathFileBuffer) {
		this.pathFileBuffer = pathFileBuffer;
	}

	public Map getFluents() {
		return fluents;
	}

	public void setFluents(Map fluents) {
		this.fluents = fluents;
	}

	public Map getAsserts() {
		return asserts;
	}

	public void setAsserts(Map asserts) {
		this.asserts = asserts;
	}

	public StringBuffer getFspFileBuffer() {
		return fspFileBuffer;
	}

	public void setFspFileBuffer(StringBuffer fspFileBuffer) {
		this.fspFileBuffer = fspFileBuffer;
	}

}
