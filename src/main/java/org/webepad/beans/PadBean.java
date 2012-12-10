package org.webepad.beans;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.control.PushControl;
import org.webepad.dao.ChangesetDAO;
import org.webepad.dao.PadDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.model.Changeset;
import org.webepad.model.Pad;
import org.webepad.model.Session;
import org.webepad.model.User;
import org.webepad.utils.DateUtils;

@ManagedBean(name = "padBean")
@SessionScoped
public class PadBean {
	private static Logger log = LoggerFactory.getLogger(PadBean.class);
	private PadDAO padDAO = HibernateDAOFactory.getInstance().getPadDAO();
	private ChangesetDAO changesetDAO = HibernateDAOFactory.getInstance().getChangesetDAO();
	private static Map<Long, Pad> activePads = new HashMap<Long, Pad>();

	private Pad pad;
	private PushControl pushControl;

	public PadBean() {
		initializePushTopic();
	}

	private void initializePushTopic() {
		if (pushControl == null) {
			pushControl = new PushControl();
		}
		pushControl.initializeTopic();
	}

	// ////////////////////////////////////////////
	// LOADING OF THE MAIN OBJECT FOR THE VIEW
	public void loadPad(Long id) {
		Pad pad = padDAO.getPad(id);
		loadPad(pad);
	}

	public void loadPad(Pad pad) {
		this.pad = pad;
	}

	public Pad getPad(Long id) {
		return padDAO.getPad(id);
	}

	public List<Pad> getPads() {
		return padDAO.readPads();
	}

	public synchronized Collection<Pad> getActivePads() {
		if (activePads != null) {
			return activePads.values();
		}
		return null;
	}

	/**
	 * Pad CRUD
	 */
	public void save(Pad pad) {
		padDAO.insert(pad);
	}

	public void update(Pad pad) {
		padDAO.update(pad);
	}

	public void deletePad(Pad pad) {
		if (activePads.containsKey(pad.getId())) {
			activePads.get(pad.getId()).setReadOnly(true);
		}
		padDAO.delete(pad);
	}

	/**
	 * Creation of new Pad
	 * 
	 * @param name
	 * @param creator
	 * @return
	 */
	public Pad createNewPad(String name, User creator) {
		Pad pad = new Pad();
		pad.setName(name);
		pad.setCreated(DateUtils.now());
		pad.setUser(creator);
		save(pad);
		return pad;
	}

	public synchronized Session openSession(Pad pad, User user, int access) {
		if (pad != null) {
			pad.setPushControl(pushControl); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
			if (access == APIBean.READWRITE) {
				pad = addActivePad(pad);
			} else {
				pad.setReadOnly(true);
			}
			loadPad(pad);
			return pad.openSession(user);
		}
		return null;
	}

	public synchronized Session openSession(Session session) {
		if (session != null) {
			if (activePads.containsKey(session.getPad().getId())) {
				session.getPad().setPushControl(pushControl); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
			}
			Pad pad = addActivePad(session.getPad());
			loadPad(pad);
			session.setPad(pad);
			return pad.openSession(session);
		}
		return null;
	}

	public synchronized void closeSession(Session session) {
		if (session != null) {
			session.close();
			Collection<Session> activePadSessions = session.getPad().getActivePadSessions();
			if (activePadSessions == null || activePadSessions.isEmpty()) {
				removeActivePad(session.getPad());
			}
		}
	}

	private synchronized Pad addActivePad(Pad pad) {
		if (activePads.containsKey(pad.getId())) {
			pad = activePads.get(pad.getId());
			log.info("PAD:"
					+ pad.getName()
					+ " ALREADY ADDED TO >> ACTIVE PADS. RETRIEVING MANAGED PAD WITH HASHCODE ["
					+ pad.hashCode() + "]");
		} else {
			activePads.put(pad.getId(), pad);
			log.info("PAD:" + pad.getName()
					+ " ADDED TO >> ACTIVE PADS WITH HASHCODE ["
					+ pad.hashCode() + "]");
		}
		return pad;
	}

	public synchronized void removeActivePad(Pad pad) {
		activePads.remove(pad.getId());
		log.info("PAD:" + pad.getName() + " REMOVED FROM << ACTIVE PADS.");
	}

	public Pad getPad() {
		return pad;
	}

	public void setPad(Pad pad) {
		this.pad = pad;
	}

	public PushControl getPushBean() {
		return pushControl;
	}

	public void setPushBean(PushControl pushBean) {
		this.pushControl = pushBean;
	}

	public Changeset getChangeset(Long id) {
		return changesetDAO.getChangeset(id);
	}

	public void togglePadLock(Pad pad) {
		if (activePads.containsKey(pad.getId())) {
			pad = activePads.get(pad.getId());
		}
		pad.setReadOnly(!pad.isReadOnly());
	}

	public String getTopicAddress() {
		if (pushControl != null) {
			return pushControl.getTopicAddress();
		}
		return null;
	}
}