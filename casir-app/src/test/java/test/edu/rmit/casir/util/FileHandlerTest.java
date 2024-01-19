package test.edu.rmit.casir.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;

import edu.rmit.casir.util.FileHandler;

public class FileHandlerTest {
	String dir="./casestudy/AbstractSample/";
	String mscName="RootA";
	String fullPath="./casestudy/AbstractSample/RootA.xml";
	Logger logger=Logger.getLogger(FileHandlerTest.class);
	
//	@Test
	public void testDisableWeighted() {
		String outputPath=null;
		try {
			outputPath=FileHandler.disableWeighted(dir, mscName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		File f= new File(outputPath);
		Assert.assertNotNull(f);
	}

	
//	@Test
	public void testFileName(){
		String dir=FileHandler.getDirFromFullPath(fullPath);
		logger.info(dir);
		String filename=FileHandler.getFileNoExtFromFullPath(fullPath);
		logger.info(filename);
		String filenameExt=FileHandler.getFilenameWithExtFromFullPath(fullPath);
		logger.info(filenameExt);
	}
	
//	@Test
	public void testString(){
		String sample="Client_TravelAgent||M_TravelAgent||R_TA||R_AL||R_BANK";
		
		String[] arr=sample.split("[||]");
		for(String a:arr){
			logger.info(a+"\t");
		}
	}
	

	
	@Test
	public void testBufferedReader() throws FileNotFoundException{
		 String fspFile="/Volumes/SANDISK/PhD/phd_thesis/overview/ltsa_pca/composite.pca";
		 BufferedReader br=new BufferedReader(new FileReader(fspFile));
		 String line=null;
		 try {
			while((line=br.readLine()) != null){
				logger.info(line); 
			 }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
