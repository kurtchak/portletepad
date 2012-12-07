package org.webepad.utils;

import java.awt.Color;
import java.util.LinkedList;

public class PadColorPalette {
	private static PadColorPalette instance;
	
	public static PadColorPalette getInstance() {
		if (instance == null) {
			instance = new PadColorPalette();
		}
		return instance;
	}

	public static LinkedList<String> getColors() {
		LinkedList<String> colors = new LinkedList<String>();
		colors.add("#66CCFF");
		colors.add("#FF0000");
		colors.add("#00FF00");
		colors.add("#FFFF00");
		colors.add("#00FFFF");
		colors.add("#FF00FF");
		colors.add("#C0C0C0");
		return colors;
	}
	
	public static Color parseColor(String hex) {
		return Color.decode(hex);
	}
}
