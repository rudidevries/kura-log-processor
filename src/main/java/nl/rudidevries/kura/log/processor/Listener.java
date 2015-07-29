package nl.rudidevries.kura.log.processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.eclipse.kura.cloud.CloudClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Listener extends TailerListenerAdapter {

	private static final Logger s_logger = LoggerFactory.getLogger(Listener.class);
	
	private Pattern pattern;
	private CloudClient cloudClient;
	private int[] topicGroups;
	private int messageGroup;
	
	public Listener(String regex, int[] topicGroups, int messageGroup, CloudClient cloudClient) {
		pattern = Pattern.compile(regex);
		this.topicGroups = topicGroups;
		this.messageGroup = messageGroup;
		this.cloudClient = cloudClient;
	}
	
	public void handle(String line) {
		Matcher m = pattern.matcher(line);
		
		if (m.find()) {
			String payload = m.group(messageGroup);
			
			StringBuilder topic = new StringBuilder();
			for (int group : topicGroups) {
				if (topic.length() != 0) {
					topic.append("/");
				}
				topic.append(m.group(group));
			}
			
			try {
			    cloudClient.publish(topic.toString(), payload.getBytes(), 1, false, 10);
			}
			catch (Exception e) {
			    s_logger.error("Cannot publish topic: {}", topic.toString(), e);
			}
		}
	}
}
