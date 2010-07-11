package thaw.fcp;

import org.junit.Test;
import junit.framework.TestCase;

public class FCPMessageTest extends TestCase{
	private FCPMessage defaultMessage;
	public void setUp() {
		defaultMessage = new FCPMessage();
	}

	@Test
	public void testGetMessageTypeWithNull() {
		defaultMessage.setMessageName(null);
		assertEquals(FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithEmpty() {
		defaultMessage.setMessageName("");
		assertEquals(FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithSingleSpace() {
		defaultMessage.setMessageName(" ");
		assertEquals(FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithUnknownMessageName() {
		defaultMessage.setMessageName("foo");
		assertEquals("Unknown message", FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithAllLowerCaseMessageName() {
		defaultMessage.setMessageName("expectedhashes");
		assertEquals("Case-insensitivity", FCPMessage.MessageType.ExpectedHashes, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithAllUpperCaseMessageName() {
		defaultMessage.setMessageName("EXPECTEDHASHES");
		assertEquals("Case-insensitivity", FCPMessage.MessageType.ExpectedHashes, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithSurroundingSpaces() {
		defaultMessage.setMessageName("   expectedhashes ");
		assertEquals("Surrounding white-space", FCPMessage.MessageType.ExpectedHashes, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithExpectedHashes() {
		defaultMessage.setMessageName("ExpectedHashes");
		assertEquals("Valid message name", FCPMessage.MessageType.ExpectedHashes, defaultMessage.getMessageType());
	}

	@Test
	public void testGetMessageTypeWithCompatibilityMode() {
		defaultMessage.setMessageName("CompatibilityMode");
		assertEquals("Valid message name", FCPMessage.MessageType.CompatibilityMode, defaultMessage.getMessageType());
	}

	@Test
	public void testLoadFromRawMessageValidWithPayload(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"Global=true\n" +
				"DataLength=37261\n" +
				"EndMessage");
		assertEquals("Valid message, with payload", FCPMessage.MessageType.DataFound, defaultMessage.getMessageType());
		assertEquals("Data payload", 37261, defaultMessage.getAmountOfDataWaiting());
		assertEquals("Fields", 2, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageValidWithoutPayload(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"Global=true\n" +
				"EndMessage");
		assertEquals("Valid message, without payload", FCPMessage.MessageType.DataFound, defaultMessage.getMessageType());
		assertEquals("No data payload", 0, defaultMessage.getAmountOfDataWaiting());
		assertEquals("Fields", 1, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageEmptyMessage(){
		defaultMessage.loadFromRawMessage("");
		assertEquals("Blank raw message", FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
		assertEquals("No data payload", 0, defaultMessage.getAmountOfDataWaiting());
		assertEquals("No fields", 0, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageNewlines(){
		defaultMessage.loadFromRawMessage(" \n     \n \n");
		assertEquals("Blank raw message", FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
		assertEquals("No data payload", 0, defaultMessage.getAmountOfDataWaiting());
		assertEquals("No fields", 0, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageNull(){
		defaultMessage.loadFromRawMessage(null);
		assertEquals("Null input", FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
		assertEquals("No data payload", 0, defaultMessage.getAmountOfDataWaiting());
		assertEquals("No fields", 0, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageWithMalformedField(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"=true\n" +
				"EndMessage");
		assertEquals("Malformed message", FCPMessage.MessageType.DataFound, defaultMessage.getMessageType());
		assertEquals("Fields", 0, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageWithEmptyValue(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"Global=\n" +
				"EndMessage");
		assertEquals("Valid message", FCPMessage.MessageType.DataFound, defaultMessage.getMessageType());
		assertEquals("Fields", 1, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageWithMalformedMessageType(){
		defaultMessage.loadFromRawMessage(
				"Global=true\n" +
				"DataFound\n" +
                "EndMessage");
		assertEquals("Invalid message", FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
		assertEquals("Fields", 0, defaultMessage.getValues().size());
	}

	@Test
	public void testLoadFromRawMessageResetExistingMessage(){
		FCPMessage message = new FCPMessage();
		message.loadFromRawMessage(
				"DataFound\n" +
				"Global=true\n" +
				"DataLength=37261\n" +
				"EndMessage");
		message.loadFromRawMessage("");
		assertEquals("Valid message, with payload", FCPMessage.MessageType.UNKNOWN_MESSAGE, defaultMessage.getMessageType());
		assertEquals("Data payload", 0, defaultMessage.getAmountOfDataWaiting());
		assertEquals("Fields", 0, defaultMessage.getValues().size());
	}
}
