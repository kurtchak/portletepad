package org.webepad.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import org.richfaces.application.push.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.beans.PushBean;
import org.webepad.beans.SessionBean;
import org.webepad.control.PadContent;
import org.webepad.control.TextSlice;
import org.webepad.dao.SessionDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.utils.DateUtils;
import org.webepad.utils.ExceptionHandler;

/**
 * The persistent class for the Session database table.
 * 
 */
public class Session extends TemporalEntity {
	private static final long serialVersionUID = -1299346837237381381L;
	private static Logger log = LoggerFactory.getLogger(Session.class);

	private SessionDAO sessionDAO = HibernateDAOFactory.getInstance().getSessionDAO();
	private String colorCode;
	private Color color;
	private Boolean colored;
	private Date opened;
	private Date lastSeen;
	private Pad pad;
	private PushBean pushBean;
	private LinkedList<String> remoteChangesetsQueue = new LinkedList<String>();

	/////////////////////////////////////////
	// CONSTRUCTORS
	public Session() {
	}
	
	public Session(User user, Pad pad) throws Exception {
		setUser(user);
		this.pad = pad;
		colorCode = pad.getFreeColor();
		setCreated(DateUtils.now());
		lastSeen = getCreated();
	}
	
	/**
	 * Opens the session with initialization of pad constructed from changesets
	 */
	public void open(PushBean pushBean) {
		this.pushBean = pushBean;
		this.pushBean.initializeTopic();
		setOpened(DateUtils.now());
		pad.addActiveSession(this);
	}

	/**
	 * Leaves the session publishing the leave message and terminate sessions
	 * sender and listener
	 */
	public void close() {
		setLastSeen(DateUtils.now());
		update();
		pad.removeActiveSession(this);
	}
	
	/**
	 * Adds local changeset to pad and applies it to produce actual editor content
	 * @param changeset
	 */
	public void addChangeset(Changeset changeset) {
		changeset.setPad(getPad());
		changeset.setAuthor(getUser());
		try {
			PadContent padContent = pad.appendChangeset(changeset);
			StringBuilder sb = new StringBuilder();
			for (Session s : pad.getOtherActivePadSessions(this)) {
				sb.append(s.getUser().getName()+", ");
			}
			log.info("PUBLISHING CHANGESET TO: "+sb.toString().substring(0, sb.length()-2)+ " [padContent: "+padContent.hashCode()+"]");
			for (Session s : pad.getOtherActivePadSessions(this)) {
				s.newChangesetAdded(changeset, padContent);
			}
//			messageFactory.publishMessage(changesetMessage(changeset, spanId, spanPos, leftId, rightId)); // korekcia pozicie +1
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
	}

//	public void addRemoteChangeset(Changeset changeset) {
//		pad.appendRemoteChangeset(changeset);
//		padAssembler.applyRemoteChangeset(changeset);
//	}

	public void newChangesetAdded(Changeset c, PadContent padContent) {
		log.info(getUser().getName()+": NEW CHANGESET ADDED BY: "+ c.getUser().getName());
		if (padContent != null) {
			TextSlice touchedTs = padContent.getTouchedTs();
			int spanId = 0, leftId = 0, rightId = 0, spanPos = 0;
			if (touchedTs != null) {
				spanId = touchedTs.getSpanId();
				TextSlice left = padContent.getPrevTextSlice(touchedTs);
				TextSlice right = padContent.getNextTextSlice(touchedTs);
				leftId = left != null && left.isTextSpan() ? left.getSpanId() : 0;
				rightId = right != null && right.isTextSpan() ? right.getSpanId() : 0;
				spanPos = touchedTs.getLastActivePos();
				String info = generateRemoteChangeString(c, spanId, spanPos, leftId, rightId);
				try {
					if (remoteChangesetsQueue.isEmpty()) {
						processToView(info);
					} else {
						publishToQueue(info);
					}
				} catch (MessageException e) {
					ExceptionHandler.handle(e,e.getMessage()+"\n (processing full reload for consistency to be kept)",true);
					reloadEditorContent();
				}
			}
		} else {
			reloadEditorContent();
		}
		log.info("\\"+getUser().getName()+": NEW CHANGESET ADDED BY: "+ c.getUser().getName()+ " [padContent: "+padContent.hashCode()+"]");
	}

	private void publishToQueue(String receivedString) {
//		TODO: AGGREGATE SUBSEQUENT CHANGES INTO ONE MESSAGE
//		if (!remoteChangesetsQueue.isEmpty()) {
//			String lastReceived = remoteChangesetsQueue.getLast();
//			Matcher mLast = Session.CHANGESET_MSG_PATTERN.matcher(lastReceived);
//			Matcher mActual = Session.CHANGESET_MSG_PATTERN.matcher(receivedString);
//			if (mLast.find() && mActual.find() && mLast.group(group)
//		}
		log.info("Adding change to queue: "+receivedString);
		remoteChangesetsQueue.addLast(receivedString);
	}

	private String generateRemoteChangeString(Changeset c, int spanId, int spanPos, int leftId, int rightId) {
		StringBuilder sb = new StringBuilder();
		// IDENTIFICATION OF SENDER
		sb.append("@[p"+pad.getId()+"u"+getUser().getId()+"]");
		sb.append(SessionBean.CHANGESET).append(":");
		sb.append(c.getAction()).append(":");
		sb.append(spanId).append(":");
		sb.append(spanPos).append(":");
		sb.append(c.getCharbank() == null ? "" : c.getCharbank()).append(":");
		sb.append(leftId).append(":");
		sb.append(rightId).append(":");
		sb.append(c.getOffset()).append(":");
		sb.append(c.getSession().getColorCode());
		return sb.toString();
	}

	private void processToView(String info) throws MessageException {
		log.info("processToView: "+info);
		pushBean.sendMessage(info);
	}
	
	public void processNext() {
		try {
			if (!remoteChangesetsQueue.isEmpty()) {
				log.info("Popping from the queue: "+remoteChangesetsQueue.peek());
				processToView(remoteChangesetsQueue.pop());
			} else {
				log.info("Queue is empty: "+remoteChangesetsQueue.peek());
			}
		} catch (MessageException e) {
			ExceptionHandler.handle(e);
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
//		String prevColorCode = getColorCode();
		setColorCode(colorCode);
		update();
//		messageFactory.publishMessage(colorChangeMessage(prevColorCode, colorCode));
	}

	public String getColorClass() {
		if (colorCode != null) {
			return "cl"+colorCode.substring(1);
		}
		return null;
	}

	public void reloadEditorContent() {
		pad.reloadPadContent();
		String content = pad.getContent();
		try {
			processToView(refreshEditorContent(content));
		} catch (MessageException e) {
			ExceptionHandler.handle(e);
		}
	}

	private String refreshEditorContent(String content) {
		StringBuilder sb = new StringBuilder();
		sb.append("REFRESH:").append(getUser().getId()).append(":").append(getPad().getId()).append(":").append(content);
		return sb.toString();
	}
}
