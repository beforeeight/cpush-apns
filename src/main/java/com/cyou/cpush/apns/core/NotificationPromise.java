package com.cyou.cpush.apns.core;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

import com.cyou.cpush.apns.notification.Device;
import com.cyou.cpush.apns.notification.ErrorPacket;
import com.cyou.cpush.apns.notification.Notification;
import com.cyou.cpush.apns.notification.Payload;

public class NotificationPromise extends DefaultPromise<Void> implements
		Notification {
	private final Notification notification;
	private EventExecutor executor;

	public NotificationPromise(Notification notification) {
		this.notification = notification;
	}

	public NotificationPromise(Notification notification, EventExecutor executor) {
		super(executor);
		this.notification = notification;
	}

	@Override
	public Promise<Void> setFailure(Throwable cause) {
		if (cause instanceof ErrorPacket) {
			((ErrorPacket) cause).setNotification(notification);
		}
		return super.setFailure(cause);
	}

	public Notification notification() {
		return notification;
	}

	@Override
	protected EventExecutor executor() {
		return executor;
	}

	public void executor(EventExecutor executor) {
		this.executor = executor;
	}

	@Override
	public Device getDevice() {
		return notification.getDevice();
	}

	@Override
	public Payload getPayload() {
		return notification.getPayload();
	}

	@Override
	public int getIdentifier() {
		return notification.getIdentifier();
	}

}
