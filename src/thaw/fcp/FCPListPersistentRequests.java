package thaw.fcp;

public class FCPListPersistentRequests implements FCPQuery {
	protected final FCPQueueManager queueManager;

	public FCPListPersistentRequests(FCPQueueManager queueManager) {
		this.queueManager = queueManager;
	}


	public boolean start() {
		final FCPMessage newMessage = new FCPMessage();

		newMessage.setMessageName("ListPersistentRequests");

		queueManager.getQueryManager().writeMessage(newMessage);

		return true;
	}

	public boolean stop() {
		return true;
	}

	public int getQueryType() {
		return 0;
	}

}
