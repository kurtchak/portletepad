package org.webepad.jms;

import java.util.Properties;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Sender {

	private String initialNameContext;
	private String urlPkgPrefixes;
	private String url;
	private String name;
	private String user;
	private String pass;
	private TopicConnection conn = null;
	private TopicSession session = null;
//	@Resource(lookup="java:jboss/jms/topic/testTopic")
	private Topic topic = null;

	public Sender(String initialNameContext, String urlPkgPrefixes, String url, String name, String user, String pass) throws JMSException, NamingException {
		this.initialNameContext = initialNameContext;
		this.urlPkgPrefixes = urlPkgPrefixes;
		this.url = url;
		this.name = name;
		this.user = user;
		this.pass = pass;

		this.initializeSender();
	}

	private void initializeSender() throws JMSException, NamingException {

		Properties props = new Properties();
		props.setProperty(Context.INITIAL_CONTEXT_FACTORY, this.initialNameContext);
		props.setProperty(Context.URL_PKG_PREFIXES, this.urlPkgPrefixes);
		props.setProperty(Context.PROVIDER_URL, this.url);
        props.setProperty(Context.SECURITY_PRINCIPAL, this.user);
        props.setProperty(Context.SECURITY_CREDENTIALS, this.pass);

		Context context = new InitialContext(props);

		TopicConnectionFactory tcf = (TopicConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
		conn = tcf.createTopicConnection(this.user, this.pass);
		topic = (Topic) context.lookup(name);

		session = conn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
		conn.start();
	}

	public void send(String text) throws JMSException, NamingException {
		// Send a text msg
		TopicPublisher send = session.createPublisher(topic);
		TextMessage tm = session.createTextMessage(text);
		send.publish(tm);
		send.close();
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

	public String getTopicName() {
		return name;
	}

	public String getTopicURL() {
		return url;
	}
}
