package org.webepad.dao.hibernate;

import org.webepad.dao.ChangesetDAO;
import org.webepad.dao.DAOFactory;
import org.webepad.dao.PadDAO;
import org.webepad.dao.SessionDAO;
import org.webepad.dao.UserDAO;

public class HibernateDAOFactory implements DAOFactory {

	private static HibernateDAOFactory instance;
	static {
		instance = new HibernateDAOFactory();
	}

	public static HibernateDAOFactory getInstance() {
		return instance;
	}

	public PadDAO getPadDAO() {
		return new HibernatePadDAO();
	}

	public UserDAO getUserDAO() {
		return new HibernateUserDAO();
	}

	public SessionDAO getSessionDAO() {
		return new HibernateSessionDAO();
	}

	public ChangesetDAO getChangesetDAO() {
		return new HibernateChangesetDAO();
	}

}
