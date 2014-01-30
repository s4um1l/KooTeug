package model;

public enum ActionType {
	PERMANENT("Permanent"),
	TRANSFER("Transfer"),
	ABORT("Abort"),
	ROLLBACK("Rollback"),
	FAIL("Fail"),
	PERMANENTANDFAIL("PermanentAndFail"),
	ABORTANDFAIL("AbortAndFail"),
	TENTATIVE("Tentative");
	
	private String type;
	
	private ActionType(String type) {
		this.type = type;
	}
	
	public static ActionType parseActionType(String typ) {
		typ = typ.trim();
		if(typ.equals("Permanent")) {
			return ActionType.PERMANENT;
		}
		else if(typ.equals("Transfer")) {
			return ActionType.TRANSFER;
		}
		else if(typ.equals("Tentative")) {
			return ActionType.TENTATIVE;
		}
		else if(typ.equals("Abort")) {
			return ActionType.ABORT;
		}
		else if(typ.equals("Fail")) {
			return ActionType.FAIL;
		}
		else if(typ.equals("PermanentAndFail")) {
			return ActionType.PERMANENTANDFAIL;
		}
		else if(typ.equals("AbortAndFail")) {
			return ActionType.ABORTANDFAIL;
		}
		else if(typ.equals("Rollback")) {
			return ActionType.ROLLBACK;
		}
		
		return ActionType.TRANSFER;
	}
	
	public String toString() {
		return type.toString();
	}
}
