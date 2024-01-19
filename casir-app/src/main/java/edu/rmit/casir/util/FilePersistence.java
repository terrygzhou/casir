package edu.rmit.casir.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FilePersistence {

	public FilePersistence() {
	}

	public static void write(String objFile, Object obj) {

		try {
			FileOutputStream f = new FileOutputStream(new File(objFile));
			ObjectOutputStream o = new ObjectOutputStream(f);
			o.writeObject(obj);
			o.close();
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static Object read(String objFile) {
		try {
			FileInputStream fi = new FileInputStream(new File(objFile));
			ObjectInputStream oi = new ObjectInputStream(fi);
			Object obj = oi.readObject();
			return obj;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
