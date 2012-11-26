package org.webepad.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.webepad.dao.ChangesetDAO;
import org.webepad.model.Changeset;
import org.webepad.model.Pad;
import org.webepad.persistence.HibernateUtil;

public class HibernateChangesetDAO implements ChangesetDAO {

	private org.hibernate.Session getSession() {
		return HibernateUtil.getSession();
	}

	private StatelessSession getStatelessSession() {
		return HibernateUtil.openStatelessSession();
	}

	public List<Changeset> readChangesets() {
		@SuppressWarnings("unchecked")
		List<Changeset> changesets = getSession().createQuery("from Changeset").list();
		return changesets;
	}

	public List<Changeset> readChangesetsFromNumber(Long padId, int number) {
		@SuppressWarnings("unchecked")
		List<Changeset> changesets = getSession().createQuery("from Changeset as c where c.pad.id = :padId and c.number > :number").setLong("padId", padId).setInteger("number", number).list();
		return changesets;
	}

	public Changeset getChangeset(Long id) {
		return (Changeset) getSession().load(Changeset.class, id);
	}

	public void insert(Changeset changeset) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.persist(changeset);
		t.commit();
	}

	public void update(Changeset changeset) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.merge(changeset);
		t.commit();
	}

	public void delete(Changeset changeset) {
		Session s = getSession();
		Transaction t = s.beginTransaction();
		s.delete(s.merge(changeset));
		t.commit();
	}

	public List<Changeset> readChangesets(Pad pad) {
		if (pad != null) {
			return readChangesets(pad.getId());
		} else {
			return null;
		}
	}
	
	public List<Changeset> readChangesets(Long padId) {
		@SuppressWarnings("unchecked")
		List<Changeset> changesets = getStatelessSession().createQuery("from Changeset as c where c.pad.id = :padId").setLong("padId", padId).list();
		return changesets;
	}

	// @Override
	// public List<Changeset> readActiveChangesets() {
	// @SuppressWarnings("unchecked")
	// List<Changeset> changesets =
	// getSession().createQuery("from Changeset as p inner join p.sessions as s where s.active = :active").setBoolean("active",
	// true).list();
	// return changesets;
	// }
}
