package edu.rmit.casir.verification;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

public class CounterexampleComicsImpl implements Counterexample {

	/**
	 * ************************* Attributes **************************
	 */
	static Logger logger = Logger.getLogger(CounterexampleComicsImpl.class);
	// Strongest Evidence of the counterexample of PCTL model checking
	private Vector<Integer> sePath;

	// the counterexample file consisting of a number of paths
	private String counterexampleFile;

	
	
	
	/**
	 * * ************************* Methods **************************
	 * 
	 */

	/**
	 * @throws Exception
	 * @throws IOException
	 */

	public CounterexampleComicsImpl(String ceFilePath, String stateMapFile) {
		this.counterexampleFile = ceFilePath;
		BufferedReader br = null;
		Vector<Integer> seStateVec = new Vector<>();
		String line = null;
		try {
			br = new BufferedReader(new FileReader(ceFilePath));
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// while (line != null) {
		String pathStr = line.substring(0, line.indexOf("("));
		String[] stateArr = pathStr.split("->");
		for (int i = 0; i < stateArr.length; i++) {
			seStateVec.add(i, Integer.parseInt(stateArr[i].trim()));
		}
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<Integer, Integer> stateMap = new HashMap<>();

		try {
			br = new BufferedReader(new FileReader(stateMapFile));
			line = br.readLine();
			line = br.readLine(); // skip the first line
			while (line != null) {
				int colon_index = line.indexOf(":");
				String stateStr = line.substring(0, colon_index);
				int unfoldedState = Integer.parseInt(stateStr.trim());
				String seStateStr = line.substring(colon_index + 2, line.indexOf(","));
				int seState = Integer.parseInt(seStateStr);
				stateMap.put(unfoldedState, seState);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.debug(stateMap);
		Vector<Integer> sePath = new Vector<>();
		seStateVec.forEach(unfoldedState -> {
			if (unfoldedState < stateMap.size())
				sePath.add(stateMap.get(unfoldedState));
		});
		this.setSePath(sePath);
	}

	/**
	 * ************************* Getter and Setter **************************
	 */

	public Vector<Integer> getSePath() {
		return sePath;
	}

	public void setSePath(Vector<Integer> sePath) {
		this.sePath = sePath;
	}

}
