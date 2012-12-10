package org.webepad.dao;

import java.util.List;

import org.hibernate.Session;
import org.webepad.model.Changeset;
import org.webepad.model.Pad;

public interface PadDAO {

	public List<Pad> readPads();

	public Pad getPad(Long id);

	public void insert(Pad pad);

	public void update(Pad pad);

	public Pad unify(Pad pad);

	public void delete(Pad pad);

	public void delete(Long id);

	// reads changesets after revision with number revision
	public List<Changeset> readChangesets(Long padId, int revision);

	//	public List<Pad> readActivePads();

	public List<Session> readUserSessions(Long padId);

}
