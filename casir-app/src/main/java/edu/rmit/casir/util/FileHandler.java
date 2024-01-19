package edu.rmit.casir.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import org.apache.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * The Class FileHandler.
 *
 * @author terryzhou Created on 10 Jan.2013
 */
public class FileHandler {
	
	/** The logger. */
	static Logger logger = Logger.getLogger(FileHandler.class);

	/**
	 * Gets the dir from full path.
	 *
	 * @param fullPath the full path
	 * @return the dir from full path
	 */
	public static String getDirFromFullPath(String fullPath) {
		logger.debug("getDirFromFullPath's full path is " + fullPath);
		if (!FileHandler.isFilePathValid(fullPath))
			return null;
		// dirStr includes "/" in the end
		String dirStr = fullPath.substring(0, fullPath.lastIndexOf("/") + 1);
		return dirStr;
	}

	/**
	 * Gets the file no ext from full path.
	 *
	 * @param fullPath the full path
	 * @return the file no ext from full path
	 */
	public static String getFileNoExtFromFullPath(String fullPath) {
		if (!FileHandler.isFilePathValid(fullPath))
			return null;
		int lastSlash = fullPath.lastIndexOf("/");
		int lastDot = fullPath.lastIndexOf(".");
		String fnwithoutExt = fullPath.substring(lastSlash + 1, lastDot);
		return fnwithoutExt;
	}

	/**
	 * Gets the filename with ext from full path.
	 *
	 * @param fullPath the full path
	 * @return the filename with ext from full path
	 */
	public static String getFilenameWithExtFromFullPath(String fullPath) {
		if (!FileHandler.isFilePathValid(fullPath))
			return null;
		int lastSlash = fullPath.lastIndexOf("/");
		return fullPath.substring(lastSlash + 1, fullPath.length());
	}

	/**
	 * Checks if is file path valid.
	 *
	 * @param fullPath the full path
	 * @return true, if is file path valid
	 */
	private static boolean isFilePathValid(String fullPath) {
		if (!fullPath.contains("."))
			return false;
		if (!fullPath.contains("/"))
			return false;
		return true;
	}

	
	/**
	 * Output path file.
	 *
	 * @param pathfile the pathfile
	 * @param pathFileBuffer the path file buffer
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void outputPathFile(String pathfile, StringBuffer pathFileBuffer)
			throws IOException {
		// Debug.devlog(this.pathFileBuffer.toString());

		// PrintWriter out = new PrintWriter(pathfile);
		PrintWriter out = new PrintWriter(new FileOutputStream(pathfile, false));
		out.print(pathFileBuffer);
		out.flush();
		out.close();
	}
	
	public static void delFile(String pathfile){
		File file=new File(pathfile);
		if(file.delete())
			logger.debug("file "+pathfile+" is deleted!");
		else 
			logger.debug("deletion if failed.");
	}
	
	
	
	/**
	 * append the stringbuffer object to a new/existing file
	 * @param pathfile
	 * @param pathFileBuffer
	 * @throws IOException
	 */
	public static void appendPathFile(String pathfile, StringBuffer pathFileBuffer)
			throws IOException {
		// Debug.devlog(this.pathFileBuffer.toString());

		// PrintWriter out = new PrintWriter(pathfile);
		PrintWriter out = new PrintWriter(new FileOutputStream(pathfile, true));
		out.print(pathFileBuffer);
		out.flush();
		out.close();
	}
	
	

	/**
	 * Checks if is exist file.
	 *
	 * @param fileFullPath the file full path
	 * @return true, if is exist file
	 */
	public static boolean isExistFile(String fileFullPath) {
		if (fileFullPath == null)
			return false;
		boolean flag = true;
		File f = new File(fileFullPath);
		if (!f.exists())
			flag = false;
		return flag;
	}

	/**
	 * Disable the weight for the given msc spec and output a new file path.
	 *
	 * @param dir the dir
	 * @param mscName the msc name
	 * @return the temporal generated no-weighted msc specification file
	 * @throws FileNotFoundException the file not found exception
	 */
	public static String disableWeighted(String dir, String mscName) throws FileNotFoundException {
		String outputMsc = dir + mscName + "_gen_noweighted.xml";
		String inputMsc = dir + mscName + ".xml";
		File input = new File(inputMsc);
		File output = new File(outputMsc);
		PrintWriter printer = new PrintWriter(output);
		Scanner sc = new Scanner(input);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.contains("weighted=\"true\"")) {
				line = line.replace("true", "false");
			}
			printer.write(line);
		}
		printer.flush();
		sc.close();
		printer.close();
		return outputMsc;
	}

	/**
	 * read cost specification from file and convert to hashmap.
	 *
	 * @param costSpecPath the cost spec path
	 * @return the cost hash
	 * @throws Exception the exception
	 */
	public static HashMap<String, Integer> getCostHash(String costSpecPath) throws Exception {
		HashMap<String, Integer> costMapHash = new HashMap<String, Integer>();
		BufferedReader br = null;
		String line = null;
		br = new BufferedReader(new FileReader(costSpecPath));
		line = br.readLine();
		while (line != null) {
			String alpha = line.substring(0, line.indexOf(":"));
			alpha = alpha.trim();
			String w = line.substring(line.indexOf(":") + 1).trim();
			w = w.trim();
			logger.debug("w string " + w);
			int intw = Integer.parseInt(w);
			logger.debug("w integer " + intw);
			costMapHash.put(alpha, Integer.parseInt(w));
			line = br.readLine();
		}
		return costMapHash;
	}

	/**
	 * Read file to sb.
	 *
	 * @param filePath the file path
	 * @return the string buffer
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static StringBuffer readFileToSB(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		StringBuilder sb = new StringBuilder();
		try {
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
//			String everything = sb.toString();
		} finally {
			br.close();
		}
		return new StringBuffer(sb.toString());
	}
}
