package org.webepad.dao.hibernate;

import java.util.List;

import org.hibernate.Transaction;
import org.webepad.dao.SessionDAO;
import org.webepad.model.Changeset;
import org.webepad.persistence.HibernateUtil;

public class HibernateSessionDAO implements SessionDAO {

	private org.hibernate.Session getSession() {
		return HibernateUtil.getSession();
	}
	public List<org.webepad.model.Session> readSessions() {
		@SuppressWarnings("unchecked")
		List<org.webepad.model.Session> sessions = getSession().createQuery("from Session").list();
		return sessions;
	}

	public org.webepad.model.Session getSession(Long id) {
		return (org.webepad.model.Session) getSession().load(org.webepad.model.Session.class, id);
	}
	
	public org.webepad.model.Session findSession(Changeset c) {
		return findSession(c.getPad().getId(), c.getUser().getId());
	}
	
	public org.webepad.model.Session findSession(Long padId, Long userId) {
		return (org.webepad.model.Session) getSession().createQuery("from Session as s Where s.pad.id = :padId and s.user.id = :userId").setLong("padId", padId).setLong("userId", userId).uniqueResult();
	}
	
	public void insert(org.webepad.model.Session session) {
		org.hibernate.Session s = getSession();
		s.getTransaction().begin();
		s.persist(session);
		s.getTransaction().commit();
	}
	
	public void update(org.webepad.model.Session session) {
		org.hibernate.Session s = getSession();
		Transaction t = s.beginTransaction();
		s.update(s.merge(session));
		t.commit();
	}
	
	public void delete(org.webepad.model.Session session) {
		org.hibernate.Session s = getSession();
		Transaction t = s.beginTransaction();
		s.delete(session);
		t.commit();
	}

	public List<org.webepad.model.Session> readActiveSessions() {
		@SuppressWarnings("unchecked")
		List<org.webepad.model.Session> sessions = getSession().createQuery("from Session as s where s.active = :active").setBoolean("active", true).list();
		return sessions;
	}

}
