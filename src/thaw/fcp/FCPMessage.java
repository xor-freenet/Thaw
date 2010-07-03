package thaw.fcp;

import java.util.Enumeration;
import java.util.Hashtable;

import thaw.core.Logger;

/**
 * This class is a generic class, able to handle all kind of FCPMessage.
 * Raw data are NOT stored inside. You *have* to handle them by yourself
 * (FCPConnection.read() / FCPConnection.write())
 * after reading / writing a message with this class.
 */
public class FCPMessage {

	private String messageName = null;
	private Hashtable<String,String> fields = null; /* String (field) -> String (value) ; See http://new-wiki.freenetproject.org/FCPv2 */
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
		FCPPluginReply
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
	 */
	public boolean loadFromRawMessage(final String rawMessage) {
		int i;

		final String[] lines = rawMessage.split("\n");

		for(i = 0 ; "".equals( lines[i] );) {
			i++;
		}

		setMessageName(lines[i]);

		for(i++; i < lines.length ; i++) {
			/* Empty lines are ignored. */
			/* Line not containing '=' (like "Data" or "EndMessage") are ignored */
			if("".equals( lines[i] ) || !(lines[i].indexOf("=") >= 0))
				continue;
			
			int equalPos = lines[i].indexOf('=');
			
			String name = lines[i].substring(0, equalPos);
			String value = lines[i].substring(equalPos+1);

			setValue(name, value);
		}


		if("ProtocolError".equals( getMessageName() )
			&& !"25".equals(getValue("Code")) ) /* code 25 == need to test DDA */
			Logger.warning(this, "PROTOCOL ERROR:\n"+toString());

		return true;
	}


	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(final String name) {
		if ((name == null) || "".equals( name )) {
			Logger.notice(this, "Setting name to empty ? weird");
		}
		else if (name.indexOf("\n") >= 0) {
			Logger.notice(this, "Name shouldn't contain '\n'");
		}

		messageName = name;
	}

	public String getValue(final String field) {
		return fields.get(field);
	}

	public Hashtable getValues() {
		return fields;
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
		if(amount == 0) {
			Logger.warning(this, "Setting amount of data waiting to 0 ?! Abnormal !");
		}

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

	public MessageType getMessageType()
	{
		String messageNameUpper = messageName.toUpperCase();

		/* Client to node */
		if( "CLIENTHELLO".equals(messageNameUpper) ) return MessageType.ClientHello;
		else if("LISTPEER".equals(messageNameUpper)) return MessageType.ListPeer;
		else if("LISTPEERS".equals(messageNameUpper)) return MessageType.ListPeers;
		else if("LISTPEERNOTES".equals(messageNameUpper)) return MessageType.ListPeerNotes;
		else if("ADDPEER".equals(messageNameUpper)) return MessageType.AddPeer;
		else if("MODIFYPEER".equals(messageNameUpper)) return MessageType.ModifyPeer;
		else if("MODIFYPEERNOTE".equals(messageNameUpper)) return MessageType.ModifyPeerNote;
		else if("REMOVEPEER".equals(messageNameUpper)) return MessageType.RemovePeer;
		else if("GETNODE".equals(messageNameUpper)) return MessageType.GetNode;
		else if("GETCONFIG".equals(messageNameUpper)) return MessageType.GetConfig;
		else if("MODIFYCONFIG".equals(messageNameUpper)) return MessageType.ModifyConfig;
		else if("TESTDDAREQUEST".equals(messageNameUpper)) return MessageType.TestDDARequest;
		else if("TESTDDARESPONSE".equals(messageNameUpper)) return MessageType.TestDDAResponse;
		else if("GENERATESSK".equals(messageNameUpper)) return MessageType.GenerateSSK;
		else if("CLIENTPUT".equals(messageNameUpper)) return MessageType.ClientPut;
		else if("CLIENTPUTDISKDIR".equals(messageNameUpper)) return MessageType.ClientPutDiskDir;
		else if("CLIENTPUTCOMPLEXDIR".equals(messageNameUpper)) return MessageType.ClientPutComplexDir;
		else if("CLIENTGET".equals(messageNameUpper)) return MessageType.ClientGet;
		else if("LOADPLUGIN".equals(messageNameUpper)) return MessageType.LoadPlugin;
		else if("RELOADPLUGIN".equals(messageNameUpper)) return MessageType.ReloadPlugin;
		else if("REMOVEPLUGIN".equals(messageNameUpper)) return MessageType.RemovePlugin;
		else if("GETPLUGININFO".equals(messageNameUpper)) return MessageType.GetPluginInfo;
		else if("FCPPLUGINMESSAGE".equals(messageNameUpper)) return MessageType.FCPPluginMessage;
		else if("SUBSCRIBEUSK".equals(messageNameUpper)) return MessageType.SubscribeUSK;
		else if("UNSUBSCRIBEUSK".equals(messageNameUpper)) return MessageType.UnsubscribeUSK;
		else if("WATCHGLOBAL".equals(messageNameUpper)) return MessageType.WatchGlobal;
		else if("GETREQUESTSTATUS".equals(messageNameUpper)) return MessageType.GetRequestStatus;
		else if("LISTPERSISTENTREQUESTS".equals(messageNameUpper)) return MessageType.ListPersistentRequests;
		else if("REMOVEREQUEST".equals(messageNameUpper)) return MessageType.RemoveRequest;
		else if("MODIFYPERSISTENTREQUEST".equals(messageNameUpper)) return MessageType.ModifyPersistentRequest;
		else if("DISCONNECT".equals(messageNameUpper)) return MessageType.Disconnect;
		else if("SHUTDOWN".equals(messageNameUpper)) return MessageType.Shutdown;
		else if("VOID".equals(messageNameUpper)) return MessageType.Void;

		/* Node to client */
		else if("NODEHELLO".equals(messageNameUpper)) return MessageType.NodeHello;
		else if("CLOSECONNECTIONDUPLICATECLIENTNAME".equals(messageNameUpper)) return MessageType.CloseConnectionDuplicateClientName;
		else if("PEER".equals(messageNameUpper)) return MessageType.Peer;
		else if("PEERNOTE".equals(messageNameUpper)) return MessageType.PeerNote;
		else if("ENDLISTPEERS".equals(messageNameUpper)) return MessageType.EndListPeers;
		else if("ENDLISTPEERNOTES".equals(messageNameUpper)) return MessageType.EndListPeerNotes;
		else if("PEERREMOVED".equals(messageNameUpper)) return MessageType.PeerRemoved;
		else if("NODEDATA".equals(messageNameUpper)) return MessageType.NodeData;
		else if("CONFIGDATA".equals(messageNameUpper)) return MessageType.ConfigData;
		else if("TESTDDAREPLY".equals(messageNameUpper)) return MessageType.TestDDAReply;
		else if("TESTDDACOMPLETE".equals(messageNameUpper)) return MessageType.TestDDAComplete;
		else if("SSKKEYPAIR".equals(messageNameUpper)) return MessageType.SSKKeypair;
		else if("PERSISTENTGET".equals(messageNameUpper)) return MessageType.PersistentGet;
		else if("PERSISTENTPUT".equals(messageNameUpper)) return MessageType.PersistentPut;
		else if("PERSISTENTPUTDIR".equals(messageNameUpper)) return MessageType.PersistentPutDir;
		else if("URIGENERATED".equals(messageNameUpper)) return MessageType.URIGenerated;
		else if("PUTSUCCESSFUL".equals(messageNameUpper)) return MessageType.PutSuccessful;
		else if("PUTFETCHABLE".equals(messageNameUpper)) return MessageType.PutFetchable;
		else if("DATAFOUND".equals(messageNameUpper)) return MessageType.DataFound;
		else if("ALLDATA".equals(messageNameUpper)) return MessageType.AllData;
		else if("STARTEDCOMPRESSION".equals(messageNameUpper)) return MessageType.StartedCompression;
		else if("FINISHEDCOMPRESSION".equals(messageNameUpper)) return MessageType.FinishedCompression;
		else if("SIMPLEPROGRESS".equals(messageNameUpper)) return MessageType.SimpleProgress;
		else if("EXPECTEDHASHES".equals(messageNameUpper)) return MessageType.ExpectedHashes;
		else if("COMPATIBILITYMODE".equals(messageNameUpper)) return MessageType.CompatibilityMode;
		else if("SENDINGTONETWORK".equals(messageNameUpper)) return MessageType.SendingToNetwork;
		else if("ENDLISTPERSISTENTREQUESTS".equals(messageNameUpper)) return MessageType.EndListPersistentRequests;
		else if("PERSISTENTREQUESTREMOVED".equals(messageNameUpper)) return MessageType.PersistentRequestRemoved;
		else if("PERSISTENTREQUESTMODIFIED".equals(messageNameUpper)) return MessageType.PersistentRequestModified;
		else if("SENDINGTONETWORKMESSAGE".equals(messageNameUpper)) return MessageType.SendingToNetworkMessage;
		else if("PUTFAILED".equals(messageNameUpper)) return MessageType.PutFailed;
		else if("GETFAILED".equals(messageNameUpper)) return MessageType.GetFailed;
		else if("PROTOCOLERROR".equals(messageNameUpper)) return MessageType.ProtocolError;
		else if("IDENTIFIERCOLLISION".equals(messageNameUpper)) return MessageType.IdentifierCollision;
		else if("UNKNOWNNODEIDENTIFIER".equals(messageNameUpper)) return MessageType.UnknownNodeIdentifier;
		else if("UNKNOWNPEERNOTETYPE".equals(messageNameUpper)) return MessageType.UnknownPeerNoteType;
		else if("SUBSCRIBEDUSK".equals(messageNameUpper)) return MessageType.SubscribedUSK;
		else if("SUBSCRIBEDUSKUPDATE".equals(messageNameUpper)) return MessageType.SubscribedUSKUpdate;
		else if("PLUGININFO".equals(messageNameUpper)) return MessageType.PluginInfo;
		else if("PLUGINREMOVED".equals(messageNameUpper)) return MessageType.PluginRemoved;
		else if("FCPPLUGINREPLY".equals(messageNameUpper)) return MessageType.FCPPluginReply;
		else throw( new IllegalArgumentException("Unknown FCP message: " + messageNameUpper));
	}
}
