package com.cyou.cpush.apns.notification;

public interface Notification {

	public abstract Device getDevice();

	public abstract Payload getPayload();

	public abstract int getIdentifier();

}