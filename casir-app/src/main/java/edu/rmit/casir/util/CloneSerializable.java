package edu.rmit.casir.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CloneSerializable {
	public static Object clone(Serializable obj) {
		ObjectInputStream is = null;
		ObjectOutputStream os = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);
			os.writeObject(obj);
			ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
			is = new ObjectInputStream(bin);
			Object clone = is.readObject();
			return clone;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (os != null)
					os.close();
				if (is != null)
					is.close();
			} catch (Exception ex) {
			}
		}
		return null;
	}

}
