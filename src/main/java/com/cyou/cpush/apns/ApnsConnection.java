package com.cyou.cpush.apns;

import io.netty.util.concurrent.Future;

import com.cyou.cpush.apns.notification.ErrorPacket;
import com.cyou.cpush.apns.notification.Notification;

public interface ApnsConnection {
	public Future<Void> push(Notification notification);

	public Future<Iterable<ErrorPacket>> push(Notification... notifications);
}
