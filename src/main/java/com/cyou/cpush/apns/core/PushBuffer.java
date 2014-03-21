package com.cyou.cpush.apns.core;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.cyou.cpush.apns.conf.Credentials;
import com.cyou.cpush.apns.notification.ErrorPacket;
import com.cyou.cpush.apns.notification.Notification;

public class PushBuffer {
	private static final InternalLogger log = InternalLoggerFactory
			.getInstance(PushBuffer.class);
	private EventLoopGroup eventLoopGroup;

	private DefaultApnsConnection connection;

	private ConcurrentLinkedQueue<NotificationPromise> bufferQueue = new ConcurrentLinkedQueue<NotificationPromise>();

	private static final int MAX_PER_THREAD = 10000;

	private static ConcurrentHashMap<Credentials, PushBuffer> bufferGroup = new ConcurrentHashMap<Credentials, PushBuffer>();
	private static Thread monitor;
	private static int INTERVAL_MONITOR = 3000;

	static {
		monitor = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						Enumeration<PushBuffer> buffers = bufferGroup.elements();
						for (; buffers.hasMoreElements();) {
							PushBuffer buffer = buffers.nextElement();
							if (!buffer.isEmpty()) {
								buffer.triggerPush();
							}
						}
						Thread.sleep(INTERVAL_MONITOR);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		};
		monitor.start();
	}

	public static PushBuffer newInstance(DefaultApnsConnection connection) {
		Credentials cred = connection.credential();
		PushBuffer buffer = bufferGroup.get(cred);
		if (buffer == null) {
			buffer = new PushBuffer(connection);
			bufferGroup.putIfAbsent(cred, buffer);
		}
		return buffer;
	}

	private PushBuffer(DefaultApnsConnection connection) {
		this.connection = connection;
		this.eventLoopGroup = connection.group();
	}

	public NotificationPromise push(Notification notification) {
		NotificationPromise promise = new NotificationPromise(notification);
		bufferQueue.offer(promise);
		return promise;
	}

	private void triggerPush() {
		final EventLoop eventLoop = eventLoopGroup.next();
		eventLoop.execute(new Runnable() {
			@Override
			public void run() {
				while (!bufferQueue.isEmpty()) {
					NotificationPromise tempPromise;
					ArrayList<NotificationPromise> tempPromises = new ArrayList<NotificationPromise>();
					for (int i = 0; i < MAX_PER_THREAD
							&& (tempPromise = bufferQueue.poll()) != null; i++) {
						tempPromise.executor(eventLoop);
						tempPromises.add(tempPromise);
					}
					if (!tempPromises.isEmpty()) {
						final NotificationPromise[] notifications = new NotificationPromise[tempPromises
								.size()];
						tempPromises.toArray(notifications);
						Future<Iterable<ErrorPacket>> future = connection
								.push(notifications);

						future.addListener(new FutureListener<Iterable<ErrorPacket>>() {
							@Override
							public void operationComplete(Future<Iterable<ErrorPacket>> future)
									throws Exception {
								if (future.isSuccess()) {
									/* notify the failure event to the listeners */
									Iterable<ErrorPacket> it = future.get();
									if (it != null) {
										for (ErrorPacket ep : it) {
											try {
												notifications[ep.getIdentifier()].setFailure(ep);
											} catch (Exception e) {
												log.warn(e.getMessage(), e);
											}
										}
									}
									/* notify the success event to the listeners */
									for (NotificationPromise n : notifications) {
										if (!n.isDone()) {
											n.setSuccess(null);
										}
									}
								}
							}
						});

					}
				}
			}
		});
	}

	private boolean isEmpty() {
		return this.bufferQueue.isEmpty();
	}
}
