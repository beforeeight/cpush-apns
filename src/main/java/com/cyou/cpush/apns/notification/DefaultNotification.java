package com.cyou.cpush.apns.notification;

import java.util.concurrent.atomic.AtomicInteger;


public class DefaultNotification implements Notification {
	private static AtomicInteger IDENTIFIER_GENERATOR = new AtomicInteger(Integer.MAX_VALUE-1);
	
	private int identifier;
	private Device device;
	private Payload payload;

	public DefaultNotification(Device device, Payload payload) {
		this(IDENTIFIER_GENERATOR.incrementAndGet(), device, payload);
	}

	public DefaultNotification(int identifier, Device device, Payload payload) {
		this.identifier = identifier;
		this.device = device;
		this.payload = payload;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cyou.cpush.apns.Notification#getDevice()
	 */
	@Override
	public Device getDevice() {
		return device;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cyou.cpush.apns.Notification#getPayload()
	 */
	@Override
	public Payload getPayload() {
		return payload;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public void setPayload(Payload payload) {
		this.payload = payload;
	}

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	@Override
	public int getIdentifier() {
		return identifier;
	}
}
