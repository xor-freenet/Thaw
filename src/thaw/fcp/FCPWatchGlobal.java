package thaw.fcp;

public class FCPWatchGlobal implements FCPQuery {
	private boolean watch;
	private final FCPQueryManager queryManager;


	public FCPWatchGlobal(final boolean v, FCPQueryManager queryManager) {
		watch = v;
		this.queryManager = queryManager;
	}

	public boolean start() {
		final FCPMessage message = new FCPMessage();

		message.setMessageName("WatchGlobal");

		if(watch)
			message.setValue("Enabled", "true");
		else
			message.setValue("Enabled", "false");

		message.setValue("VerbosityMask", "1");

		queryManager.writeMessage(message);

		return true;
	}

	public boolean stop() {
		return true;
	}

	public int getQueryType() {
		return 0;
	}

}
