package thaw.fcp;

import java.util.*;

import thaw.core.Logger;

/**
 * This class is a generic class, able to handle all kind of FCPMessage.
 * Raw data are NOT stored inside. You *have* to handle them by yourself
 * (FCPConnection.read() / FCPConnection.write())
 * after reading / writing a message with this class.
 */
public class FCPMessage {

	private String messageName = null;
	private Hashtable<String,String> fields; /* String (field) -> String (value) ; See http://new-wiki.freenetproject.org/FCPv2 */
	private long dataWaiting = 0;

	public enum MessageType{
		/* Client to node */
		ClientHello,
		ListPeer,
		ListPeers,
		ListPeerNotes,
		AddPeer,
		ModifyPeer,
		ModifyPeerNote,
		RemovePeer,
		GetNode,
		GetConfig,
		ModifyConfig,
		TestDDARequest,
		TestDDAResponse,
		GenerateSSK,
		ClientPut,
		ClientPutDiskDir,
		ClientPutComplexDir,
		ClientGet,
		LoadPlugin,
		ReloadPlugin,
		RemovePlugin,
		GetPluginInfo,
		FCPPluginMessage,
		SubscribeUSK,
		UnsubscribeUSK,
		WatchGlobal,
		GetRequestStatus,
		ListPersistentRequests,
		RemoveRequest,
		ModifyPersistentRequest,
		Disconnect,
		Shutdown,
		Void,

		/* Node to client */
		NodeHello,
		CloseConnectionDuplicateClientName,
		Peer,
		PeerNote,
		EndListPeers,
		EndListPeerNotes,
		PeerRemoved,
		NodeData,
		ConfigData,
		TestDDAReply,
		TestDDAComplete,
		SSKKeypair,
		PersistentGet,
		PersistentPut,
		PersistentPutDir,
		URIGenerated,
		PutSuccessful,
		PutFetchable,
		DataFound,
		AllData,
		StartedCompression,
		FinishedCompression,
		SimpleProgress,
		ExpectedHashes,
		CompatibilityMode,
		SendingToNetwork,
		EndListPersistentRequests,
		PersistentRequestRemoved,
		PersistentRequestModified,
		SendingToNetworkMessage,
		PutFailed,
		GetFailed,
		ProtocolError,
		IdentifierCollision,
		UnknownNodeIdentifier,
		UnknownPeerNoteType,
		SubscribedUSK,
		SubscribedUSKUpdate,
		PluginInfo,
		PluginRemoved,
		FCPPluginReply,
		ExpectedMIME,
		ExpectedDataLength,

		/* Unrecognised message identifier */
		UNKNOWN_MESSAGE;

		private static final Map<String,MessageType> messageMap = new HashMap<String,MessageType>();

		static {
			for(MessageType messageType : MessageType.values()) {
				messageMap.put(messageType.name().toUpperCase(), messageType);
			}
		}

		/**
		 * Given a message name string, returns the associated message enumeration.
		 *
		 * @param message Message name string as defined in http://new-wiki.freenetproject.org/FCPv2.
		 * @return The associated message enumeration value, if found.  If not found, return null.
		 */
		public static MessageType getMessageType(String message) {
			MessageType result = null;

			if(message != null)
			{
				result = messageMap.get(message.trim().toUpperCase());
			}

			if(result == null) {
				result = UNKNOWN_MESSAGE;
			}
			
			return result;
		}
	}

	public FCPMessage() {
		fields = new Hashtable<String,String>();
	}

	/**
	 * As you can't fetch the value returns by loadFromRawMessage(), this constructor is not recommended.
	 */
	public FCPMessage(final String rawMessage) {
		this();
		loadFromRawMessage(rawMessage);
	}


	/**
	 * Raw message does not need to finish by "EndMessage" / "Data".
	 * Removes existing fields while loading in the new raw message.
	 * If null or a string with no valid fields is passed, the message
	 * will be empty.
	 */
	public boolean loadFromRawMessage(final String rawMessage) {
		/* Loading a new raw message - remove the existing fields */
		fields = new Hashtable<String,String>();
		setAmountOfDataWaiting(0);

		if(rawMessage != null) {
			int messageIdentifierLine;
			ArrayList<String> lines = new ArrayList<String>(Arrays.asList(rawMessage.split("\n")));
			messageIdentifierLine = findMessageIdentifier(lines);

			if((0 <= messageIdentifierLine) && (messageIdentifierLine < lines.size())) {
				setMessageName(lines.get(messageIdentifierLine));
				setMessageFields(lines, messageIdentifierLine+1);

				if("ProtocolError".equals( getMessageName() )
					&& !"25".equals(getValue("Code")) ) /* code 25 == need to test DDA */
					Logger.warning(this, "PROTOCOL ERROR:\n"+toString());
			} else {
				setMessageName("");
			}
		} else {
			setMessageName("");
		}

		return true;
	}

	/**
	 * Returns the first line number that contains a valid message identifier.  If a field
	 * is found (denoted by the "=" character) before a valid identifier is found, returns -1.
	 * If a valid identifier is not found, returns -1.
	 *
	 * @param messageLines ArrayList<String> containing the lines of a message.
	 *                     If null, does nothing and returns -1.
	 * @return The line number starting at 0 of the message identifier.  On error, -1.
	 */
	private int findMessageIdentifier(final ArrayList<String> messageLines) {
		if(messageLines != null) {
			int lineNumber = 0;
			for(String line : messageLines) {
				line = line.trim();
				if(line.length() > 0) {
					if(line.contains("=")) {
						/* Missing message identifier? */
						return -1;
					} else {
						return lineNumber;
					}
				}
				lineNumber++;
			}
		}

		return -1;
	}

	/**
	 * Extracts and stores fields from a message.
	 * Empty lines are ignored.
	 * Lines not containing '=' (like "Data" or "EndMessage") are ignored
	 * Lines with a leading '=' (like "=True") are malformed and are ignored.
	 *
	 * @param messageLines The message from which to extract the fields and values.  If null, does nothing.
	 * @param startLine The line number in messageLines to start searching from.
	 */
	private void setMessageFields(final ArrayList<String> messageLines, int startLine) {
		if(messageLines != null) {
			for(int currentLineNum = startLine; currentLineNum < messageLines.size(); currentLineNum++) {
				String[] line = messageLines.get(currentLineNum).split("=",2);

				if(line.length == 2) {
					line[0] = line[0].trim();
					if(line[0].length() > 0) {
						setValue(line[0], line[1]);
					} else {
						/* No field identifier, ignoring */
					}
				} else {
					/* String not in the form "field=value", ignoring */
				}
			}
		}
	}


	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(final String name) {
		if ((name == null) || "".equals( name.trim() )) {
			Logger.notice(this, "Setting name to empty ? weird");
		}
		else if (name.indexOf("\n") >= 0) {
			Logger.notice(this, "Name shouldn't contain '\n'");
		}
		else if (name.contains("=")) {
			Logger.notice(this, "Name shouldn't contain '='");
		}

		messageName = name;
	}

	public String getValue(final String field) {
		return fields.get(field);
	}

	public Hashtable<String,String> getValues() {
		return new Hashtable<String,String>(fields);
	}

	public void setValue(final String field, final String value) {
		if("DataLength".equals( field )) {
			setAmountOfDataWaiting((new Long(value)).longValue());
		}

		if(value == null) {
			fields.remove(field);
			return;
		}

		fields.put(field, value);
	}


	/**
	 * Returns the amount of data waiting on socket (in octets).
	 * @return if > 0 : Data are still waiting (except if the message name is "PersistentPut" !), if == 0 : No data waiting, if < 0 : These data are now unavailable.
	 */
	public long getAmountOfDataWaiting() {
		return dataWaiting;
	}


	public void setAmountOfDataWaiting(final long amount) {
		dataWaiting = amount;
	}


	/**
	 * Generate FCP String to send.
	 * If amount of data waiting is set to > 0, then, a field "DataLength" is added,
	 * and resulting string finish by "Data", else resulting string simply finish by "EndMessage".
	 */
	public String toString() {
		String result = "";

		result = result + getMessageName() + "\n";

		for(final Enumeration fieldNames = fields.keys() ; fieldNames.hasMoreElements();) {
			final String fieldName = ((String)fieldNames.nextElement());

			result = result + fieldName + "=" + getValue(fieldName) + "\n";
		}

		if(getAmountOfDataWaiting() == 0)
			result = result + "EndMessage\n";
		else {
			result = result + "DataLength="+ (new Long(getAmountOfDataWaiting())).toString() + "\n";
			result = result + "Data\n";
		}

		return result;
	}

	/**
	 * @return The associated message enumeration value, if found.  If not found, return null. 
	 */
	public MessageType getMessageType()
	{
		return MessageType.getMessageType(messageName);
	}
}
