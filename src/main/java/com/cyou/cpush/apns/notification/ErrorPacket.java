package com.cyou.cpush.apns.notification;

public class ErrorPacket extends Throwable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -283103614954000880L;
	private int identifier;
	private byte status;
	private Notification notification;


	public ErrorPacket(byte status, int identifier) {
		super();
		this.status = status;
		this.identifier = identifier;
	}

	public int getIdentifier() {
		return identifier;
	}

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public Notification getNotification() {
		return notification;
	}

	public void setNotification(Notification notification) {
		this.notification = notification;
	}

}
