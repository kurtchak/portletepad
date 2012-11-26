package org.webepad.utils;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandler {
	private static Logger log = LoggerFactory.getLogger(ExceptionHandler.class);
  
	public static void handle(Exception e) {
		// trimming the stacktrace to only 5 lines
		ArrayList<StackTraceElement> list = new ArrayList<StackTraceElement>();
		for (int i=0; i<5; i++) {
			list.add(e.getStackTrace()[i]);
		}
		e.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
		handle(e, e.getMessage(), true);
	}

	public static void handle(Exception e, String msg, boolean printStackTrace) {
		log.error(e.getClass().getSimpleName() + " - " + msg);
		if (printStackTrace) {
			e.printStackTrace();
		}
	}
}
