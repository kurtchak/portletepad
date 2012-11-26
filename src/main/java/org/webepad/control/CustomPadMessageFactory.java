package org.webepad.control;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.beans.SessionBean;
import org.webepad.exceptions.MalformedMessage;
import org.webepad.exceptions.NoSuchPadException;
import org.webepad.exceptions.NoSuchUserException;
import org.webepad.jms.Listener;
import org.webepad.jms.Sender;
import org.webepad.model.Changeset;
import org.webepad.model.Session;
import org.webepad.utils.ExceptionHandler;

public class CustomPadMessageFactory implements PadMessageFactory {

	private Logger log = LoggerFactory.getLogger(CustomPadMessageFactory.class);

	private static final Pattern JOIN_LEAVE_MSG_PATTERN = Pattern.compile("^((J|L):([\\d]+):([\\d]+))$");
	private static final Pattern RESPONSE_MSG_PATTERN = Pattern.compile("^R:([\\d]+):([\\d]+):([\\d]+):([\\d]+)$");
	public static final Pattern CHANGESET_MSG_PATTERN = Pattern.compile("^C:([WD]):([\\d]+):([\\d]+):([\\d]+):([^\\$]+)\\$(.)?(.*)_(\\d+):(\\d+)_\\[(\\d*),(\\d*)\\](\\d+)(#[0-9a-fA-F]{6})(.*)$");
	private static final Pattern USERCOLOR_MSG_PATTERN = Pattern.compile("^UC:([\\d]+):([\\d]+):(#[0-9a-fA-F]{6})-(#[0-9a-fA-F]{6})$");

	private Listener listener;
	private Sender sender;

//	private static String INITIAL_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";
	private static String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
//	private static String URL_PKG_PREFIXES = "org.jboss.naming";
	private static String URL_PKG_PREFIXES = "org.jboss.ejb.client.naming";
//	private static String TOPIC_NAME = "java:/topic/padTopic";
	private static String TOPIC_NAME = "jms/topic/padTopic";
//	private static String TOPIC_URL = "127.0.0.1:9999";
	private static String TOPIC_URL = "remote://localhost:4447";
	private static String JBOSS_USER = "kurtcha";
	private static String JBOSS_PASS = "portletepad";
	
	public Session session;
	private SessionBean sessionBean;
	
	public void publishMessage(String msg) {
		try {
			sender.send(msg);
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
					log.info("editorspanPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId());
					sessionBean.remoteUserChange(action, padId, userId, null, null, null, null, null, null, null, null, null, null, null);
				}
			} else if (mr.find()) {
				String action = "R";
				Long fromPadId = Long.decode(mr.group(1));
				Long fromUserId = Long.decode(mr.group(2));
				Long toPadId = Long.decode(mr.group(3));
				Long toUserId = Long.decode(mr.group(4));
				// receiving only response event addressed to myself
				if (toPadId.equals(session.getPad().getId()) && toUserId.equals(session.getUser().getId())) {
					log.info("\nPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId());
					sessionBean.remoteUserChange(action, fromPadId, fromUserId, null, null, null, null, null, null, null, null, null, null, null);
				}
			} else if (mc.find()) {
				String action = "C";
				String oper = mc.group(1);
				Long userId = Long.decode(mc.group(2));
				Long padId = Long.decode(mc.group(3));
				Integer number = Integer.decode(mc.group(4));
				String charbank = mc.group(6);
				Integer spanId = Integer.decode(mc.group(8));
				Integer spanPos = Integer.decode(mc.group(9));
				Integer leftId = Integer.decode(mc.group(10));
				Integer rightId = Integer.decode(mc.group(11));
				Integer offset = Integer.decode(mc.group(12));
				String fgColor = mc.group(13);
				String hashCode = mc.group(14);
				if (padId.equals(session.getPad().getId()) && !userId.equals(session.getUser().getId())) {
					log.info("\nPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId()+",spanId:"+spanId+",spanPos:"+spanPos+",leftId:"+leftId+",rightId:"+rightId+",fgColor:"+fgColor);
					sessionBean.remoteUserChange(action, padId, userId, number, spanId, spanPos, charbank, leftId, rightId, offset, fgColor, null, oper, hashCode);
				}
			} else if (mu.find()) {
				String action = "UC";
				Long userId = Long.decode(mu.group(1));
				Long padId = Long.decode(mu.group(2));
				String prevFgColor = mu.group(3);
				String fgColor = mu.group(4);
				if (padId.equals(session.getPad().getId()) && !userId.equals(session.getUser().getId())) {
					log.info("\nPROCESS MESSAGE:"+msg+"\n\t-> RECEIVED BY:pad:"+session.getPad().getId()+",user:"+session.getUser().getId()+",fgColor:"+fgColor);
					sessionBean.remoteUserChange(action, padId, userId, null, null, null, null, null, null, null, fgColor, prevFgColor, null, null);
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
			listener.disconnect();
			sender.disconnect();
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
		try {
			this.sessionBean = sessionBean;
			this.session = sessionBean.getSession();
			this.listener = new Listener(INITIAL_CONTEXT_FACTORY, URL_PKG_PREFIXES, TOPIC_URL, TOPIC_NAME, JBOSS_USER, JBOSS_PASS, this); // reference on 'this' makes two-way asociation for communication between factory and listener
			this.sender = new Sender(INITIAL_CONTEXT_FACTORY, URL_PKG_PREFIXES, TOPIC_URL, TOPIC_NAME, JBOSS_USER, JBOSS_PASS);
		} catch (JMSException e) {
			ExceptionHandler.handle(e);
		} catch (NamingException e) {
			ExceptionHandler.handle(e);
		}
	}
}
