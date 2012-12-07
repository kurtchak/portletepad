package org.webepad.control;

import java.io.Serializable;

import org.richfaces.application.push.MessageException;
import org.richfaces.application.push.TopicKey;
import org.richfaces.application.push.TopicsContext;

public class PushControl implements Serializable {
	private static final long serialVersionUID = 1L;
	private TopicKey topicKey;

	public void initializeTopic() {
		topicKey = new TopicKey("push"); // same topic for all clients => programmatical filtering
		TopicsContext topicsContext = TopicsContext.lookup();
	    topicsContext.getOrCreateTopic(topicKey);
	}

	public void sendMessage(String message) throws MessageException {
	    TopicsContext topicsContext = TopicsContext.lookup();
	    topicsContext.publish(topicKey, message);
	}
	
	public String getTopicAddress() {
//		System.out.println("getTopicAddress:"+topicKey);
		if (topicKey != null) {
			return topicKey.getTopicAddress();
		}
		return null;
	}
	
	public Boolean isInitialized() {
		return topicKey != null;
	}
}