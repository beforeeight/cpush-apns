package com.cyou.cpush.apns.core;

import io.netty.bootstrap.SSLBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericProgressiveFutureListener;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.List;

import com.cyou.cpush.apns.ApnsConnection;
import com.cyou.cpush.apns.conf.Credentials;
import com.cyou.cpush.apns.handler.ApnsInboundHandler;
import com.cyou.cpush.apns.handler.ApnsOutboundHandler;
import com.cyou.cpush.apns.notification.DefaultNotification;
import com.cyou.cpush.apns.notification.Device;
import com.cyou.cpush.apns.notification.ErrorPacket;
import com.cyou.cpush.apns.notification.Notification;
import com.cyou.cpush.apns.notification.Payload;

public final class DefaultApnsConnection implements ApnsConnection {
	/**
	 * configuration, such as p12 certification and password
	 */
	private Credentials credential;

	private final SSLBootstrap bootstrap;

	private PushBuffer buffer;

	public DefaultApnsConnection(Credentials credential) {
		this.credential = credential;
		this.bootstrap = BootstrapFactory.create(credential);
		buffer = PushBuffer.newInstance(this);
	}

	@Override
	public Future<Void> push(Notification notification) {
		NotificationPromise promise = buffer.push(notification);
		return promise;
	}

	@Override
	public Future<Iterable<ErrorPacket>> push(Notification... message) {
		ApnsShortConnectionThread thread = new ApnsShortConnectionThread(bootstrap,
				message);
		return thread.send();
	}

	EventLoopGroup group() {
		return bootstrap.group();
	}

	Credentials credential() {
		return credential;
	}
}

class ApnsShortConnectionThread {
	private static final InternalLogger log = InternalLoggerFactory
			.getInstance(DefaultApnsConnection.class);

	private final SSLBootstrap bootstrap;

	/**
	 * all the notification which will be sent
	 */
	private final SortedNotification[] notifications;
	/**
	 * all the sending failure notification received from apns server
	 */
	private final List<ErrorPacket> failedNotifications;
	/**
	 * the next index of notification which will be sent
	 */
	private int nextIndex = 0;
	/**
	 * the index at which the sending stopped
	 */
	private int stopIndex = -1;

	public static final Notification TAIL_INVALID_NOTIFICATION = new DefaultNotification(
			new Device(""), new Payload(""));

	ApnsShortConnectionThread(SSLBootstrap bootstrap, Notification... message) {
		this.bootstrap = bootstrap;
		// this.conf = conf;
		if (message != null) {
			this.notifications = new SortedNotification[message.length + 1];
			for (int i = 0; i < message.length; i++) {
				this.notifications[i] = new SortedNotification(message[i], i);
			}
			this.notifications[message.length] = new SortedNotification(
					TAIL_INVALID_NOTIFICATION, message.length);
		} else {
			this.notifications = new SortedNotification[] {};
		}
		failedNotifications = new ArrayList<ErrorPacket>();
	}

	public Future<Iterable<ErrorPacket>> send() {
		final NotificationsPromise promise = new NotificationsPromise(bootstrap
				.group().next());

		promise
				.addListener(new GenericProgressiveFutureListener<ProgressivePromise<Iterable<ErrorPacket>>>() {

					@Override
					public void operationComplete(
							ProgressivePromise<Iterable<ErrorPacket>> future)
							throws Exception {
						// NOOP
					}

					@Override
					public void operationProgressed(
							ProgressivePromise<Iterable<ErrorPacket>> future, long progress,
							long total) throws Exception {
						ApnsShortConnectionThread connection = ApnsShortConnectionThread.this;
						byte code = (byte) (progress & 0xffffffff);
						int identifier = (int) (progress >> 32);
						/* relocated the index if a notification can not be delivered */
						connection.relocatedIndex(identifier);
						if (connection.isDone()) {
							/* if the index is the tail invalid notification, set Success */
							if (log.isDebugEnabled()) {
								log.debug("a pushing progress has done.");
							}
							future.setSuccess(connection.getFailedNotifications());
						} else {
							/* add the failed notification to the FailureNotification List */
							if (log.isDebugEnabled()) {
								log.debug(String
										.format(
												"a notification could not be delivered, identifier: %d, error-response code: %d",
												identifier, code));
							}
							connection.addFailureNotification(identifier, code);
							/*
							 * reconnect to APNS server in order to send the rest of
							 * notification
							 */
							ApnsShortConnectionThread.connect(connection, promise);
						}
					}
				});

		connect(this, promise);
		return promise;
	}

	private static final int MAX_HANDSHAKE_RETRY = 3;
	private int handshakeTimes = 0;

	private static void connect(final ApnsShortConnectionThread connection,
			final NotificationsPromise promise) {
		/* create a new channel */
		Future<Channel> future = connect0(connection, promise);
		/* listen on the connected and handshake success event */
		future.addListener(new FutureListener<Channel>() {
			@Override
			public void operationComplete(Future<Channel> future) throws Exception {
				if (future.isSuccess()) {
					if (log.isDebugEnabled()) {
						log.debug("a new channel has been registered and connected to the APNS server");
					}
					/* write data to the channel after handshake success */
					send(connection, future.get());
				} else {
					log.warn("a new channel connected to the APNS server fail");
					connection.handshakeTimes++;
					if (connection.handshakeTimes <= MAX_HANDSHAKE_RETRY) {
						connect(connection, promise);
					}
				}
			}
		});
	}

	private static Future<Channel> connect0(
			final ApnsShortConnectionThread connection,
			final NotificationsPromise promise) {
		/* create a new channel and initial several handlers */
		ChannelFuture connFuture = connection.bootstrap.connect(
				new ApnsOutboundHandler(), new ApnsInboundHandler(promise));
		final Channel channel = connFuture.channel();
		return channel.pipeline().get(SslHandler.class).handshakeFuture();
	}

	private static void send(final ApnsShortConnectionThread connection,
			final Channel channel) {
		for (int i = connection.getNextIndex(); i < connection.getNotifications().length; i++) {
			channel.write(connection.notifications[i]);
		}
	}

	class SortedNotification implements Notification {
		private Notification principal;
		private int identifier;

		SortedNotification(Notification notification, int identifier) {
			this.principal = notification;
			this.identifier = identifier;
		}

		@Override
		public Device getDevice() {
			return principal.getDevice();
		}

		@Override
		public Payload getPayload() {
			return principal.getPayload();
		}

		@Override
		public int getIdentifier() {
			return identifier;
		}

		Notification principal() {
			return principal;
		}
	}

	protected void addFailureNotification(int identifier, byte code) {
		if (identifier >= 0 && identifier < notifications.length) {
			ErrorPacket ep = new ErrorPacket(code, identifier);
			ep.setNotification(notifications[identifier].principal());
			failedNotifications.add(ep);
		}
	}

	protected void relocatedIndex(int stop) {
		this.stopIndex = stop;
		this.nextIndex = this.stopIndex + 1;
	}

	protected Notification[] getNotifications() {
		return notifications;
	}

	protected int getNextIndex() {
		return nextIndex;
	}

	protected int getStopIndex() {
		return stopIndex;
	}

	public boolean isDone() {
		return this.getStopIndex() >= this.getNotifications().length - 1
				|| this.getNextIndex() >= this.getNotifications().length;
	}

	public List<ErrorPacket> getFailedNotifications() {
		return failedNotifications;
	}

	public static void main(String args[]) {
	}

}
