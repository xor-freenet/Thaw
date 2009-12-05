package thaw.fcp;

public class FCPListPersistentRequests implements FCPQuery {
	private final FCPQueryManager queryManager;

	public FCPListPersistentRequests(FCPQueryManager queryManager) {
		this.queryManager = queryManager;
	}


	public boolean start() {
		final FCPMessage newMessage = new FCPMessage();

		newMessage.setMessageName("ListPersistentRequests");

		queryManager.writeMessage(newMessage);

		return true;
	}

	public boolean stop() {
		return true;
	}

	public int getQueryType() {
		return 0;
	}

}
