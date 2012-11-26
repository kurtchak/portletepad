package org.webepad.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// TODO: REPLACE WITH === JODA ===
public class DateUtils {

	// Explicit private constructor preventing the instantiation of this class
	private DateUtils() {}
	
	public static Date now() {
		return Calendar.getInstance().getTime();
	}

	public static boolean isToday(Date date) {
		Calendar now = Calendar.getInstance();
		Calendar dat = Calendar.getInstance();
		now.setTime(now());
		dat.setTime(date);
		if (now.get(Calendar.YEAR) == dat.get(Calendar.YEAR)
				&& now.get(Calendar.DAY_OF_YEAR) == dat.get(Calendar.DAY_OF_YEAR)) {
			return true;
		} else {
			return false;
		}
	}

	private static String getDayTimeString(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return dateFormat.format(cal.getTime());
	}

	private static String getDayWithoutTimeString(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return dateFormat.format(cal.getTime());
	}

	public static String getShortDate(Date date) {
		if (date != null) {
			if (DateUtils.isToday(date)) {
				return getDayTimeString(date);
			} else {
				return getDayWithoutTimeString(date);
			}
		} else {
			return "Unknown";
		}
	}
}
