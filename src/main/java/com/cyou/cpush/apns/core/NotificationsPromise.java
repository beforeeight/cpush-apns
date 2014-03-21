package com.cyou.cpush.apns.core;

import io.netty.util.concurrent.DefaultProgressivePromise;
import io.netty.util.concurrent.EventExecutor;

import com.cyou.cpush.apns.notification.ErrorPacket;

public class NotificationsPromise extends
		DefaultProgressivePromise<Iterable<ErrorPacket>> {

	public NotificationsPromise(EventExecutor executor) {
		super(executor);
	}

}
