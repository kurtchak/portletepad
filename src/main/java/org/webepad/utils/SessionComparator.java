package org.webepad.utils;

import java.util.Comparator;

import org.webepad.model.Session;

public class SessionComparator implements Comparator<Session> {
	public enum Order {
		LastSeen, User, Pad, Color
	}

	private Order sortingBy = Order.LastSeen;

	@Override
	public int compare(Session s1, Session s2) {
		switch (sortingBy) {
		case LastSeen:
			return s2.getLastSeen().compareTo(s1.getLastSeen()); // reverse order - FROM LATEST TO OLDEST
		case User:
			return s1.getUser().getId().compareTo(s2.getUser().getId());
		case Pad:
			return s1.getPad().getId().compareTo(s2.getPad().getId());
		}
		return 0;
	}

	public void setSortingBy(Order sortingBy) {
		this.sortingBy = sortingBy;
	}
}