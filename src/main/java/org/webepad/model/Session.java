package org.webepad.model;

import java.awt.Color;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.beans.SessionBean;
import org.webepad.control.CustomPadMessageFactory;
import org.webepad.control.PadAssembler;
import org.webepad.control.PadContent;
import org.webepad.control.PadMessageFactory;
import org.webepad.control.TextSlice;
import org.webepad.dao.SessionDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.exceptions.MalformedMessage;
import org.webepad.utils.DateUtils;
import org.webepad.utils.ExceptionHandler;

/**
 * The persistent class for the Session database table.
 * 
 */
public class Session extends TemporalEntity {
	private static final long serialVersionUID = -1299346837237381381L;

	private Logger log = LoggerFactory.getLogger(Session.class);

	private SessionDAO sessionDAO = HibernateDAOFactory.getInstance().getSessionDAO();
	
	private String colorCode;
	private Color color;
	private boolean colored;
	private Date opened;
	private Date lastSeen;
	private Pad pad;
	private PadAssembler padAssembler;
	private PadMessageFactory messageFactory = new CustomPadMessageFactory();

	/////////////////////////////////////////
	// CONSTRUCTORS
	public Session() {
	}
	
	public Session(User user, Pad pad) {
		this.setUser(user);
		this.pad = pad;
		this.colorCode = pad.notUsedColor();
		setCreated(DateUtils.now());
		this.lastSeen = getCreated();
	}
	
	/**
	 * Opens the session with initialization of pad constructed from changesets
	 */
	public void open() {
		buildPadContent();
		setOpened(DateUtils.now());
	}

	/**
	 * Leaves the session publishing the leave message and terminate sessions
	 * sender and listener
	 */
	public void close() {
		setLastSeen(DateUtils.now());
		update();
		messageFactory.publishMessage(leaveMessage());
		messageFactory.closeConnection();
	}
	
	/**
	 * Initializes this sessions listener and sender for communication with other sessions
	 * and publishes the join message to the topic
	 * @param sessionBean
	 */
	public void startListener(SessionBean sessionBean) {
		messageFactory.initConection(sessionBean);
		messageFactory.publishMessage(joinMessage());
	}
	
	/**
	 * Publishes constructed response message to the topic
	 * @param toPadId
	 * @param toUserId
	 */
	public void sendReponseMessage(Long toPadId, Long toUserId) {
		messageFactory.publishMessage(responseMessage(toPadId, toUserId));
	}
	
	/**
	 * The message that produce the user already joined on pad and sending towards the recently joined user
	 * to inform him about his present. The message is generated after the receive of joinMessage from the
	 * actually joined user
	 * @param toPadId
	 * @param toUserId
	 * @return
	 */
	private String responseMessage(Long toPadId, Long toUserId) {
		StringBuilder sb = new StringBuilder();
		// example message: R:1:2:1:1 ~ from|to
		sb.append("R:").append(pad.getId()).append(":").append(getUser().getId()) 
		.append(":").append(toPadId).append(":").append(toUserId); // R as Response on Join msg
		return sb.toString();
	}

	/**
	 * Join message is published by user actually joined to the pad for o all colaborative users to inform them.
	 * @return
	 */
	private String joinMessage() {
		StringBuilder sb = new StringBuilder();
		// example message: J:1:2
		sb.append("J:").append(pad.getId()).append(":").append(getUser().getId()); // J as Join of user into Pad
		return sb.toString();
	}

	/**
	 * Leave message is published by the user actually leaving the pad to for other joined pad users.
	 * @return
	 */
	private String leaveMessage() {
		StringBuilder sb = new StringBuilder();
		// example message: L:1:2
		sb.append("L:").append(pad.getId()).append(":").append(getUser().getId()); // L as Join of user into Pad
		return sb.toString();
	}

	/**
	 * This message is published on change event occured in the local editor for other pad users to update their editor.
	 * @param changeset
	 * @return
	 */
	private String changesetMessage(Changeset c, int spanId, int spanPos, int leftId, int rightId) {
		StringBuilder sb = new StringBuilder();
		String action = c.getAction() == Changeset.WRITE ? "W" : (c.getAction() == Changeset.DELETE ? "D" : "X");
		sb.append("C:").append(action).append(":").append(c.getId()).append(";");
		if (c.getAttributePool() != null && c.getAttributePool().getAttributeItems() != null) {
			for (AttributeItem item : c.getAttributePool().getAttributeItems()) {
				sb.append(item.getNumber()).append(":").append(item.getAttribute()).append("=").append(item.getValue());
			}
		}
		sb.append("_").append(spanId).append(":").append(spanPos).append("_");
		sb.append("[").append(leftId).append(",").append(rightId).append("]");
		return sb.toString(); // C as Changeset created on Pad
	}

	/**
	 * Message tells other users that the user has changed its text color for the current pad.
	 * @param colorCode
	 * @param colorCode 
	 * @return
	 */
	private String colorChangeMessage(String prevColor, String newColor) {
		StringBuilder sb = new StringBuilder();
		sb.append("UC:").append(getUser().getId()).append(":").append(getPad().getId()).append(":").append(prevColor);
		return sb.toString();
	}
	
	/**
	 * Received message is parsed and processed.
	 * @param msg
	 * @param sessionBean
	 * @throws MalformedMessage 
	 */
	public void processMessage(String msg) throws MalformedMessage {
		messageFactory.processMessage(msg);
	}

	/**
	 * Adds local changeset to pad and applies it to produce actual editor content
	 * @param changeset
	 */
	public void addChangeset(Changeset changeset) {
		changeset.setPad(getPad());
		changeset.setAuthor(getUser());
		try {
			pad.appendChangeset(changeset);
			PadContent padContent = padAssembler.applyLocalChangeset(changeset);
			TextSlice touchedTs = padContent.getTouchedTs();
			int spanId = 0, leftId = 0, rightId = 0, spanPos = 0, offset = 0;
			if (touchedTs != null) {
				spanId = touchedTs.getSpanId();
				TextSlice left = padContent.getPrevTextSlice(touchedTs);
				TextSlice right = padContent.getNextTextSlice(touchedTs);
				leftId = left != null && left.isTextSpan() ? left.getSpanId() : 0;
				rightId = right != null && right.isTextSpan() ? right.getSpanId() : 0;
				spanPos = touchedTs.getLastActivePos();
			}
			// TODO: change name changeMessage to generateChangeMessage 
			messageFactory.publishMessage(changesetMessage(changeset, spanId, spanPos, leftId, rightId)); // korekcia pozicie +1
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
	}

//	public void addRemoteChangeset(Changeset changeset) {
//		pad.appendRemoteChangeset(changeset);
//		padAssembler.applyRemoteChangeset(changeset);
//	}

	/**
	 * Builds pad content from loaded changesets
	 */
	public void buildPadContent() {
		try {
			padAssembler = new PadAssembler(this);
			padAssembler.buildViewRepr();
			log.info(padAssembler.getViewRepr());
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
	}

	/**
	 * Appends new changesets (submitted by the remote users) to pad
	 */
	public void updatePad(List<Changeset> changesets) {
		pad.appendRemoteChangesets(changesets);
		updateEditorContent(changesets);
	}
	
	private void updateEditorContent(List<Changeset> changesets) {
		for (Changeset ch : changesets) {
			padAssembler.applyRemoteChangeset(ch);
		}
	}

	// Data manipulation methods
	public void save() {
		sessionDAO.insert(this);
	}
	
	public void update() {
		sessionDAO.update(this);
	}

	// GETTERS AND SETTERS
	public Pad getPad() {
		return pad;
	}

	public void setPad(Pad pad) {
		this.pad = pad;
	}

	public User getUser() {
		return getCreator();
	}

	public void setUser(User user) {
		setCreator(user);
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
		if (colorCode != null && colorCode.length() > 0) {
			this.color = Color.decode(colorCode);
		}
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}

	public boolean isColored() {
		return colored;
	}

	public void setColored(boolean colored) {
		this.colored = colored;
	}

	public Date getLastSeen() {
		return this.lastSeen;
	}

	public void setLastSeen(Date lastSeen) {
		this.lastSeen = lastSeen;
	}

	public String getShortCreated() {
		return DateUtils.getShortDate(getCreated());
	}

	public String getShortLastSeen() {
		return DateUtils.getShortDate(getLastSeen());
	}

	public Boolean isPadCreator() {
		return getUser().equals(pad.getCreator());
	}

	public Boolean getPadCreator() {
		return getUser().equals(pad.getCreator());
	}

	public PadAssembler getPadAssembler() {
		return padAssembler;
	}

	public void setPadAssembler(PadAssembler padAssembler) {
		this.padAssembler = padAssembler;
	}

	public Date getOpened() {
		return opened;
	}

	public void setOpened(Date opened) {
		this.opened = opened;
	}

	public String getShortOpened() {
		Date date = getOpened();
		return DateUtils.getShortDate(date);
	}

	public void changeUserColor(String colorCode) {
		String prevColorCode = getColorCode();
		setColorCode(colorCode);
		update();
		messageFactory.publishMessage(colorChangeMessage(prevColorCode, colorCode));
	}

	public String getColorClass() {
		if (colorCode != null) {
			return "cl"+colorCode.substring(1);
		}
		return null;
	}
}
