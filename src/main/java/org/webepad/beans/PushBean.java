package org.webepad.beans;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.richfaces.application.push.MessageException;
import org.richfaces.application.push.TopicKey;
import org.richfaces.application.push.TopicsContext;
import org.webepad.model.Session;

@ManagedBean(name="pushBean")
@SessionScoped
public class PushBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private TopicKey topicKey;

	public void initializeTopic(Session session) {
		topicKey = new TopicKey("push"); // same topic for all clients => programmatical filtering
		TopicsContext topicsContext = TopicsContext.lookup();
	    topicsContext.getOrCreateTopic(topicKey);
	}

	public void sendMessage(String message) throws MessageException {
	    TopicsContext topicsContext = TopicsContext.lookup();
	    topicsContext.publish(topicKey, message);
	}
	
	public String getTopicAddress() {
		if (topicKey != null) {
			return topicKey.getTopicAddress();
		}
		return null;
	}
}