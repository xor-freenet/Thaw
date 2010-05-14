package thaw.fcp;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import thaw.core.Logger;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;


/**
 * Allow to insert a simple file.
 */
public class FCPClientPut extends FCPTransferQuery implements Observer {

	private final FCPQueueManager queueManager;
	private File localFile;
	private long fileSize;
	private int keyType;
	private int rev;
	private String name;
	private boolean global;
	private int persistence;
	private boolean compressFile;
	private final boolean getCHKOnly;
	private int compressionCodec = -1;

	private String privateKey; /* must finish by '/' (cf SSKKeypair) */
	private String publicKey; /* publicKey contains the filename etc */
	private int priority = DEFAULT_PRIORITY;

	private int toTheNodeProgress = 0;
	private String status;

	private int attempt = 0;

	private boolean fatal = true;
	private boolean sending = false;

	private FCPGenerateSSK sskGenerator;
	private boolean lockOwner = false;

	private final HashMap<String,String> metadatas = new LinkedHashMap<String,String>();

	private final static int PACKET_SIZE = 1024;

	private SHA256Computer sha;

	private int putFailedCode = -1;


	public static class Builder {
		/* Required parameters */

		private final FCPQueueManager queueManager;

		/* Optional parameters */
		private int keyType = KEY_TYPE_CHK;
		private int rev = 0;
		private String name;
		private String privateKey;
		private int priority = DEFAULT_PRIORITY;
		private boolean global = true;
		private int persistence = PERSISTENCE_FOREVER;
		private boolean getCHKOnly;
		private String identifier;
		private boolean compress = true;
		private String publicKey;
		private String fileName;
		private String status;
		private long fileSize = 0;
		private File localFile;
		private TransferStatus transferStatus = TransferStatus.NOT_RUNNING;

		public Builder(FCPQueueManager queueManager) {
			this.queueManager = queueManager;
		}

		public FCPClientPut build() {
			return new FCPClientPut(this);
		}

		public Builder Parameters(HashMap<String,String> parameters) {
			setLocalFile(new File(parameters.get("localFile")));
			fileSize = localFile.length();

			keyType = Integer.parseInt(parameters.get("keyType"));
			rev = Integer.parseInt(parameters.get("Revision"));
			name = parameters.get("name");

			privateKey = parameters.get("privateKey");
			if((privateKey == null) || privateKey.equals("")) {
				privateKey = null;
			}

			publicKey = parameters.get("publicKey");
			if((privateKey == null) || (publicKey == null) || publicKey.equals("")) {
				publicKey = null;
			}

			priority = Integer.parseInt(parameters.get("priority"));
			global = Boolean.valueOf(parameters.get("global"));
			identifier = parameters.get("identifier");

			boolean running = Boolean.valueOf(parameters.get("running"));
			boolean successful = Boolean.valueOf(parameters.get("successful"));
			boolean finished = Boolean.valueOf(parameters.get("finished"));
			this.transferStatus = TransferStatus.getTransferStatus(running,finished,successful);
			persistence = Integer.parseInt(parameters.get("persistence"));

			if ((persistence == PERSISTENCE_UNTIL_DISCONNECT) && !transferStatus.isFinished()) {
				this.transferStatus = TransferStatus.NOT_RUNNING;
				status = "Waiting";
			} else {
				status = parameters.get("status");
			}

			return this;
		}

		public Builder setKeyType(int keyType) {
			this.keyType = keyType;
			return this;
		}

		public Builder setRev(int rev) {
			this.rev = rev;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setPrivateKey(String privateKey) {
			this.privateKey = privateKey;
			return this;
		}

		public Builder setPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public Builder setGlobal(boolean global) {
			this.global = global;
			return this;
		}

		public Builder setPersistence(int persistence) {
			this.persistence = persistence;
			return this;
		}

		public Builder setGetCHKOnly(boolean getCHKOnly) {
			this.getCHKOnly = getCHKOnly;
			return this;
		}

		public Builder setIdentifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder setCompress(boolean compress) {
			this.compress = compress;
			return this;
		}

		public Builder setPublicKey(String publicKey) {
			this.publicKey = publicKey;
			return this;
		}

		public Builder setFileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public Builder setStatus(String status) {
			this.status = status;
			return this;
		}

		public Builder setFileSize(long fileSize) {
			this.fileSize = fileSize;
			return this;
		}
		
		public Builder setTransferStatus(TransferStatus transferStatus) {
			this.transferStatus = transferStatus;
			return this;
		}

		public Builder setLocalFile(File localFile) {
			this.localFile = localFile;
			return this;
		}
	}

	private FCPClientPut(Builder builder) {
		super(builder.identifier, true);

		this.queueManager = builder.queueManager;
		this.compressFile = builder.compress;
		this.priority = builder.priority;
		this.global = builder.global;
		this.persistence = builder.persistence;
		this.getCHKOnly = builder.getCHKOnly;
		this.rev = builder.rev;

		fatal = true;
		attempt = 0;

		if(builder.publicKey == null || builder.publicKey.equals("")) {
			/* New insert */
			localFile = builder.localFile;

			this.keyType = builder.keyType;
			if(keyType == 0) {
				if(localFile != null) {
					this.name = localFile.getName();
					this.privateKey = null;
				} else {
					throw(new IllegalStateException("localFile is not allowed to be null for CHK keys."));
				}
			} else {
				this.name = builder.name;
				this.privateKey = builder.privateKey;
			}

			if(localFile != null) {
				fileSize = localFile.length();
			} else {
				fileSize = 0;
			}

			status = "Waiting";
			setStatus(TransferStatus.NOT_RUNNING);
		} else {
			/* Resuming */
			localFile = null;
			name = builder.fileName;

			if(builder.fileSize > 0) {
				this.fileSize = builder.fileSize;
			} else {
				this.fileSize = 0;
			}

			toTheNodeProgress = 100;

			if(builder.publicKey.startsWith("CHK")) {
				keyType = KEY_TYPE_CHK;
			} else if(builder.publicKey.startsWith("KSK")) {
				keyType = KEY_TYPE_KSK;
			} else if(builder.publicKey.startsWith("SSK")) {
				keyType = KEY_TYPE_SSK;
			} else if(builder.publicKey.startsWith("USK")) {
				keyType = KEY_TYPE_SSK;
			} else {
				throw(new IllegalStateException("Unknown publicKey state."));
			}

			this.status = builder.status;
			setStatus(TransferStatus.RUNNING);
		}

		/* TODO: Why isn't publicKey stored? */
		publicKey = null;
		setBlockNumbers(-1, -1, -1, true);

		this.persistence = persistence;

		setBlockNumbers(-1, -1, -1, true);
		setStatus(true, false, false);

		this.status = status;
		fatal = true;
	}

	
	public boolean start() {
		putFailedCode = -1;
		setIdentifier(null);

		if((localFile != null) && (localFile.length() <= 0)) {
			Logger.warning(this, "Empty or unreachable file:"+localFile.getPath());

			status = "EMPTY OR UNREACHABLE FILE";
			setStatus(TransferStatus.FINISHED);

			fatal = true;

			notifyChange();

			return false;
		}

		queueManager.getQueryManager().addObserver(this);

		setBlockNumbers(-1, -1, -1, false);
		setStatus(TransferStatus.RUNNING);

		sha = null;

		if (queueManager.getQueryManager().getConnection().isLocalSocket() && localFile != null) {
			status = "Computing hash to get approval from the node ...";

			setIdentifier(queueManager.getAnID() + "-"+ localFile.getName());

			String salt = queueManager.getQueryManager().getConnection().getClientHello().getConnectionId()
				+"-"+ getIdentifier()
				+"-";
			Logger.info(this, "Salt used for this transfer: ~" + salt+ "~");

			sha = new SHA256Computer(salt, localFile.getPath());
			sha.addObserver(this);

			Thread th = new Thread(new ThawThread(sha, "Hash computer", this));
			th.start();
		} else {
			return startProcess();
		}

		return true;
	}


	public boolean startProcess() {
		if((keyType == KEY_TYPE_SSK) && (privateKey == null)) {
			generateSSK();
		}

		if( ((keyType == KEY_TYPE_SSK) && (privateKey != null)) || (keyType != KEY_TYPE_SSK)) {
			startInsert();
		}

		notifyChange();

		return true;
	}

	/**
	 * doesn't set running = true. startInsert() will.
	 */
	public void generateSSK() {
		status = "Generating keys";

		sskGenerator = new FCPGenerateSSK(queueManager);

		sskGenerator.addObserver(this);
		sskGenerator.start();
	}



	private class UnlockWaiter implements ThawRunnable {
		FCPClientPut clientPut;
		FCPConnection c;

		public UnlockWaiter(final FCPClientPut clientPut, final FCPConnection c) {
			this.clientPut = clientPut;
			this.c = c;
		}

		private Thread th;
		private boolean waiting = false;

		public void run() {
			synchronized(this) {
				waiting = true;
			}

			c.addToWriterQueue();

			synchronized(this) {
				waiting = false;

				if (Thread.interrupted()) {
					c.removeFromWriterQueue();
					return;
				}
			}

			lockOwner = true;

			clientPut.continueInsert();
			return;
		}

		public void setThread(Thread th) {
			synchronized(this) {
				this.th = th;
			}
		}

		/* race-conditions may happen but "shits happen" */
		public void stop() {
			synchronized(this) {
				if (waiting)
					th.interrupt();
			}
		}
	}



	public boolean startInsert() {
		final FCPConnection connection = queueManager.getQueryManager().getConnection();

		toTheNodeProgress= 0;

		status = "Waiting for socket availability";

		Logger.info(this, "Waiting for socket availability ...");

		UnlockWaiter uw = new UnlockWaiter(this, connection);

		final Thread fork = new Thread(new ThawThread(uw,
				"Unlock waiter",
				this));
		uw.setThread(fork);
		fork.start();

		return true;
	}

	public boolean continueInsert() {
		setStatus(TransferStatus.RUNNING);

		sending = true;

		final FCPConnection connection = queueManager.getQueryManager().getConnection();

		status = "Sending to the node";

		if(getIdentifier() == null) { /* see start() ; when computing hash */
			if (localFile != null)
				setIdentifier(queueManager.getAnID() + "-"+ localFile.getName());
			else
				setIdentifier(queueManager.getAnID());
		}

		notifyChange();

		final FCPMessage msg = new FCPMessage();

		msg.setMessageName("ClientPut");
		msg.setValue("URI", getInsertionKey());

		if(!metadatas.isEmpty()) {
			Collection<String> metadatasKeySet = metadatas.keySet();
			for(String key : metadatasKeySet) {
				final String value = metadatas.get(key);
				msg.setValue("Metadata."+key, value);
			}
		}

		msg.setValue("Identifier", getIdentifier());
		msg.setValue("MaxRetries", "-1");
		msg.setValue("PriorityClass", Integer.toString(priority));

		if(getCHKOnly) {
			msg.setValue("GetCHKOnly", "true");
		} else {
			msg.setValue("GetCHKOnly", "false");
		}
		msg.setValue("Verbosity", "512");

		if(global) {
			msg.setValue("Global", "true");
		} else {
			msg.setValue("Global", "false");
		}
		if (localFile != null) {
			msg.setValue("ClientToken", localFile.getPath());
		}

		switch(persistence) {
		case(PERSISTENCE_FOREVER): msg.setValue("Persistence", "forever"); break;
		case(PERSISTENCE_UNTIL_NODE_REBOOT): msg.setValue("Persistence", "reboot"); break;
		case(PERSISTENCE_UNTIL_DISCONNECT): msg.setValue("Persistence", "connection"); break;
		default: Logger.error(this, "Unknow persistence !?"); break;
		}

		if (localFile != null) {
			msg.setValue("TargetFilename", localFile.getName());
		} else {
			msg.setValue("TargetFilename", name);
		}

		if (!connection.isLocalSocket()) {
			msg.setValue("UploadFrom", "direct");
			msg.setAmountOfDataWaiting(fileSize);
		} else {
			msg.setValue("UploadFrom", "disk");
			msg.setValue("Filename", localFile.getPath());

			if (sha != null) {
				msg.setValue("FileHash", sha.getHash());
			}
		}

		if(compressFile && compressionCodec != -1)
			msg.setValue("Codecs", Integer.toString(compressionCodec));
		else
			msg.setValue("DontCompress", "true");

		Logger.info(this, "Sending "+Long.toString(fileSize)+" bytes on socket ...");

		queueManager.getQueryManager().writeMessage(msg, false);

		boolean ret = true;

		if (!connection.isLocalSocket()) {
			Logger.info(this, "Sending file to the node");
			ret = sendFile();
			Logger.info(this, "File sent (or not :p)");
		}

		connection.removeFromWriterQueue();
		lockOwner = false;
		sending = false;

		setBlockNumbers(-1, -1, -1, true);

		if(ret == true) {
			setStatus(TransferStatus.RUNNING);
			fatal = true;

			if (!getCHKOnly)
				status = "Inserting";
			else
				status = "Computing";

			notifyChange();

			return true;
		} else {
			setStatus(TransferStatus.FINISHED);
			Logger.warning(this, "Unable to send the file to the node");
			status = "Unable to send the file to the node";

			notifyChange();

			return false;
		}

	}

	private boolean sendFile() {
		final FCPConnection connection = queueManager.getQueryManager().getConnection();

		long remaining = fileSize;
		byte[] data = null;

		FileInputStream in = null;

		if (localFile == null) {
			toTheNodeProgress = 100;
			return true;
		}

		try {
			in = new FileInputStream(localFile);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "FileNotFoundException ?! ohoh, problems ...");
			return false;
		}

		long startTime = System.currentTimeMillis();
		final long origSize = remaining;

		while(remaining > 0) {
			int to_read = FCPClientPut.PACKET_SIZE;

			if(remaining < to_read)
				to_read = (int)remaining;

			data = new byte[to_read];

			try {
				if(in.read(data) < 0) {
					Logger.error(this, "Error while reading file ?!");
					return false;
				}
			} catch(final java.io.IOException e) {
				Logger.error(this, "IOException while reading file! proobleeem");
				return false;
			}

			if(!connection.rawWrite(data)) {
				Logger.error(this, "Error while writing file on socket ! Disconnected ?");
				return false;
			}

			remaining = remaining - to_read;

			if( System.currentTimeMillis() >= (startTime+3000) ) {
				toTheNodeProgress = (int) (((origSize - remaining) * 100) / origSize);
				notifyChange();
				startTime = System.currentTimeMillis();
			}

			//Logger.verbose(this, "Remaining: "+(new Long(remaining)).toString());
		}


		toTheNodeProgress = 100;

		try {
			if(in.available() > 0) {
				Logger.error(this, "File not send completly ?!");
				return false;
			}
		} catch(final java.io.IOException e) {
			/* we will suppose its ok ... */
			Logger.notice(this, "available() IOException (hu ?)");
		}

		return true;
	}

	public boolean stop() {
		boolean wasFinished = isFinished();

		if(removeRequest()) {
			queueManager.getQueryManager().deleteObserver(this);

			status = "Stopped";

			if (wasFinished || !isSuccessful())
				setStatus(TransferStatus.FINISHED);
			else
				setStatus(TransferStatus.SUCCESSFUL);

			fatal= true;

			if (!wasFinished) {
				notifyChange();
			}

			return true;
		}

		return false;
	}

	public void update(final Observable o, final Object param) {
		if (o == sha) {
			if(sha.isFinished())
				startProcess();
			else {
				status = "Computing hash";
				setBlockNumbers(100, 100, sha.getProgression(), true);
			}

			notifyChange();

			return;
		}

		if(o == sskGenerator) {
			privateKey = sskGenerator.getPrivateKey();
			publicKey = sskGenerator.getPublicKey() + "/" + name;

			notifyChange();

			startInsert();
			return;
		}

		if (param != null && param instanceof FCPMessage) {
			final FCPMessage msg = (FCPMessage)param;

			if((msg.getValue("Identifier") == null)
			   || !msg.getValue("Identifier").equals(getIdentifier()))
				return;

			if("URIGenerated".equals( msg.getMessageName() )
			   || "PutFetchable".equals( msg.getMessageName() )) {
				setStatus(TransferStatus.RUNNING);

				publicKey = msg.getValue("URI");

				publicKey = publicKey.replaceAll("freenet:", "");

				Logger.info(this, msg.getMessageName()+": "+publicKey);

				if(getCHKOnly) {
					status = "CHK";
					setStatus(TransferStatus.SUCCESSFUL);

					toTheNodeProgress = 100;
					fatal = false;
					sending = false;

					notifyChange();
					queueManager.getQueryManager().deleteObserver(this);
					return;
				}

				status = "Inserting";

				notifyChange();
				return;
			}

			if("PutSuccessful".equals(msg.getMessageName())) {
				setStatus(TransferStatus.SUCCESSFUL);
				queueManager.getQueryManager().deleteObserver(this);

				setStartupTime(Long.valueOf(msg.getValue("StartupTime")).longValue());
				setCompletionTime(Long.valueOf(msg.getValue("CompletionTime")).longValue());
				publicKey = msg.getValue("URI");

				if (publicKey == null) {
					status = "[Warning]";
					Logger.warning(this, "PutSuccessful message without URI field ?!");
					notifyChange();
					return;
				}

				publicKey = publicKey.replaceAll("freenet:", "");

				if(keyType == KEY_TYPE_KSK) {
					if (rev >= 0)
						publicKey = "KSK@"+name+"-" + Integer.toString(rev);
					else
						publicKey = "KSK@"+name;
				}
				//if(keyType == KEY_TYPE_SSK)
				//	publicKey = publicKey + "/" + name + "-" + Integer.toString(rev);


				status = "Finished";

				notifyChange();
				return;
			}

			if ("PersistentRequestModified".equals(msg.getMessageName())) {
				if (msg.getValue("PriorityClass") == null) {
					Logger.warning(this, "No priority specified ?! Message ignored.");
				} else {
					priority = Integer.parseInt(msg.getValue("PriorityClass"));
				}
				return;
			}

			if ("PersistentRequestRemoved".equals(msg.getMessageName())) {
				if (!isFinished()) {
					setStatus(TransferStatus.FINISHED);
					fatal = true;
					status = "Removed";
				}

				Logger.info(this, "PersistentRequestRemoved >> Removing from the queue");
				queueManager.getQueryManager().deleteObserver(this);
				queueManager.remove(this);

				notifyChange();
				return;
			}


			if ("PutFailed".equals( msg.getMessageName() )) {
				setStatus(TransferStatus.FINISHED);
				fatal = true;

				putFailedCode = Integer.parseInt(msg.getValue("Code"));

				status = "Failed ("+msg.getValue("CodeDescription")+")";

				if((msg.getValue("Fatal") != null) &&
				   msg.getValue("Fatal").equals("false")) {
					status = status + " (non-fatal)";
					fatal = false;
				}

				if (putFailedCode != 9)
					Logger.warning(this, "Insertion failed");
				else
					Logger.warning(this, "Insertion error : collision");
				Logger.notice(this, msg.toString());

				notifyChange();
				return;
			}

			if("ProtocolError".equals( msg.getMessageName() )) {
				setStatus(TransferStatus.FINISHED);
				fatal = true;

				if(lockOwner) {
					lockOwner = false;
					queueManager.getQueryManager().getConnection().removeFromWriterQueue();
				}

				Logger.warning(this, "Protocol error ! : "+msg.getValue("CodeDescription"));
				status = "Protocol error ("+msg.getValue("CodeDescription")+")";

				if((msg.getValue("Fatal") != null) &&
				   msg.getValue("Fatal").equals("false")) {
					status = status + " (non-fatal)";
					fatal = false;
				}

				notifyChange();

				return;
			}

			if("IdentifierCollision".equals(msg.getMessageName())) {
				status = "Identifier collision";
				start(); /* et hop ca repart :) */
				return;
			}


			if("PersistentPut".equals(msg.getMessageName())) {
				status = "Inserting";
				//publicKey = msg.getValue("URI");
				return;
			}

			if("StartedCompression".equals(msg.getMessageName())) {
				status = "Compressing";

				notifyChange();

				return;
			}

			if("FinishedCompression".equals(msg.getMessageName())) {
				status = "Inserting";

				if((msg.getValue("OrigSize") == null)
				   || (msg.getValue("CompressedSize") == null)) {
					notifyChange();
					return;
				}

				final int rate = (int)( ((new Long(msg.getValue("OrigSize"))).longValue() * 100) / (new Long(msg.getValue("CompressedSize"))).longValue() );

				Logger.notice(this, "Compression rate: "+ Integer.toString(rate)+" %");

				notifyChange();

				return;
			}

			if("SimpleProgress".equals(msg.getMessageName())) {

				if (msg.getValue("Total") != null
					&& msg.getValue("Required") != null
					&& msg.getValue("Succeeded") != null) {

					final long total = (new Long(msg.getValue("Total"))).longValue();
					final long required = (new Long(msg.getValue("Required"))).longValue();
					final long succeeded = (new Long(msg.getValue("Succeeded"))).longValue();

					//boolean progressReliable = false;

					//if((msg.getValue("FinalizedTotal") != null) &&
					//		   msg.getValue("FinalizedTotal").equals("true")) {
					//	progressReliable = true; //has to be true if we want to see progress at all.
					//}

					setBlockNumbers(required, total, succeeded, true);
					setStatus(TransferStatus.RUNNING);

					if (!getCHKOnly)
						status = "Inserting";
					else
						status = "Computing";


				} else {
					setBlockNumbers(-1, -1, -1, false);
				}

				notifyChange();

				return;
			}


			if (msg.getMessageName() == null)
				Logger.notice(this, "Unknow message (name == null)");
			else
				Logger.notice(this, "Unkwown message: "+msg.getMessageName());

			return;
		}

	}


	public int getQueryType() {
		return 2;
	}

	public boolean pause(final FCPQueueManager queueManager) {
		/*
		 * TODO : lower priority ?
		 */
		setStatus(TransferStatus.NOT_RUNNING);
		status = "Delayed";

		fatal = true;

		removeRequest();
		notifyChange();

		return false;
	}


	public boolean removeRequest() {
		if(sending) {
			Logger.notice(this, "Can't interrupt while sending to the node ...");
			status = status + " (can't interrupt while sending to the node)";
			return false;
		}

		if(isRunning() || isFinished()) {
			final FCPMessage msg = new FCPMessage();
			msg.setMessageName("RemovePersistentRequest");
			msg.setValue("Identifier", getIdentifier());

			if(global)
				msg.setValue("Global", "true");
			else
				msg.setValue("Global", "false");

			queueManager.getQueryManager().writeMessage(msg);

			setStatus(TransferStatus.NOT_RUNNING);

			queueManager.getQueryManager().deleteObserver(this);
		} else {
			Logger.notice(this, "Nothing to remove");
		}


		return true;
	}

	public void updatePersistentRequest(final boolean clientToken) {
		//if(!isPersistent())
		//	return;

		final FCPMessage msg = new FCPMessage();

		msg.setMessageName("ModifyPersistentRequest");
		msg.setValue("Global", Boolean.toString(global));
		msg.setValue("Identifier", getIdentifier());
		msg.setValue("PriorityClass", Integer.toString(priority));

		if(clientToken && (getPath() != null))
			msg.setValue("ClientToken", getPath());

		queueManager.getQueryManager().writeMessage(msg);
	}

	public int getThawPriority() {
		return priority;
	}

	public int getFCPPriority() {
		return priority;
	}

	public void setFCPPriority(final int prio) {
		Logger.info(this, "Setting priority to "+Integer.toString(prio));

		priority = prio;

		notifyChange();
	}



	public String getStatus() {
		return status;
	}

	/**
	 * @return public key
	 */
	public String getFileKey() {
		return publicKey;
	}

	public int getKeyType() {
		return keyType;
	}

	public String getInsertionKey() {
		String key = null;

		if ((keyType == KEY_TYPE_CHK) && (publicKey != null))
			key = publicKey;

		else if ((keyType == KEY_TYPE_CHK) && (publicKey == null))
			key = "CHK@";

		else if (keyType == KEY_TYPE_KSK) {
			if (rev >= 0)
				key = "KSK@" + name + "-"+ Integer.toString(rev);
			else
				key = "KSK@" + name;
		}
		else if (keyType == KEY_TYPE_SSK && privateKey.startsWith("SSK")) {
			if (rev >= 0)
				key = privateKey + name+"-"+rev;
			else
				key = privateKey + name;
		}
		else if (keyType == KEY_TYPE_SSK && privateKey.startsWith("USK")) {
			if (rev >= 0)
				key = privateKey + name + "/" + rev;
			else
				key = privateKey + name;
		}

		if (key == null) {
			Logger.warning(this, "Unknown key type ?! May result in a strange behavior !");
			return privateKey;
		}

		return key;
	}

	public long getFileSize() {
		return fileSize; /* a "file length" ? why not "file size" as usual ? */
	}

	public String getPath() {
		if(localFile != null)
			return localFile.getPath();
		else
			return null;
	}

	public String getFilename() {
		if(localFile != null)
			return localFile.getName();
		else
			return name;
	}

	public int getAttempt() {
		return attempt;
	}

	public void setAttempt(final int x) {
		attempt = x;
	}

	public int getMaxAttempt() {
		return -1;
	}

	public boolean isFatallyFailed() {
		return ((!isSuccessful()) && fatal);
	}

	public HashMap<String,String> getParameters() {
		final HashMap<String,String> result = new HashMap<String,String>();

		result.put("localFile", localFile.getPath());
		result.put("keyType", Integer.toString(keyType));
		result.put("Revision", Integer.toString(rev));
		result.put("Name", name);
		if(privateKey != null)
			result.put("privateKey", privateKey);
		if(publicKey != null)
			result.put("publicKey", publicKey);
		result.put("priority", Integer.toString(priority));
		result.put("global", Boolean.toString(global));
		result.put("persistence", Integer.toString(persistence));

		result.put("status", status);

		result.put("attempt", Integer.toString(attempt));
		if(getIdentifier() != null)
			result.put("identifier", getIdentifier());
		result.put("running", Boolean.toString(isRunning()));
		result.put("successful", Boolean.toString(isSuccessful()));
		result.put("finished", Boolean.toString(isFinished()));
		result.put("compressFile", Boolean.toString(compressFile));
		result.put("compressionCodec", Integer.toString(compressionCodec));

		return result;
	}

	public boolean setParameters(final HashMap parameters) {

		localFile = new File((String)parameters.get("localFile"));

		fileSize = localFile.length();

		keyType = Integer.parseInt((String)parameters.get("keyType"));
		rev = Integer.parseInt((String)parameters.get("Revision"));
		name = (String)parameters.get("name");

		privateKey = (String)parameters.get("privateKey");
		if((privateKey == null) || privateKey.equals("")) {
			privateKey = null;
		}

		publicKey = (String)parameters.get("publicKey");
		if((privateKey == null) || (publicKey == null) || publicKey.equals("")) {
			publicKey = null;
		}

		priority = Integer.parseInt((String)parameters.get("priority"));

		global = Boolean.valueOf((String)parameters.get("global")).booleanValue();

		persistence = Integer.parseInt((String)parameters.get("persistence"));
		status = (String)parameters.get("status");
		attempt = Integer.parseInt((String)parameters.get("attempt"));

		compressFile = Boolean.parseBoolean("compressFile");
		compressionCodec = Integer.parseInt("compressionCodec");

		setIdentifier((String)parameters.get("identifier"));

		boolean running = Boolean.valueOf((String)parameters.get("running")).booleanValue();
		boolean successful = Boolean.valueOf((String)parameters.get("successful")).booleanValue();
		boolean finished = Boolean.valueOf((String)parameters.get("finished")).booleanValue();

		setStatus(running, finished, successful);

		if ((persistence == PERSISTENCE_UNTIL_DISCONNECT) && !isFinished()) {
			setStatus(false, false, false);
			status = "Waiting";
		}

		return true;
	}

	public boolean isPersistent() {
		return persistence < 2;
	}

	public boolean isGlobal() {
		return global;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public int getRevision() {
		return rev;
	}

	/**
	 * Do nothing.
	 */
	public boolean saveFileTo(final String dir) {
		return false;
	}

	public int getTransferWithTheNodeProgression() {
		return toTheNodeProgress;
	}

	public HashMap<String,String> getMetadatas() {
		return new HashMap<String,String>(metadatas);
	}

	public void setMetadata(final String name, final String val) {
		if(val == null) {
			metadatas.remove(name);
		} else {
			metadatas.put(name, val);
		}
	}

	public String getMetadata(final String name) {
		return metadatas.get(name);
	}


	/**
	 * @return -1 if none
	 */
	public int getPutFailedCode() {
		return putFailedCode;
	}

}
