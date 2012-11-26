package org.webepad.utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class PadColorUtils {
	private static PadColorUtils instance;
	private static Set<String> colors;
	
	private PadColorUtils() {
		colors = new HashSet<String>();
		colors.add("#66CCFF");
		colors.add("#FF0000");
		colors.add("#00FF00");
		colors.add("#FFFF00");
		colors.add("#00FFFF");
		colors.add("#FF00FF");
		colors.add("#C0C0C0");
	}
	
	public static PadColorUtils getInstance() {
		if (instance == null) {
			instance = new PadColorUtils();
		}
		return instance;
	}

	public Set<String> getColors() {
		return colors;
	}
	
	public static Color parseColor(String hex) {
		return Color.decode(hex);
	}
}
