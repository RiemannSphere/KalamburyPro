package model;

/**
 * Chat message
 * 
 * @author Piotr Ko³odziejski
 */
public class ChatMessage {
	private String msgType;
	private String msgContent;

	public ChatMessage() {

	}

	public ChatMessage(MsgType msgType, String msgContent) {
		this.msgType = msgType.value;
		this.msgContent = msgContent;
	}

	public enum MsgType {
		WORD_TO_GUESS("WORD_TO_GUESS"), MESSAGE("MESSAGE"), YOU_GUESSED_IT("YOU_GUESSED_IT"), NEXT_WORD("NEXT_WORD"),
		CLEAN_CANVAS("CLEAN_CANVAS"), CLEAN_WORD_TO_GUESS("CLEAN_WORD_TO_GUESS"), SCOREBOARD("SCOREBOARD");

		private String value;

		MsgType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}

}
