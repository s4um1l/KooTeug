package model;

public enum MessageType {
	
	CHECKPOINT_REQUEST("Request"),
	APPLICATION("Application"),
	ABORT("Abort"),
	YES("Yes"),
	OK("Ok"),
	RBOK("Rollback_Ok"),
	RBAGREED("Rollback_Agreed"),
	PERMANENT("Permanent"),
	FAIL("Fail"),
	ROLLBACK("Rollback"),
	NO("No");
	
	private String type;
	
	private MessageType(String type) {
		this.type = type;
	}
	
	public static MessageType parseMessageType(String typ) {
		typ = typ.trim();
		if(typ.equals("Request")) {
			return MessageType.CHECKPOINT_REQUEST;
		}
		else if(typ.equals("Abort")) {
			return MessageType.ABORT;
		}
		else if(typ.equals("Yes")) {
			return MessageType.YES;
		}
		else if(typ.equals("No")) {
			return MessageType.NO;
		}
		else if(typ.equals("Ok")) {
			return MessageType.OK;
		}
		else if(typ.equals("Permanent")) {
			return MessageType.PERMANENT;
		}
		else if(typ.equals("Fail")) {
			return MessageType.FAIL;
		}
		else if(typ.equals("Rollback")) {
			return MessageType.ROLLBACK;
		}
		else if(typ.equals("Application")) {
			return MessageType.APPLICATION;
		}
		else if(typ.equals("Rollback_Ok")) {
			return MessageType.RBOK;
		}
		else if(typ.equals("Rollback_Agreed")) {
			return MessageType.RBAGREED;
		}
		return MessageType.CHECKPOINT_REQUEST;
	}
	
	public String toString() {
		return type.toString();
	}

}
