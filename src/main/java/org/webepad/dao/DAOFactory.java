package org.webepad.dao;

public interface DAOFactory {
	public PadDAO getPadDAO();

	public UserDAO getUserDAO();

	public SessionDAO getSessionDAO();
}
