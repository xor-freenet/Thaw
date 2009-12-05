package thaw.fcp;

public class FCPModifyConfig implements FCPQuery {
	private String name;
	private String value;
	private final FCPQueryManager queryManager;
	
	public FCPModifyConfig(String name, String newValue, FCPQueryManager queryManager) {
		this.name = name;
		this.value = newValue;
		this.queryManager = queryManager;
	}

	public int getQueryType() {
		return 0;
	}

	public boolean start() {
		FCPMessage msg = new FCPMessage();
		msg.setMessageName("ModifyConfig");
		msg.setValue(name, value);
		
		queryManager.writeMessage(msg);
		
		return true;
	}

	public boolean stop() {
		return false;
	}

}
