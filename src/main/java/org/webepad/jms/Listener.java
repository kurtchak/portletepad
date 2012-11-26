package org.webepad.jms;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.webepad.control.PadMessageFactory;
import org.webepad.utils.ExceptionHandler;

public class Listener implements MessageListener {

	private String initialNameContext;
	private String urlPkgPrefixes;
	private String url;
	private String name;
	private TopicConnection conn = null;
	private TopicSession session = null;
	private Topic topic = null;
	private PadMessageFactory messageFactory;
	private String user;
	private String pass;

	public Listener(String initialNameContext, String urlPkgPrefixes, String url, String name, String user, String pass, PadMessageFactory messageFactory) {
		super();
		this.initialNameContext = initialNameContext;
		this.urlPkgPrefixes = urlPkgPrefixes;
		this.url = url;
		this.name = name;
		this.user = user;
		this.pass = pass;
		this.messageFactory = messageFactory;

		try {
			this.initializeListener();
		} catch (Exception e) {
			ExceptionHandler.handle(e, "Error creating listener: " + e, false);
		}

	}

	public void onMessage(Message msg) {
		TextMessage tm = (TextMessage) msg;
		try {
			messageFactory.processMessage(tm.getText());
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
	}

	private void initializeListener() throws JMSException, NamingException {

		Properties props = new Properties();
		props.setProperty(Context.INITIAL_CONTEXT_FACTORY, this.initialNameContext);
		props.setProperty(Context.URL_PKG_PREFIXES, this.urlPkgPrefixes);
		props.setProperty(Context.PROVIDER_URL, this.url);
        props.setProperty(Context.SECURITY_PRINCIPAL, this.user);
        props.setProperty(Context.SECURITY_CREDENTIALS, this.pass);
        System.out.println(props.toString());
 

		Context context = new InitialContext(props);
		TopicConnectionFactory tcf = (TopicConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
		conn = tcf.createTopicConnection(this.user, this.pass);
		topic = (Topic) context.lookup(this.name);
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
