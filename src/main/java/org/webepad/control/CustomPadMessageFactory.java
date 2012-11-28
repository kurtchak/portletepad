package org.webepad.control;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.beans.SessionBean;
import org.webepad.dao.ChangesetDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.exceptions.MalformedMessage;
import org.webepad.exceptions.NoSuchPadException;
import org.webepad.exceptions.NoSuchUserException;
import org.webepad.jms.Communicator;
import org.webepad.model.Changeset;
import org.webepad.model.Session;
import org.webepad.utils.ExceptionHandler;

public class CustomPadMessageFactory implements PadMessageFactory {
	private static Logger log = LoggerFactory.getLogger(CustomPadMessageFactory.class);
	private static ChangesetDAO changesetDAO = HibernateDAOFactory.getInstance().getChangesetDAO();

	private static final Pattern JOIN_LEAVE_MSG_PATTERN = Pattern.compile("^((J|L):([\\d]+):([\\d]+))$");
	private static final Pattern RESPONSE_MSG_PATTERN = Pattern.compile("^R:([\\d]+):([\\d]+):([\\d]+):([\\d]+)$");
	public static final Pattern CHANGESET_MSG_PATTERN = Pattern.compile("^C:([WD]):([\\d]+);_(\\d+):(\\d+)_\\[(\\d*),(\\d*)\\]$");
	private static final Pattern USERCOLOR_MSG_PATTERN = Pattern.compile("^UC:([\\d]+):([\\d]+):(#[a-zA-Z0-9]{6})$");

	private Communicator communicator;
	public Session session;
	private SessionBean sessionBean;

	public void publishMessage(String msg) {
		try {
			communicator.sendMessage(msg);
		} catch (JMSException e) {
			ExceptionHandler.handle(e);
		} catch (NamingException e) {
			ExceptionHandler.handle(e);
		}
	}

	public void publishChangeset(Changeset c) {
		// TODO Auto-generated method stub
		
	}

	public void processMessage(String msg) {
		Matcher m = JOIN_LEAVE_MSG_PATTERN.matcher(msg);
		Matcher mr = RESPONSE_MSG_PATTERN.matcher(msg);
		Matcher mc = CHANGESET_MSG_PATTERN.matcher(msg);
		Matcher mu = USERCOLOR_MSG_PATTERN.matcher(msg);
		try {
			if (m.find()) {
				String action = m.group(2);
				Long padId = Long.decode(m.group(3));
				Long userId = Long.decode(m.group(4));
				// receiving only events from the same pad and not from myself
				if (padId.equals(session.getPad().getId()) && !userId.equals(session.getUser().getId())) {
					log.info("\nPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId());
					sessionBean.processRemotePresence(action, padId, userId);
				}
			} else if (mr.find()) {
				Long fromPadId = Long.decode(mr.group(1));
				Long fromUserId = Long.decode(mr.group(2));
				Long toPadId = Long.decode(mr.group(3));
				Long toUserId = Long.decode(mr.group(4));
				// receiving only response event addressed to myself
				if (toPadId.equals(session.getPad().getId()) && toUserId.equals(session.getUser().getId())) {
					log.info("\nPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId());
					sessionBean.processRemotePresence(SessionBean.JOIN_RESPONSE, fromPadId, fromUserId);
				}
			} else if (mc.find()) {
				Long cId = Long.decode(mc.group(2));
				Integer spanId = Integer.decode(mc.group(3));
				Integer spanPos = Integer.decode(mc.group(4));
				Integer leftId = Integer.decode(mc.group(5));
				Integer rightId = Integer.decode(mc.group(6));
				Changeset c = changesetDAO.getChangeset(cId);
				if (c.getPad().getId().equals(session.getPad().getId()) && !c.getUser().getId().equals(session.getUser().getId())) {
					log.info("\nPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId()+",spanId:"+spanId+",spanPos:"+spanPos+",leftId:"+leftId+",rightId:"+rightId);
					sessionBean.processRemoteChangeset(c, spanId, spanPos, leftId, rightId);
				}
			} else if (mu.find()) {
				Long userId = Long.decode(mu.group(1));
				Long padId = Long.decode(mu.group(2));
				String prevColor = mu.group(3);
				Session session = sessionBean.findSession(padId, userId);
				if (padId.equals(session.getPad().getId()) && !userId.equals(session.getUser().getId())) {
					log.info("\nPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId());
					sessionBean.processRemoteUserChange(SessionBean.USERCOLOR, session, prevColor);
				}
			} else {
				throw new MalformedMessage(msg);
			}
		} catch (NoSuchUserException e) {
			ExceptionHandler.handle(e);
		} catch (NoSuchPadException e) {
			ExceptionHandler.handle(e);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
	}
	
	/**
	 * Leaves the session publishing the leave message and terminate sessions
	 * sender and listener
	 */
	public void closeConnection() {
		try {
			communicator.disconnect();
		} catch (Exception e) {
			ExceptionHandler.handle(e, "Error terminating listener JMS objects: " + e, true);
		}
	}
	
	/**
	 * Initializes this sessions listener and sender for communication with other sessions
	 * and publishes the join message to the topic
	 * @param sessionBean
	 */
	public void initConection(SessionBean sessionBean) {
//		try {
			this.sessionBean = sessionBean;
			this.session = sessionBean.getSession();
			log.info("PadMessageFactory<<" + this.toString() + ">> sessionBean="+ sessionBean.hashCode() + ":session="+session+":"+session.getId());
			this.communicator = new Communicator(this); // reference on 'this' makes two-way association for communication between factory and listener
//			this.sender = new Sender(INITIAL_CONTEXT_FACTORY, URL_PKG_PREFIXES, TOPIC_URL, TOPIC_NAME, TOPIC_USER, TOPIC_PASS);
//		} catch (JMSException e) {
//			ExceptionHandler.handle(e);
//		} catch (NamingException e) {
//			ExceptionHandler.handle(e);
//		}
	}
}
