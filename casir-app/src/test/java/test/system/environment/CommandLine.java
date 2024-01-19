package test.system.environment;

import java.io.IOException;

public class CommandLine {
	String scriptPath;
	
	public CommandLine() {
		 scriptPath="./casestudy/thesis/furniture_maker/scenario_2/run.sh";

	}

	public static void main(String[] args) {
		CommandLine cl=new CommandLine();
		System.getProperty("os.name"); 
		Runtime rt=Runtime.getRuntime();
		String[] cmdScript = new String[]{cl.scriptPath};
		Process pr;
		try {
			System.out.println(cl.scriptPath);
			 pr = Runtime.getRuntime().exec(cmdScript);
			 pr.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

}
