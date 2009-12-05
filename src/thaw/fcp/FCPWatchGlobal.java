package thaw.fcp;

public class FCPWatchGlobal implements FCPQuery {
	private boolean watch;
	private final FCPQueueManager queueManager;


	public FCPWatchGlobal(final boolean v, FCPQueueManager queueManager) {
		watch = v;
		this.queueManager = queueManager;
	}

	public boolean start() {
		final FCPMessage message = new FCPMessage();

		message.setMessageName("WatchGlobal");

		if(watch)
			message.setValue("Enabled", "true");
		else
			message.setValue("Enabled", "false");

		message.setValue("VerbosityMask", "1");

		queueManager.getQueryManager().writeMessage(message);

		return true;
	}

	public boolean stop() {
		return true;
	}

	public int getQueryType() {
		return 0;
	}

}
