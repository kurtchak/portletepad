package org.webepad.exceptions;

public class RangeOutOfDimensionsException extends Exception {
	private static final long serialVersionUID = 1L;

	public RangeOutOfDimensionsException(String text, int start, int end) {
		super("Given range exceeds the dimensions of the text: text -> " + text + " start -> " + start + " end -> " + end); 
	}
}
