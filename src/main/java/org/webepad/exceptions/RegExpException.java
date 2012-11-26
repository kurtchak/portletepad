package org.webepad.exceptions;

public class RegExpException extends Exception {
	private static final long serialVersionUID = -2126422645404149506L;

	public RegExpException() {
		super();
	}

	public RegExpException(String rule, String changePattern) {
		super("REGEXPEXCEPTION: String '"+rule+"' doesn't match pattern '"+changePattern+"'");
	}
}
