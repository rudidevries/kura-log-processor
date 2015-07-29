package nl.rudidevries.kura.log.processor;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.input.Tailer;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Processor implements ConfigurableComponent {
	private static final Logger s_logger = LoggerFactory.getLogger(Processor.class);
    private static final String APP_ID = "Processor";
    
    private static final String LOG_PATH_PROP_NAME = "logfile.path";
    private static final String LOG_REGEX_PROP_NAME = "logfile.regex";
    private static final String LOG_TOPIC_GROUPS_PROP_NAME = "logfile.topic.groups";
    private static final String LOG_MESSAGE_GROUP_PROP_NAME = "logfile.message.group";
    
    private Map<String, Object> properties;
	private CloudService cloudService;
	private CloudClient cloudClient;
    private Tailer tailer;

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        try {
			cloudClient = cloudService.newCloudClient(APP_ID);
		}
		catch (Exception e) {
			s_logger.info("Failed retrieving new CloudClient for {}...", APP_ID);
			throw new ComponentException(e);
		}
        
        updated(properties);
        s_logger.info("Bundle " + APP_ID + " has started with config!");
    }

    protected void deactivate(ComponentContext componentContext) {
    	if (tailer != null) {
    		tailer.stop();
    		tailer = null;
    	}

		cloudClient.release();
        s_logger.info("Bundle " + APP_ID + " has stopped!");
    }

    public void updated(Map<String, Object> properties) {
        this.properties = properties;
        
        if(properties != null && !properties.isEmpty()) {
            Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                s_logger.info("New property - " + entry.getKey() + " = " +
                entry.getValue() + " of type " + entry.getValue().getClass().toString());
            }
        }
        createTailer();
    }
    
    public void setCloudService(CloudService cloudService) {
	    this.cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
	    cloudService = null;
	}
    
    /**
     * Create a new Tailer for the logfile.
     */
    private void createTailer() {
    	// Clean up existing tailer, if any.
    	if (tailer != null) {
    		tailer.stop();
    		tailer = null;
    	}
    	
    	try {
	    	// Create listener & Tailer.
	    	Listener listener = new Listener(
	    			(String) properties.get(LOG_REGEX_PROP_NAME), 
	    			getTopicGroups(),
	    			getMessageGroup(),
	    			cloudClient
				);
	    	
	    	tailer = Tailer.create(
	    			new File((String) properties.get(LOG_PATH_PROP_NAME)), 
	    			listener, 
	    			1000, 
	    			true
				);
    	}
    	catch (Exception e) {
    		s_logger.error("An error has occured in bundle {}. {}", APP_ID, e);
    	}
    }
    
    private int[] getTopicGroups() {
    	String[] strTopicGroups = ((String) properties.get(LOG_TOPIC_GROUPS_PROP_NAME)).split("/");
    	int[] topicGroups = new int[strTopicGroups.length];
    	for (int i = 0; i < strTopicGroups.length; i++) {
    		topicGroups[i] = Integer.parseInt(strTopicGroups[i]);
    	}
    	
    	return topicGroups;
    }
    
    private int getMessageGroup() {
    	return Integer.parseInt((String) properties.get(LOG_MESSAGE_GROUP_PROP_NAME));
    }
}
