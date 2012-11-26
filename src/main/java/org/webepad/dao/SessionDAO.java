package org.webepad.dao;

import java.util.List;

import org.webepad.model.Changeset;
import org.webepad.model.Session;

public interface SessionDAO {

	public List<Session> readSessions();

	public List<Session> readActiveSessions();

	public Session getSession(Long id);

	public void insert(Session session);

	public void update(Session session);

	public void delete(Session session);

	public Session findSession(Long padId, Long userId);

	public Session findSession(Changeset changeset);

}
