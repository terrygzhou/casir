package edu.rmit.casir.prism;

import java.io.File;
import java.io.FileNotFoundException;

import parser.ast.ModulesFile;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;

public class PrismUtil {

	private static Prism getPrism(String modelFile) throws PrismException, FileNotFoundException {
		PrismLog mainLog;
		Prism prism;
		mainLog = new PrismFileLog("stdout");
		prism = new Prism(mainLog, mainLog);
		prism.initialise();
		ModulesFile modulesFile = prism.parseModelFile(new File(modelFile));
		prism.loadPRISMModel(modulesFile);
		return prism;

	}

	public static void generateDot(String modelFilePath, String dotPath){
		Prism prism;
		try {
			prism = getPrism(modelFilePath);
			prism.exportTransToFile(true, Prism.EXPORT_DOT, new File(dotPath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PrismException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void genStateDot(String modelFilePath, String dotPath){
		try {
			Prism prism =getPrism(modelFilePath);
			prism.exportTransToFile(true, Prism.EXPORT_DOT_STATES, new File(dotPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (PrismException e) {
			e.printStackTrace();
		}
	}
	
	
}
