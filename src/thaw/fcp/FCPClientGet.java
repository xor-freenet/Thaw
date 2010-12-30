package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import thaw.core.Logger;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;


public class FCPClientGet extends FCPTransferQuery implements Observer {

	private int maxRetries = -1;
	private final static int PACKET_SIZE = 65536;

	private final FCPQueueManager queueManager;
	private FCPQueryManager duplicatedQueryManager;           /* TODO: Necessary? */

	private final FCPQueryManager queryManager;
	private final int persistence;
	private final boolean globalQueue;
	private final long maxSize;

	private String key;
	private String filename; /* Extract from the key */
	private int priority;

	private String destinationDir;
	private String finalPath;

	private int attempt;
	private String status;

	private int fromTheNodeProgress;   /* -1 if the transfer hasn't started, else [0-100] */

	private long fileSize;
	private boolean gotExpectedFileSize = false;

	private boolean writingSuccessful = true;
	private boolean fatal = true;
	private boolean isLockOwner = false;

	private boolean alreadySaved = false;

	private boolean noDDA;
	private boolean noRedir = false;

	private FCPTestDDA testDDA = null;

	/* used when redirected */
	private boolean restartIfFailed = false;

	private int protocolErrorCode = -1;
	private int getFailedCode = -1;

	public static class Builder {
		/* Required parameters */
		private final FCPQueueManager queueManager;

		/* Optional parameters */
		private String key;
		private int priority = DEFAULT_PRIORITY;
		private int persistence = PERSISTENCE_FOREVER;
		private boolean globalQueue = true;
		private int maxRetries = -1;
		private String destinationDir;
		private long maxSize = 0;
		private boolean noDDA = false;
		private String status = "Waiting";
		private String identifier;
		private int attempt = -1;
		private long fileSize;
		private String filename;
		private TransferStatus transferStatus = TransferStatus.NOT_RUNNING;
		private boolean isNewRequest = true;

		public Builder(FCPQueueManager queueManager) {
			this.queueManager = queueManager;
		}

		public FCPClientGet build() {
			return new FCPClientGet(this);
		}

		public Builder setParameters(HashMap<String,String> parameters) {
			key = parameters.get("URI");
 			Logger.debug(this, "Resuming key : "+key);

			filename       = parameters.get("Filename");
			priority       = Integer.parseInt(parameters.get("Priority"));
			persistence    = Integer.parseInt(parameters.get("Persistence"));
			globalQueue    = Boolean.valueOf(parameters.get("Global"));
			maxRetries     = Integer.parseInt(parameters.get("MaxRetries"));
			destinationDir = parameters.get("ClientToken");
			attempt        = Integer.parseInt(parameters.get("Attempt"));
			status         = parameters.get("Status");
			identifier     = parameters.get("Identifier");
			Logger.info(this, "Resuming id : "+identifier);

			fileSize       = Long.parseLong(parameters.get("FileSize"));

			boolean running        = Boolean.valueOf(parameters.get("Running"));
			boolean successful     = Boolean.valueOf(parameters.get("Successful"));
			transferStatus = TransferStatus.getTransferStatus(running, !running, successful);

			if((persistence == PERSISTENCE_UNTIL_DISCONNECT) && !transferStatus.isFinished()) {
				transferStatus = TransferStatus.NOT_RUNNING;
				status = "Waiting";
			}

			return this;
		}

		public Builder setKey(String key) {
			this.key = key;
			return this;
		}

		public Builder setPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public Builder setPersistence(int persistence) {
			this.persistence = persistence;
			return this;
		}

		public Builder setGlobalQueue(boolean globalQueue) {
			this.globalQueue = globalQueue;
			return this;
		}

		public Builder setMaxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
			return this;
		}

		public Builder setDestinationDir(String destinationDir) {
			this.destinationDir = destinationDir;
			return this;
		}

		public Builder setMaxSize(long maxSize) {
			this.maxSize = maxSize;
			return this;
		}

		public Builder setNoDDA(boolean noDDA) {
			this.noDDA = noDDA;
			return this;
		}

		public Builder setStatus(String status) {
			this.status = status;
			return this;
		}

		public Builder setIsNewRequest(boolean isNew) {
			this.isNewRequest = isNew;
			return this;
		}

		public Builder setIdentifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder setTransferStatus(TransferStatus transferStatus) {
			this.transferStatus = transferStatus;
			return this;
		}
	}

	private FCPClientGet(final Builder builder) {
        super(builder.identifier, false);
		this.queueManager = builder.queueManager;
		this.queryManager = queueManager.getQueryManager();
		fromTheNodeProgress = -1;

		this.maxRetries = builder.maxRetries;
		this.key = builder.key;
		this.persistence = builder.persistence;
		this.destinationDir = builder.destinationDir;
		this.setPriority(builder.priority);
		this.filename = builder.filename;
		this.attempt = builder.attempt;
		this.fileSize = builder.fileSize;
		this.noDDA = builder.noDDA;
		this.maxSize = builder.maxSize;
		setStatus(builder.transferStatus);

		if(builder.status == null) {
			this.status = "(null)";
		} else {
			this.status = builder.status;
		}

		if (builder.globalQueue && (persistence >= PERSISTENCE_UNTIL_DISCONNECT)) {
			globalQueue = false; /* else protocol error */
		} else {
			globalQueue = builder.globalQueue;
		}

		if(filename == null) {
			filename = FreenetURIHelper.getFilenameFromKey(key);
		}

		if (filename == null) {
			Logger.warning(this, "Nameless key !!");
		}

		/* FIX : This is a fix for the files inserted by Frost */
		/* To remove when bback will do his work correctly */
		if (filename == null && builder.identifier != null) {
			Logger.notice(this, "Fixing Frost key filename");
			Logger.notice(this, builder.identifier);
			String[] split = builder.identifier.split("-");

			if (split.length >= 2) {
				filename = "";
				for (int i = 1 ; i < split.length; i++)
					filename += split[i];
			}
		}
		/* /FIX */

		if(builder.isNewRequest) {
			Logger.debug(this, "Query for getting "+key+" created");
		} else {
			Logger.debug(this, "Resuming key : "+key);
			Logger.info(this, "Resuming id : "+getIdentifier());
		}

		queryManager.addObserver(this);
	}



	/**
	 * won't follow the redirections
	 */
	public void setNoRedirectionFlag(boolean noRedir) {
		this.noRedir = noRedir;
	}

	public boolean start(){
		attempt++;

		setStatus(TransferStatus.RUNNING);

		/* TODO : seems to be true sometimes => find why */ 
		if (queueManager == null /* TODO: Needed anymore?*/
				|| queryManager == null
				|| queryManager.getConnection() == null)
			return false;

		return sendClientGet();
	}

	public boolean sendClientGet() {

		if (finalPath == null && destinationDir == null) {
			if ((destinationDir = System.getProperty("java.io.tmpdir")) == null) {
				Logger.error(this, "Unable to find temporary directory ! Will create troubles !");
				destinationDir = "";
			}
			else
				Logger.notice(this, "Using temporary file: "+getPath());
		}

		status = "Requesting";

		if(getIdentifier() == null)
			setIdentifier(queueManager.getAnID() + "-"+filename);

		Logger.debug(this, "Requesting key : "+getFileKey());

		final FCPMessage queryMessage = new FCPMessage();

		queryMessage.setMessageName("ClientGet");
		queryMessage.setValue("URI", key);
		queryMessage.setValue("Identifier", getIdentifier());
		queryMessage.setValue("Verbosity", "1");
		queryMessage.setValue("MaxRetries", Integer.toString(maxRetries));
		queryMessage.setValue("PriorityClass", Integer.toString(getPriority()));

		if (maxSize > 0)
			queryMessage.setValue("MaxSize", Long.toString(maxSize));

		if(destinationDir != null)
			queryMessage.setValue("ClientToken", destinationDir);

		queryMessage.setValue("Persistence", getPersistenceString());

		if(globalQueue)
			queryMessage.setValue("Global", "true");
		else
			queryMessage.setValue("Global", "false");

		if (!queryManager.getConnection().isLocalSocket() || noDDA)
			queryMessage.setValue("ReturnType", "direct");
		else {
			queryMessage.setValue("ReturnType", "disk");
			queryMessage.setValue("Filename", getPath());

			if (getPath() == null) {
				Logger.error(this, "getPath() returned null ! Will create troubles !");
			}
		}

		queryManager.addObserver(this);
		return queryManager.writeMessage(queryMessage);
	}


	public void update(final Observable o, final Object arg) {
		if (o == testDDA) {
			if (!testDDA.mayTheNodeWrite())
				noDDA = true;

			sendClientGet();

			return;
		}

		final FCPMessage message = (FCPMessage)arg;
		if(!identifierMatches(message))
			return;

		FCPQueryManager queryManager;
		if (o instanceof FCPQueryManager)
			queryManager = (FCPQueryManager)o;
		else
			queryManager = this.queryManager; /* default one */

		switch (message.getMessageType()) {
			case DataFound:
				dataFound(message);
				break;

			case IdentifierCollision:
				identifierCollision();
				break;

			case PersistentGet:
				/* not our problem */
				break;

			case ProtocolError:
				protocolError(message);
				break;

			/* we assume that the change is not about the clientToken */
			case PersistentRequestModified:
				if (message.getValue("PriorityClass") == null) {
					Logger.warning(this, "No priority specified ?! Message ignored.");
				} else {
					setPriority(Integer.parseInt(message.getValue("PriorityClass")));
				}
				break;

			case PersistentRequestRemoved:
				persistentRequestRemoved();
				break;

			case GetFailed:
				getFailed(message);
				break;

			case SimpleProgress:
				simpleProgress(message);
				break;

			case AllData:
				allData(queryManager, message);
				break;

			case SendingToNetwork: /* Fall-thru */
			case ExpectedHashes: /* Fall-thru */
			case CompatibilityMode: /* Fall-thru */
			case ExpectedMIME: /* Fall-thru */
				break;

			case ExpectedDataLength:
				expectedDataLength(message);
				break;

			case UNKNOWN_MESSAGE: /* Fall-thru */
			default:
				Logger.warning(this, "Unknown message : " + message.getMessageName() + " !");
				break;
		}
	}


	protected void dataFound(FCPMessage message) {
		Logger.debug(this, "DataFound!");

		/* Mark the network block count as being reliable if it hasn't been marked already */
		makeReliable();

		if(!isFinished()) {
			if(!alreadySaved) {
				alreadySaved = true;

				fileSize = Long.parseLong(message.getValue("DataLength"));

				if(isPersistent() || ddaAllowed()) {
					if(destinationDir != null) {
						if (!fileExists()
								&& !(ddaAllowed())
								&& queryManager.getConnection().getAutoDownload()) {
							status = "Requesting file from the node";

							writingSuccessful = false;
							saveFileTo(destinationDir, false);

						} else {
							status = "Available";
							setStatus(TransferStatus.SUCCESSFUL);
							writingSuccessful = true;
							Logger.notice(this, "Download finished => File already existing. Not rewritten");
						}

					} else {
						setStatus(TransferStatus.SUCCESSFUL);
						status = "Available but not downloaded";
						writingSuccessful = true;
						Logger.notice(this, "Download finished => Don't know where to put file, so file not asked to the node");
					}
				} else {
					/* we do nothing : the request is not persistent, so we should get a AllData */
				}
			}

			notifyChange();
		}
	}


	protected void identifierCollision() {
		Logger.notice(this, "IdentifierCollision ! Resending with another id");

		setIdentifier(null);
		start();

		notifyChange();
	}


	protected void protocolError(FCPMessage message) {
		Logger.debug(this, "ProtocolError !");

		if ("25".equals(message.getValue("Code"))
				&& ddaAllowed()
				&& (destinationDir != null || finalPath != null)) {

			if (destinationDir == null)
				destinationDir = new File(finalPath).getAbsoluteFile().getParent();

			testDDA = new FCPTestDDA(destinationDir, false, true, queryManager);
			testDDA.addObserver(this);
			testDDA.start();
		}
		else if("15".equals( message.getValue("Code") )) {
			Logger.debug(this, "Unknown URI ? was probably a stop order so no problem ...");
		}
		else{
			if ("4".equals(message.getValue("Code"))) {
				Logger.warning(this, "The node reported an invalid key. Please check the following key\n"+
						key);
			}

			Logger.error(this, "=== PROTOCOL ERROR === \n"+message.toString());

			protocolErrorCode = Integer.parseInt(message.getValue("Code"));

			status = "Protocol Error ("+message.getValue("CodeDescription")+")";

			setStatus(TransferStatus.FAILED);

			fatal = true;

			if((message.getValue("Fatal") != null) &&
					message.getValue("Fatal").equals("false")) {
				fatal = false;
				status = status + " (non-fatal)";
			}

			if(isLockOwner) {
				if (duplicatedQueryManager != null)
					duplicatedQueryManager.getConnection().removeFromWriterQueue();
				isLockOwner= false;
			}

			notifyChange();

			queryManager.deleteObserver(this);
		}
	}


	protected void persistentRequestRemoved() {
		status = "Removed";

		if (!isFinished()) {
			Logger.warning(this, "Transfer canceled by another client");
			setStatus(TransferStatus.FAILED);
			fatal = true;
		}

		Logger.info(this, "PersistentRequestRemoved >> Removing from the queue");
		queryManager.deleteObserver(this);
		queueManager.remove(this);

		notifyChange();
	}


	protected void getFailed(FCPMessage message) {
		Logger.info(this, "GetFailed for "+key);

		if (message.getValue("RedirectURI") != null ) {
			Logger.info(this, "Redirected from " + key + " to " + message.getValue("RedirectURI"));

			if ( key.equals(message.getValue("RedirectURI")) ) {
				Logger.warning(this, "Redirected to the same key we tried to fetch !?");
			} else if ( !noRedir ) {
				key = message.getValue("RedirectURI");
				status = "Redirected ...";
				if (queueManager.isOur(message.getValue("Identifier"))) {
					restartIfFailed = true;
					stop(queryManager, false);
				} else {
					Logger.info(this, "Not our transfer ; we don't touch");
				}
			}
			else
				Logger.notice(this, "Redirected, but noredir flag is set. Ignoring redirection");
		}

		if (restartIfFailed) {
			restartIfFailed = false;
			start();
			return;
		}
		else if (!"13".equals(message.getValue("Code"))){ /* if != of Data Not Found */
			Logger.notice(this, "GetFailed : "+message.getValue("CodeDescription"));
		}

		if (isRunning()) {
			//removeRequest();

			getFailedCode = Integer.parseInt(message.getValue("Code"));

			status = "Failed ("+message.getValue("CodeDescription")+")";
			Logger.warning(this, "Transfer failed: "+message.getValue("CodeDescription"));
			setStatus(TransferStatus.FAILED);

			fatal = true;

			if((message.getValue("Fatal") != null) &&
					message.getValue("Fatal").equals("false")) {
				fatal = false;
				status = status + " (non-fatal)";
			}

			notifyChange();
		} else { /* Must be a "GetFailed: cancelled by caller", so we simply ignore */
			Logger.info(this, "Cancellation confirmed.");
		}
	}


	protected void simpleProgress(FCPMessage message) {
		Logger.debug(this, "SimpleProgress !");

		if (message.getValue("Total") != null
				&& message.getValue("Required") != null
				&& message.getValue("Succeeded") != null) {

			if ( !gotExpectedFileSize)
				fileSize = Long.parseLong(message.getValue("Required"))* FCPClientGet.BLOCK_SIZE;

			final long total = Long.parseLong(message.getValue("Total"));
			final long required = Long.parseLong(message.getValue("Required"));
			final long succeeded = Long.parseLong(message.getValue("Succeeded"));

			boolean progressReliable = false;
			if((message.getValue("FinalizedTotal") != null) &&
					message.getValue("FinalizedTotal").equals("true")) {
				progressReliable = true;
			}

			status = "Fetching";
			setBlockNumbers(required, total, succeeded, progressReliable);
			setStatus(TransferStatus.RUNNING);

		} else {
			setBlockNumbers(-1, -1, -1, false);
		}

		notifyChange();
	}

	protected void expectedDataLength(FCPMessage message) {
		fileSize = Long.parseLong(message.getValue("DataLength"));
		gotExpectedFileSize = true;
	}

	/**
	 * Handles the allData node message and attempts to write
	 * the contents to the target file.
	 */
	protected void allData(FCPQueryManager queryManager, FCPMessage message) {
		Logger.debug(this, "AllData ! : " + getIdentifier());

		gotExpectedFileSize = true;
		fileSize = message.getAmountOfDataWaiting();

		setStatus(TransferStatus.RUNNING);
		setStartupTime(Long.valueOf(message.getValue("StartupTime")));
		setCompletionTime(Long.valueOf(message.getValue("CompletionTime")));

		status = "Writing to disk";
		Logger.info(this, "Receiving file ...");

		notifyChange();

		if(fetchDirectly(queryManager.getConnection(), fileSize, true)) {
			Logger.info(this, "File received");
			writingSuccessful = true;
			status = "Available";
		} else {
			Logger.warning(this, "Unable to fetch correctly the file. This may create problems on socket");
			writingSuccessful = false;
			status = "Error while receiving the file";
		}

		if (duplicatedQueryManager != null)
			duplicatedQueryManager.getConnection().removeFromWriterQueue();

		isLockOwner = false;

		if(writingSuccessful) {
			setStatus(TransferStatus.SUCCESSFUL);
		} else {
			setStatus(TransferStatus.FAILED);
		}

		queryManager.deleteObserver(this);

		if (queryManager != this.queryManager) {
			this.queryManager.deleteObserver(this);
			queryManager.getConnection().disconnect();
			duplicatedQueryManager = null;
		}
		
		notifyChange();
	}


	protected boolean identifierMatches(FCPMessage message) {
		return (message.getValue("Identifier") != null)
				&& message.getValue("Identifier").equals(getIdentifier());
	}



	protected boolean ddaAllowed() {
		return queryManager.getConnection().isLocalSocket() && !noDDA;
	}

	protected int getPriority() {
		return priority;
	}

	protected void setPriority(int priority) {
		this.priority = priority;
	}

	private class UnlockWaiter implements ThawRunnable {
		FCPClientGet clientGet;
		FCPConnection c;
		String dir;

		public UnlockWaiter(final FCPClientGet clientGet, final FCPConnection c, final String dir) {
			this.clientGet = clientGet;
			this.dir = dir;
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

			isLockOwner = true;

			Logger.debug(this, "I take the lock !");

			if(dir == null) {
				Logger.warning(this, "UnlockWaiter.run() : Wtf ?");
			}

			clientGet.continueSaveFileTo(dir);
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

	public boolean saveFileTo(final String dir) {
		return saveFileTo(dir, true);
	}

	public synchronized boolean saveFileTo(final String dir, final boolean checkStatus) {
		fromTheNodeProgress = 0;

		Logger.info(this, "Saving file to '"+dir+"'");

		if(dir == null) {
			Logger.warning(this, "saveFileTo() : Can't save to null.");
			return false;
		}

		destinationDir = dir;


		if(checkStatus && (!isFinished() || !isSuccessful())) {
			Logger.warning(this, "Unable to fetch a file not finished");
			return false;
		}

		if(!isPersistent()) {
			Logger.warning(this, "Not persistent, so unable to ask");
			return false;
		}

		Logger.info(this, "Duplicating socket ...");

		if (globalQueue) {
			duplicatedQueryManager = queryManager.duplicate(getIdentifier());
			duplicatedQueryManager.addObserver(this);
		} else { /* won't duplicate ; else it will use another id */
			duplicatedQueryManager = queryManager;
		}

		Logger.info(this, "Waiting for socket  ...");
		status = "Waiting for socket availability ...";
		fromTheNodeProgress = 1; /* display issue */
		
		setStatus(TransferStatus.RUNNING);

		notifyChange();
		
		UnlockWaiter uw = new UnlockWaiter(this, duplicatedQueryManager.getConnection(), dir);

		final Thread fork = new Thread(new ThawThread(uw,
				"Unlock waiter",
				this));
		uw.setThread(fork);

		fork.start();

		return true;
	}

	public synchronized boolean continueSaveFileTo(final String dir) {

		Logger.info(this, "Asking file '"+filename+"' to the node...");

		destinationDir = dir;

		status = "Requesting file";
		fromTheNodeProgress = 1; /* display issue */
		setStatus(TransferStatus.RUNNING);

		notifyChange();
		
		if(destinationDir == null) {
			Logger.warning(this, "saveFileTo() : Wtf ?");
		}

		final FCPMessage getRequestStatus = new FCPMessage();

		getRequestStatus.setMessageName("GetRequestStatus");
		getRequestStatus.setValue("Identifier", getIdentifier());
		if(globalQueue)
			getRequestStatus.setValue("Global", "true");
		else
			getRequestStatus.setValue("Global", "false");
		getRequestStatus.setValue("OnlyData", "true");

		duplicatedQueryManager.writeMessage(getRequestStatus, false);

		return true;
	}


	private boolean fileExists() {
		final File newFile = new File(getPath());
		return newFile.exists();
	}


	protected File getDirectFile(){
		String filePath;
		File file;

		filePath = getPath();
		if (filePath != null) {
			file = new File(filePath);

			try{
				if(file.exists() || file.createNewFile())
				{
					return file;
				}
			}
			catch(final java.io.IOException e)
			{
				 Logger.notice(this, "First attempted filename failed");
			}
		}

		/* First try at creating the filePath failed.  Now try again removing
		   the characters that some filePath systems have troubles with.
		 */

		filePath = getSafePath();

		if(filePath != null){
			file = new File(filePath);

			try {
				Logger.notice(this, "Trying a simpler filename...");

				if(file.exists() || file.createNewFile())
				{
					return file;
				}
			} catch(final java.io.IOException e){
				Logger.notice(this, "Simpler filePath name failed: "+e.toString());
				Logger.notice(this, "filePath: "+filePath);
			}
		}
		else{
			/* Filename is null, so create a temporary filePath instead. */
			try {
				Logger.info(this, "Using temporary filePath");
				file = File.createTempFile("thaw_", ".tmp");
				finalPath = file.getPath();
				file.deleteOnExit();
				return file;
			} catch(final java.io.IOException e) {
				Logger.error(this, "Error while creating temporary filePath: "+e.toString());
			}
		}

		return null;
	}


	/**
	 * Reads the remaining data bytes from the socket as defined by size.
	 * @param connection Connection to read the data from.
	 * @param size The number of bytes to read from the connection.
	 */
	protected void dummyDataGet(FCPConnection connection, long size) {
		final int packet = FCPClientGet.PACKET_SIZE;
		byte[] read;
		int amount;

		read = new byte[packet];

		while(size > 0){
			if ( size < packet )
				read = new byte[(int)size]; /* otherwise we would read too much */
			amount = connection.read(read);

			if(amount >= 0){
				size -= amount;
			}
			else{
				/* Either EOF, or socket error */
				return;
			}
		}
	}


	private boolean fetchDirectly(final FCPConnection connection, final long expectedFileSize, final boolean reallyWrite) {
		File newFile;
		OutputStream outputStream;

		newFile = getDirectFile();
		if (reallyWrite || !newFile.exists() || (newFile.length() > 0)) {
			Logger.info(this, "Getting file from node ... ");
		} else {
			Logger.info(this, "File is supposed already written. Not rewriting.");
			status = "File already exists";
			setStatus(TransferStatus.FAILED);
			
			/* Clear the data from the socket to prevent socket problems */
			dummyDataGet(connection, expectedFileSize);
			return false;
		}

		try {
			outputStream = new FileOutputStream(newFile);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "Unable to write file on disk ... disk space / perms / filename ? : "+e.toString());
			status = "Write error";
			return false;
		}

		/* bytesRemaining == bytes remaining on socket */
		long bytesRemaining = expectedFileSize;
		long startTime = System.currentTimeMillis();

		writingSuccessful = true;

		final int packet = FCPClientGet.PACKET_SIZE;
		byte[] read = new byte[packet];

		while(bytesRemaining > 0) {

			int amount;

			if ( bytesRemaining < packet )
				read = new byte[(int) bytesRemaining]; /* otherwise we would read too much */

			amount = connection.read(read);

			if (amount >= 0) {
				bytesRemaining -= amount;
				try {
					outputStream.write(read, 0, amount);
					if( System.currentTimeMillis() >= (startTime+3000)) {
						status = "Writing to disk";
						fromTheNodeProgress = (int) (((expectedFileSize - bytesRemaining) * 100) / expectedFileSize);

						if (fromTheNodeProgress <= 0) /* display issue */
							fromTheNodeProgress = 1;

						notifyChange();

						startTime = System.currentTimeMillis();
					}
				} catch(final java.io.IOException e) {
					/* Unable to continue writing to the file.  Disable writing, but
					 * keep reading data from the socket so that the socket doesn't
					 * get messed up.
					 */
					Logger.error(this, "Unable to write file on disk ... out of space ? : "+e.toString());
					status = "Unable to fetch / disk probably full !";
					writingSuccessful = false;
					setStatus(TransferStatus.FAILED);
					try {
						outputStream.close();
					} catch(java.io.IOException ex) {
						Logger.error(this, "Unable to close the file cleanly : "+ex.toString());
						Logger.error(this, "Things seem to go wrong !");
					}
					newFile.delete();
					dummyDataGet(connection, bytesRemaining);
					return false;
				}
			} else {
				Logger.error(this, "Socket closed, damn !");
				status = "Unable to read data from the node";
				writingSuccessful = false;
				setStatus(TransferStatus.FAILED);
				try {
					outputStream.close();
				} catch(java.io.IOException ex) {
					Logger.error(this, "Unable to close the file cleanly : "+ex.toString());
					Logger.error(this, "Things seem to go wrong !");
				}
				newFile.delete();
				return false;
			}
		}

		fromTheNodeProgress = 100;

		if(reallyWrite) {
			try {
				outputStream.close();

				if(!writingSuccessful && (newFile != null))
					newFile.delete();

			} catch(final java.io.IOException e) {
				Logger.notice(this, "Unable to close correctly file on disk !? : "+e.toString());
			}
		}

		Logger.info(this, "File written");


		return true;
	}


	public boolean removeRequest() {
		final FCPMessage stopMessage = new FCPMessage();

		if(!isPersistent()) {
			Logger.notice(this, "Can't remove non persistent request.");

			return false;
		}

		if(getIdentifier() == null) {
			Logger.notice(this, "Can't remove non-started queries");
			return true;
		}

		stopMessage.setMessageName("RemovePersistentRequest");

		if(globalQueue)
			stopMessage.setValue("Global", "true");
		else
			stopMessage.setValue("Global", "false");

		stopMessage.setValue("Identifier", getIdentifier());

		queryManager.writeMessage(stopMessage);

		if ( isSuccessful() )
			setStatus(TransferStatus.SUCCESSFUL);
		else
			setStatus(TransferStatus.FAILED);

		return true;
	}

	public boolean pause(final FCPQueueManager queryManager) {
		/* TODO ? : Reduce the priority
		   instead of stopping */

		Logger.info(this, "Pausing fetching of the key : "+getFileKey());

		removeRequest();
		
		setStatus(TransferStatus.NOT_RUNNING);

		status = "Delayed";

		notifyChange();

		return true;

	}
	
	public boolean stop() {
		return stop(queryManager, true);
	}

	public boolean stop(final FCPQueryManager queryManager, boolean notify) {
		Logger.info(this, "Stop fetching of the key : "+getFileKey());
		
		if(isPersistent() && !removeRequest())
			return false;
		
		queryManager.deleteObserver(this);

		boolean wasFinished = isFinished();

		if(wasFinished && isSuccessful()) {
			setStatus(TransferStatus.SUCCESSFUL);
		} else {
			setStatus(TransferStatus.FAILED);
		}

		fatal = true;
		status = "Stopped";

		if (!restartIfFailed && !wasFinished && notify) {
			notifyChange();
		}

		return true;
	}


	public void updatePersistentRequest(final boolean clientToken) {
		if(!isPersistent())
			return;

		final FCPMessage msg = new FCPMessage();

		msg.setMessageName("ModifyPersistentRequest");
		msg.setValue("Global", Boolean.toString(globalQueue));
		msg.setValue("Identifier", getIdentifier());
		msg.setValue("PriorityClass", Integer.toString(getPriority()));

		if(clientToken && (destinationDir != null))
			msg.setValue("ClientToken", destinationDir);

		queryManager.writeMessage(msg);

	}


	public int getThawPriority() {
		return getPriority();
	}

	public int getFCPPriority() {
		return getPriority();
	}

	public void setFCPPriority(final int prio) {
		Logger.info(this, "Setting priority to "+Integer.toString(prio));

		setPriority(prio);

		notifyChange();
	}

	public int getQueryType() {
		return 1;
	}

	public String getStatus() {
		return status;
	}

	public String getFileKey() {
		// TODO : It's a fix due to Frost
		//        => to remove when it will become unneeded

		if (filename != null && key != null
		    && key.startsWith("CHK@")
		    && key.indexOf('/') < 0) {
			return key + "/" + filename;
		}

		return key;
	}

	public long getFileSize() {
		return fileSize;
	}

	public String getPath() {
		String path = null;

		if (finalPath != null)
			path = finalPath;
		else if(destinationDir != null)
			path = destinationDir + File.separator + filename;

		if (path != null)
			path = path.replaceAll("\\|", "_");

		return path;
	}

	/**
	 * Returns the path, replacing characters that are often not
	 * allowed in file systems.  Used as a fallback in case the
	 * result of getPath() fails.
	 *
	 * @return Path with "non-standard" characters replaced with underscores.
	 */
	public String getSafePath() {
		String sanitizedFilename = "";
		String path;

		if (finalPath != null)
			path = finalPath;
		else if(destinationDir != null)
			sanitizedFilename = filename.replaceAll("[\\\\/:\"*?<>|\\r\\n]", "_");
			path = destinationDir + File.separator + sanitizedFilename;

		return path;
	}
	

	public String getFilename() {
		if (filename != null)
			return filename.replaceAll("[\\/:*?\"<>|]", "_");
		return key;
	}

	public int getAttempt() {
		return attempt;
	}

	public void setAttempt(final int x) {
		attempt = x;

		if(x == 0) {
			/* We suppose it's a restart */
			setBlockNumbers(-1, -1, -1, false);
		}
	}

	public int getMaxAttempt() {
		return maxRetries;
	}

	public boolean isWritingSuccessful() {
		return writingSuccessful;
	}

	public boolean isFatallyFailed() {
		return ((!isSuccessful()) && fatal);
	}

	public HashMap<String,String> getParameters() {
		final HashMap<String,String> result = new HashMap<String,String>();

		result.put("URI", key);
		result.put("Filename", filename);
		result.put("Priority", Integer.toString(getPriority()));
		result.put("Persistence", Integer.toString(persistence));
		result.put("Global", Boolean.toString(globalQueue));
		result.put("ClientToken", destinationDir);
		result.put("Attempt", Integer.toString(attempt));

		result.put("status", status);

		result.put("Identifier", getIdentifier());
		result.put("FileSize", Long.toString(fileSize));
		result.put("Running", Boolean.toString(isRunning()));
		result.put("Successful", Boolean.toString(isSuccessful()));
		result.put("MaxRetries", Integer.toString(maxRetries));

		return result;
	}

	public boolean isPersistent() {
		return (persistence < PERSISTENCE_UNTIL_DISCONNECT);
	}

	public boolean isGlobal() {
		return globalQueue;
	}

	public int getTransferWithTheNodeProgression() {
		return fromTheNodeProgress;
	}

	public int getGetFailedCode() {
		return getFailedCode;
	}

	public int getProtocolErrorCode() {
		return protocolErrorCode;
	}

    protected String getPersistenceString()
    {
        switch(persistence)
        {
            case PERSISTENCE_FOREVER:
                return "forever";
            case PERSISTENCE_UNTIL_NODE_REBOOT:
                return "reboot";
            case PERSISTENCE_UNTIL_DISCONNECT:
                return "connection";
            default:
                return "forever";
        }
    }
}