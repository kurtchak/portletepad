package org.webepad.jms;

import java.util.Properties;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.control.PadMessageFactory;
import org.webepad.utils.ExceptionHandler;

public class Communicator implements MessageListener {
	private static Logger log = LoggerFactory.getLogger(Communicator.class);
	private static String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
	private static String URL_PKG_PREFIXES = "org.jboss.ejb.client.naming";
	private static String TOPIC_PORT = String.valueOf(4447 + Integer.valueOf(System.getProperty("jboss.socket.binding.port-offset")));
	private static String TOPIC_HOST = System.getProperty("jboss.bind.address");
	private static String TOPIC_URL = "remote://" + TOPIC_HOST + ":" + TOPIC_PORT;
	private static ExternalContext CONTEXT = FacesContext.getCurrentInstance().getExternalContext();
	private static String TOPIC_NAME = CONTEXT.getInitParameter("jboss.jms.padtopic.name");
	private static String TOPIC_USER = CONTEXT.getInitParameter("jboss.jms.padtopic.user");
	private static String TOPIC_PASS = CONTEXT.getInitParameter("jboss.jms.padtopic.password");
	
	private TopicConnection conn;
	private TopicSession session;
	private Topic topic;
	private PadMessageFactory messageFactory;

	public Communicator(PadMessageFactory messageFactory) {
		super();
		try {
			this.messageFactory = messageFactory;
			this.initializeCommunication();
		} catch (Exception e) {
			ExceptionHandler.handle(e, "Error creating listener/sender: " + e, false);
		}
	}

	public void onMessage(Message msg) {
		try {
			TextMessage tm = (TextMessage) msg;
			messageFactory.processMessage(tm.getText());
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
	}

	public void sendMessage(String msg) throws JMSException, NamingException {
		TopicPublisher send = session.createPublisher(topic);
		TextMessage tm = session.createTextMessage(msg);
		send.publish(tm);
		send.close();
	}

	private void initializeCommunication() throws JMSException, NamingException {
		Properties props = new Properties();
		props.setProperty(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
		props.setProperty(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);
		props.setProperty(Context.PROVIDER_URL, TOPIC_URL);
        props.setProperty(Context.SECURITY_PRINCIPAL, TOPIC_USER);
        props.setProperty(Context.SECURITY_CREDENTIALS, TOPIC_PASS);
        log.info(props.toString());

		Context context = new InitialContext(props);
		TopicConnectionFactory tcf = (TopicConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
		conn = tcf.createTopicConnection(TOPIC_USER, TOPIC_PASS);
		topic = (Topic) context.lookup(TOPIC_NAME);
		session = conn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
		conn.start();

		TopicSubscriber recv = session.createSubscriber(topic);
		recv.setMessageListener(this);
	}

	public void disconnect() throws JMSException {
		if (conn != null) {
			conn.stop();
		}

		if (session != null) {
			session.close();
		}

		if (conn != null) {
			conn.close();
		}
	}
}
