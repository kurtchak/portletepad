package org.webepad.beans;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static Map<Long,Pad> activePads = new HashMap<Long, Pad>();
	
	private String padname;
	private String number;
	private String text;
	private Pad pad;
	
	@ManagedProperty(value="#{userBean}")
	public UserBean userBean;
	
	public PadBean() {
	}
	
	//////////////////////////////////////////////
	// LOADING OF THE MAIN OBJECT FOR THE VIEW
	public void loadPad(Long id) {
		loadPad(padDAO.getPad(id));
	}

	public void loadPad(Pad pad) {
		this.pad = pad;
	}

	/**
	 * Creation of new Pad
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

	public Pad getPad(Long id) {
		return padDAO.getPad(id);
	}

	public List<Pad> getPads() {
		return padDAO.readPads();
	}
	
	public List<Changeset> retrieveNewChangesets() {
		return padDAO.readChangesets(pad.getId(), pad.getLastRevisionNumber());
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
		padDAO.delete(pad);
	}

	public synchronized Session openSession(Pad pad, User user) {
		if (pad != null) {
			pad = addActivePad(pad);
			loadPad(pad);
			return pad.openSession(user);
		}
		return null;
	}

	public synchronized Session openSession(Session session) {
		if (session != null) {
			Pad pad = addActivePad(session.getPad());
			session.setPad(pad);
			loadPad(pad);
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
			log.info("PAD:"+pad.getName()+" ALREADY ADDED TO >> ACTIVE PADS. RETRIEVING MANAGED PAD WITH HASHCODE ["+pad.hashCode()+"]");
		} else {
			activePads.put(pad.getId(), pad);
			log.info("PAD:"+pad.getName()+" ADDED TO >> ACTIVE PADS WITH HASHCODE ["+pad.hashCode()+"]");
		}
		return pad;
	}

	public synchronized void removeActivePad(Pad pad) {
		activePads.remove(pad.getId());
		log.info("PAD:"+pad.getName()+" REMOVED FROM << ACTIVE PADS.");
	}

	public Pad getPad() {
		return pad;
	}

	public void setPad(Pad pad) {
		this.pad = pad;
	}

	public String getPadname() {
		return padname;
	}

	public void setPadname(String padname) {
		this.padname = padname;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}

	public boolean isOnlyMyChangesets() {
		if (pad != null) {
			for (Changeset c : pad.getChangesets()) {
				if (!c.getCreator().equals(userBean.getActualUser())) {
					return false; 
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public Changeset getChangeset(Long id) {
		return changesetDAO.getChangeset(id);
	}
}