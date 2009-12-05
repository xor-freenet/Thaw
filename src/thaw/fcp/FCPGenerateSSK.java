package thaw.fcp;

import java.util.Observable;
import java.util.Observer;

import thaw.core.Logger;

public class FCPGenerateSSK extends Observable implements FCPQuery, Observer {
	private String identifier = null;
	private String privateKey = null;
	private String publicKey = null;

	private final FCPQueueManager queueManager;
	private final FCPQueryManager queryManager;


	public FCPGenerateSSK(FCPQueueManager queueManager) {

		this.queueManager = queueManager;
		this.queryManager = queueManager.getQueryManager();
	}


	public boolean start() {
		queryManager.addObserver(this);

		identifier = queueManager.getAnID();

		final FCPMessage msg = new FCPMessage();
		msg.setMessageName("GenerateSSK");
		msg.setValue("Identifier", identifier);

		queryManager.writeMessage(msg);

		return true;
	}


	public void update (final Observable o, final Object param) {
		final FCPMessage msg = (FCPMessage)param;

		if((msg.getValue("Identifier") == null)
		   || !msg.getValue("Identifier").equals(identifier))
			return;

		if("SSKKeypair".equals( msg.getMessageName() )) {
			Logger.debug(this, "SSKKeypair !");

			privateKey = msg.getValue("InsertURI");
			publicKey = msg.getValue("RequestURI");

			privateKey = privateKey.replaceFirst("freenet:", "");
			publicKey = publicKey.replaceFirst("freenet:", "");

			stop();
			
			setChanged();
			this.notifyObservers();

			return;
		}


	}


	public boolean stop() {
		queryManager.deleteObserver(this);

		return true;
	}

	public int getQueryType() {
		return 0;
	}


	/**
	 * @return privateKey without the "freenet:" prefix.
	 */
	public String getPrivateKey() {
		return privateKey;
	}

	/**
	 * @return publicKey without the "freenet:" prefix.
	 */
	public String getPublicKey() {
		return publicKey;
	}

}
