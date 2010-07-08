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
		assertEquals(defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
	}

	@Test
	public void testGetMessageTypeWithEmpty() {
		defaultMessage.setMessageName("");
		assertEquals(defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
	}

	@Test
	public void testGetMessageTypeWithSingleSpace() {
		defaultMessage.setMessageName(" ");
		assertEquals(defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
	}

	@Test
	public void testGetMessageTypeWithUnknownMessageName() {
		defaultMessage.setMessageName("foo");
		assertEquals("Unknown message", defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
	}

	@Test
	public void testGetMessageTypeWithAllLowerCaseMessageName() {
		defaultMessage.setMessageName("expectedhashes");
		assertEquals("Case-insensitivity", defaultMessage.getMessageType(), FCPMessage.MessageType.ExpectedHashes);
	}

	@Test
	public void testGetMessageTypeWithAllUpperCaseMessageName() {
		defaultMessage.setMessageName("EXPECTEDHASHES");
		assertEquals("Case-insensitivity", defaultMessage.getMessageType(), FCPMessage.MessageType.ExpectedHashes);
	}

	@Test
	public void testGetMessageTypeWithSurroundingSpaces() {
		defaultMessage.setMessageName("   expectedhashes ");
		assertEquals("Surrounding white-space", defaultMessage.getMessageType(), FCPMessage.MessageType.ExpectedHashes);
	}

	@Test
	public void testGetMessageTypeWithExpectedHashes() {
		defaultMessage.setMessageName("ExpectedHashes");
		assertEquals("Valid message name", defaultMessage.getMessageType(), FCPMessage.MessageType.ExpectedHashes);
	}

	@Test
	public void testGetMessageTypeWithCompatibilityMode() {
		defaultMessage.setMessageName("CompatibilityMode");
		assertEquals("Valid message name", defaultMessage.getMessageType(), FCPMessage.MessageType.CompatibilityMode);
	}

	@Test
	public void testLoadFromRawMessageValidWithPayload(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"Global=true\n" +
				"DataLength=37261\n" +
				"EndMessage");
		assertEquals("Valid message, with payload", defaultMessage.getMessageType(), FCPMessage.MessageType.DataFound);
		assertEquals("Data payload", defaultMessage.getAmountOfDataWaiting(), 37261);
		assertEquals("Fields", defaultMessage.getValues().size(), 2);
	}

	@Test
	public void testLoadFromRawMessageValidWithoutPayload(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"Global=true\n" +
				"EndMessage");
		assertEquals("Valid message, without payload", defaultMessage.getMessageType(), FCPMessage.MessageType.DataFound);
		assertEquals("No data payload", defaultMessage.getAmountOfDataWaiting(), 0);
		assertEquals("Fields", defaultMessage.getValues().size(), 1);
	}

	@Test
	public void testLoadFromRawMessageEmptyMessage(){
		defaultMessage.loadFromRawMessage("");
		assertEquals("Blank raw message", defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
		assertEquals("No data payload", defaultMessage.getAmountOfDataWaiting(), 0);
		assertEquals("No fields", defaultMessage.getValues().size(), 0);
	}

	@Test
	public void testLoadFromRawMessageNewlines(){
		defaultMessage.loadFromRawMessage(" \n     \n \n");
		assertEquals("Blank raw message", defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
		assertEquals("No data payload", defaultMessage.getAmountOfDataWaiting(), 0);
		assertEquals("No fields", defaultMessage.getValues().size(), 0);
	}

	@Test
	public void testLoadFromRawMessageNull(){
		defaultMessage.loadFromRawMessage(null);
		assertEquals("Null input", defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
		assertEquals("No data payload", defaultMessage.getAmountOfDataWaiting(), 0);
		assertEquals("No fields", defaultMessage.getValues().size(), 0);
	}

	@Test
	public void testLoadFromRawMessageWithMalformedField(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"=true\n" +
				"EndMessage");
		assertEquals("Malformed message", defaultMessage.getMessageType(), FCPMessage.MessageType.DataFound);
		assertEquals("Fields", defaultMessage.getValues().size(), 0);
	}

	@Test
	public void testLoadFromRawMessageWithEmptyValue(){
		defaultMessage.loadFromRawMessage(
				"DataFound\n" +
				"Global=\n" +
				"EndMessage");
		assertEquals("Valid message", defaultMessage.getMessageType(), FCPMessage.MessageType.DataFound);
		assertEquals("Fields", defaultMessage.getValues().size(), 1);
	}

	@Test
	public void testLoadFromRawMessageWithMalformedMessageType(){
		defaultMessage.loadFromRawMessage(
				"Global=true\n" +
				"DataFound\n" +
                "EndMessage");
		assertEquals("Invalid message", defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
		assertEquals("Fields", defaultMessage.getValues().size(), 0);
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
		assertEquals("Valid message, with payload", defaultMessage.getMessageType(), FCPMessage.MessageType.UNKNOWN_MESSAGE);
		assertEquals("Data payload", defaultMessage.getAmountOfDataWaiting(), 0);
		assertEquals("Fields", defaultMessage.getValues().size(), 0);
	}

//	@Test
//	public void testGetMessageNameUnknownMessage() {
//		assertEquals(defaultMessage.getMessageName(), FCPMessage.MessageType.UNKNOWN_MESSAGE.name());
//	}

//	@Test
//	public void testGetMessageNameKnownMessage() {
//		defaultMessage.setMessageName("ClientGet");
//		assertEquals(defaultMessage.getMessageName(), FCPMessage.MessageType.ClientGet.name());
//	}

//	@Test
//	public void testGetMessageNameTestCase() {
//		defaultMessage.setMessageName("CLIENTget");
//		assertEquals(defaultMessage.getMessageName(), FCPMessage.MessageType.ClientGet.name());
//	}
}
