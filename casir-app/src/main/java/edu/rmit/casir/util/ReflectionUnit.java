package edu.rmit.casir.util;

import java.lang.reflect.Field;

public class ReflectionUnit {

	public static Object getVariableValue(Class<?> c, String fieldName) {

		Object result = null;
		try {
			Field f = c.getDeclaredField(fieldName);
			f.setAccessible(true);
			if (f.isAccessible()) {
				result = f.get(null);
			} else {
				return null;
			}

		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;

	}
}
