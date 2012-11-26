package org.webepad.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.webepad.dao.UserDAO;
import org.webepad.model.User;
import org.webepad.persistence.HibernateUtil;

public class HibernateUserDAO implements UserDAO {
	// TODO: EFFECIVITY OF COMMUNICATION - EVERYTIME NEW SESSION OPENED - IS IT
	// GOOD?
	private org.hibernate.Session getSession() {
		return HibernateUtil.getSession();
	}

	public List<User> readUsers() {
		@SuppressWarnings("unchecked")
		List<User> users = getSession().createQuery("from User").list();
		return users;
	}

	public User getUser(Long id) {
		return (User) getSession().load(User.class, id);
	}

	public void insert(User user) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.persist(user);
		t.commit();
	}

	public void update(User user) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.merge(user);
		t.commit();
	}

	public void delete(User user) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.delete(user);
		t.commit();
	}

	public User findUser(String name) {
		User user;
//		try {
			user = (User) getSession().createQuery("from User u where u.name = :name").setParameter("name", name).uniqueResult();
//		} catch NonUniqueResultException() {
//			
//		}
		return user;
	}

}
