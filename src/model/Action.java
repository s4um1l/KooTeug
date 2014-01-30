package model;

public class Action {
	
	private ActionType actionType;
	private double amount;
	
	public Action(ActionType actionType) {
		this.actionType = actionType;
		amount = 0;
	}
	
	public Action(ActionType actionType, double amount) {
		this.actionType = actionType;
		this.amount = amount;
	}
	
	public ActionType getActionType() {
		return actionType;
	}
	
	public double getAmount() {
		return amount;
	}
}
