package org.webepad.beans;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.html.HtmlDataTable;

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
	private PadDAO padDAO = HibernateDAOFactory.getInstance().getPadDAO();
	private ChangesetDAO changesetDAO = HibernateDAOFactory.getInstance().getChangesetDAO();

	private String padname;
	private String number;
	private String text;
	private Pad pad;
	
	private HtmlDataTable padListTable;

	public PadBean() {
	}
	
	//////////////////////////////////////////////
	// LOADING OF THE MAIN OBJECT FOR THE VIEW
	public void loadPad(Long id) {
		this.pad = padDAO.getPad(id);
	}

	public void loadPad(Pad pad) {
		this.pad = pad;
	}

	public void loadListedPad() {
		pad = (Pad) padListTable.getRowData();
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

	/**
	 * Removal of selected Pad
	 */
	public void deteleSelectedPad() {
		deletePad((Pad) padListTable.getRowData());
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

	public void save(Pad pad) {
		padDAO.insert(pad);
	}

	public void update(Pad pad) {
		padDAO.update(pad);
	}

	public void deletePad(Long id) {
		padDAO.delete(id);
	}

	public void deletePad(Pad pad) {
		padDAO.delete(pad);
	}

	public Session openSession(Pad pad, User user) {
		if (pad != null) {
			setPad(pad);
			return pad.openSession(user);
		}
		return null;
	}

	public Session openSession(Session session) {
		if (session != null) {
			setPad(session.getPad());
			return pad.openSession(session);
		}
		return null;
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

	public HtmlDataTable getPadListTable() {
		return padListTable;
	}

	public void setPadListTable(HtmlDataTable padListTable) {
		this.padListTable = padListTable;
	}

	public ChangesetDAO getChangesetDAO() {
		return changesetDAO;
	}

	public void setChangesetDAO(ChangesetDAO changesetDAO) {
		this.changesetDAO = changesetDAO;
	}
}