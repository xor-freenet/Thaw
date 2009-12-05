package thaw.fcp;


public class FCPRemovePeer implements FCPQuery {
	private String name;
	private final FCPQueueManager queueManager;

	/**
	 * Ref can be a real ref, or URL=http://where.to-get-the-ref-on-the.net/
	 */
	public FCPRemovePeer(String name, FCPQueueManager queueManager) {
		this.name = name;
		this.queueManager = queueManager;
	}


	public boolean start() {
		FCPMessage msg = new FCPMessage();

		msg.setMessageName("RemovePeer");

		msg.setValue("NodeIdentifier", name);

		return queueManager.getQueryManager().writeMessage(msg);
	}


	public boolean stop() {
		/* can't stop */
		return false;
	}


	public int getQueryType() {
		return 0;
	}
}
