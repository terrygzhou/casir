package test.edu.rmit.casir.prism;

import java.io.*;

import parser.ast.*;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.Result;

/**
 * Example class demonstrating how to control PRISM programmatically, i.e. through the "API" exposed by the class
 * prism.Prism. (this now uses the newer version of the API, released after PRISM 4.0.3) Test like this:
 * PRISM_MAINCLASS=prism.PrismTest bin/prism ../prism-examples/polling/poll2.sm ../prism-examples/polling/poll3.sm
 */
public class TestPrism {
	
//	String moduleFilePath="/Users/terryzhou/PhD/conference/2014/CBSE2014/model/controllerNdevice.nm";
	String filename="RootA";
	String dir="./casestudy/AbstractSample/";
	String moduleFilePath=dir+filename+".nm";
	
	String dotFile="./output/"+filename+".dot";
	String stateDotFile="./output/"+filename+"_state.dot";
	
	public static void main(String args[]) {
		new TestPrism().go(args);
	}

	public void go(String args[]) {
		try {
			PrismLog mainLog;
			Prism prism;
			ModulesFile modulesFile;
			PropertiesFile propertiesFile;
			Result result;

			// Init
			mainLog = new PrismFileLog("stdout");
			prism = new Prism(mainLog, mainLog);
			prism.initialise();

			// Parse/load model 1
			// NB: no need to build explicitly - it will be done if/when neeed
			modulesFile = prism.parseModelFile(new File(this.moduleFilePath));
			prism.loadPRISMModel(modulesFile);
			prism.exportTransToFile(true, Prism.EXPORT_DOT, new File(this.dotFile)); //export the model to state transition dot graph
			prism.exportTransToFile(true, Prism.EXPORT_DOT_STATES, new File(this.stateDotFile));

			// Parse a prop, check on model 1
			propertiesFile = prism.parsePropertiesString(modulesFile, "Pmin = ? [F<=1 s=1]");
			result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
			System.out.println(result.getResult()+"\t"+result.getCounterexample().toString());
			// Close down
			prism.closeDown();
		} catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}
