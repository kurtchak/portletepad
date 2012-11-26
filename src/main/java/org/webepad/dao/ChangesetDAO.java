package org.webepad.dao;

import java.util.List;

import org.webepad.model.Changeset;
import org.webepad.model.Pad;

public interface ChangesetDAO {

	public List<Changeset> readChangesets();

	public List<Changeset> readChangesetsFromNumber(Long padId, int number);

	public Changeset getChangeset(Long id);

	public void insert(Changeset changeset);

	public void update(Changeset changeset);

	public void delete(Changeset changeset);

	public List<Changeset> readChangesets(Pad pad);

//	public List<Changeset> readActiveChangesets();

}
