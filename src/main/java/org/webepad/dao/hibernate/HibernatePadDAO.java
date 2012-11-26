package org.webepad.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.dao.PadDAO;
import org.webepad.model.Changeset;
import org.webepad.model.Pad;
import org.webepad.persistence.HibernateUtil;

public class HibernatePadDAO implements PadDAO {

	private Logger log = LoggerFactory.getLogger(HibernatePadDAO.class);
	
	private org.hibernate.Session getSession() {
		return HibernateUtil.getSession();
	}
	
	public List<Pad> readPads() {
		@SuppressWarnings("unchecked")
		List<Pad> pads = getSession().createQuery("from Pad").list();
		return pads;
	}

	public Pad getPad(Long id) {
		return (Pad) getSession().load(Pad.class, id);
	}
	
	public void insert(Pad pad) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.persist(pad);
		t.commit();
	}
	
	public void update(Pad pad) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.update(s.merge(pad));
		t.commit();
	}
	
	public void delete(Pad pad) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.delete(s.merge(pad));
		t.commit();
	}

	public void delete(Long id) {
		Session s = getSession();
		Pad pad = getPad(id);
		if (pad != null) {
			Transaction t = s.beginTransaction();
			s.delete(s.merge(pad));
			t.commit();
		}
	}

	public List<Changeset> readChangesets(Long padId, int revision) {
		@SuppressWarnings("unchecked")
		List<Changeset> changesets = getSession()
				.createQuery("from Changeset as c where c.pad.id = :padId AND c.number > :revision")
				.setLong("padId", padId).setInteger("revision", revision)
				.list();
		return changesets;
	}
	
	public List<Session> readUserSessions(Long padId) {
		@SuppressWarnings("unchecked")
		List<Session> sessions = getSession().createQuery("from Session as s where pad.id = :padId").setLong("padId", padId).list();
		return sessions;
	}

//	@Override
//	public List<Pad> readActivePads() {
//		@SuppressWarnings("unchecked")
//		List<Pad> pads = getSession().createQuery("from Pad as p inner join p.sessions as s where s.active = :active").setBoolean("active", true).list();
//		return pads;
//	}
}
