package thaw.fcp;

public class FCPModifyConfig implements FCPQuery {
	private String name;
	private String value;
	private final FCPQueueManager queueManager;
	
	public FCPModifyConfig(String name, String newValue, FCPQueueManager queueManager) {
		this.name = name;
		this.value = newValue;
		this.queueManager = queueManager;
	}

	public int getQueryType() {
		return 0;
	}

	public boolean start() {
		FCPMessage msg = new FCPMessage();
		msg.setMessageName("ModifyConfig");
		msg.setValue(name, value);
		
		queueManager.getQueryManager().writeMessage(msg);
		
		return true;
	}

	public boolean stop() {
		return false;
	}

}
