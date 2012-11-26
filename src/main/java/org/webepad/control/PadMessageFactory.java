package org.webepad.control;

import org.webepad.beans.SessionBean;
import org.webepad.model.Changeset;

public interface PadMessageFactory {

	public void publishMessage(String msg);

	public void publishChangeset(Changeset c);

	public void processMessage(String msg);
	
	/**
	 * Leaves the session publishing the leave message and terminate sessions
	 * sender and listener
	 */
	public void closeConnection();
	
	/**
	 * Initializes this sessions listener and sender for communication with other sessions
	 * and publishes the join message to the topic
	 * @param sessionBean
	 */
	public void initConection(SessionBean sessionBean);
}
