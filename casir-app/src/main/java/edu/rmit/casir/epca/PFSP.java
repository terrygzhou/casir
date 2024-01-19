package edu.rmit.casir.epca;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import lts.SymbolTable;
import api.LTSA_PCA_Tool;

import edu.rmit.casir.util.GeneralUtil;

public class PFSP {
	String pfspFilePath;
	Logger logger = Logger.getLogger(PFSP.class);

	private HashMap<String, String> pFSPHash;
	private HashMap<String, HashSet> compositeHash;
	private LTSA_PCA_Tool ltsaPca;

	
//**********************************************************************************
	
	
	
	/**
	 * constructing from given pfsp file
	 * @param file
	 */
	public PFSP(String file) {
		this.pfspFilePath = file;
		this.loadPcaFsp(file);
	}
	
	/**
	 * constructing from pfsp stringbuffer
	 * @param sb
	 */
	public PFSP(StringBuffer sb){
		InputStream is=new ByteArrayInputStream(sb.toString().getBytes());
		BufferedReader br= new BufferedReader(new InputStreamReader(is));
		this.loadPcaFmStringBuffer(br);
	}

	/**
	 * Read pca from a pFSP file
	 * @param filePath
	 */
	public void loadPcaFsp(String filePath) {
		this.init();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		this.loadPcaFmStringBuffer(br);
	}

	/**
	 * Read pca from a bufferedReader obj
	 * @param br
	 */
	public void loadPcaFmStringBuffer(BufferedReader br) {
		String line;
		try {
			line = br.readLine();
			while (line != null) {
				if (line.startsWith("//")) {
					line = br.readLine();
					continue;
				}
				if (line.contains("=")) {
					String leftString = GeneralUtil.getLeftPart(line, "=");
					// for non composite processes
					if (!leftString.contains(" ") && !leftString.contains("||")) {
						String key = leftString;
						String process = "";
						if (line.endsWith(".")) {
							process = GeneralUtil.getRightPart(line, "=");
							this.pFSPHash.put(key, process);
							line = br.readLine();
							continue;
						}
						while (!line.endsWith(".")) {
							leftString = GeneralUtil.getLeftPart(line, "=");
							if (leftString != null && leftString.trim().equals(key))
								line = line.substring(line.indexOf("=") + 1);
							process = process + line;//
							try {
								line = br.readLine();// .trim();
								if (line != null)
									line.trim();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						process = process + line.trim();
						this.pFSPHash.put(key, process);
						line = br.readLine();
					}
					// if a parallel composite
					if (line.startsWith("||")) {
						String compKey = GeneralUtil.getRightPart(leftString, "||");
						String rightString = GeneralUtil.getRightPart(line, "=");

						String allCompProcess = GeneralUtil.getRightPart(rightString, "(");
						allCompProcess = GeneralUtil.getLeftPart(allCompProcess, ")");
						HashSet<String> ps = new HashSet<String>();
						if (!allCompProcess.contains("||")) {
							ps.add(allCompProcess);
						} else {
							String[] comps = allCompProcess.split("\\|\\|");
							for (String s : comps) {
								ps.add(s);
							}
						}
						this.compositeHash.put(compKey, ps);
					}
				}
				line = br.readLine();
			} // while line != null;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void init() {
		this.pFSPHash = new HashMap<String, String>();
		this.compositeHash = new HashMap<String, HashSet>();
		this.ltsaPca = new LTSA_PCA_Tool();
		SymbolTable.init();
	}

	public HashMap<String, String> getpFSPHash() {
		return pFSPHash;
	}

	public void setpFSPHash(HashMap<String, String> pFSPHash) {
		this.pFSPHash = pFSPHash;
	}

	public HashMap<String, HashSet> getCompositeHash() {
		return compositeHash;
	}

	public void setCompositeHash(HashMap<String, HashSet> compositeHash) {
		this.compositeHash = compositeHash;
	}

}
