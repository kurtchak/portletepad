package org.webepad.utils;

import org.webepad.control.TextSlice;

public class TextSliceTuple<X, Y> extends Tuple<TextSlice, Integer> {
	public TextSliceTuple(TextSlice x, Integer y) {
		super(x, y);
	}
	public TextSlice getTextSlice() {
		return (TextSlice) this.x;
	}
	public Integer getOffset() {
		return (Integer) this.y;
	}
}

class Tuple<X,Y> {
	public final X x;
	public final Y y;
	public Tuple(X x, Y y) {
		this.x = x;
		this.y = y;
	}
};

