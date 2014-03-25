package com.cyou.cpush.apns;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cyou.cpush.apns.conf.DefaultCredentials;
import com.cyou.cpush.apns.core.BootstrapFactory;
import com.cyou.cpush.apns.core.DefaultApnsConnection;
import com.cyou.cpush.apns.notification.DefaultNotification;
import com.cyou.cpush.apns.notification.Device;
import com.cyou.cpush.apns.notification.ErrorPacket;
import com.cyou.cpush.apns.notification.Notification;
import com.cyou.cpush.apns.notification.Payload;

public class ShortConnectionTest {
	ApnsConnection apns;

	@Before
	public void setUp() throws Exception {
		ResourceLeakDetector.setLevel(Level.ADVANCED);
		DefaultCredentials conf = getConf();
		apns = new DefaultApnsConnection(conf);
	}

	@Test
	public void testPushSingle() {
		int maxNumberPerThread = 50000;
		while (true) {
			final AtomicInteger count = new AtomicInteger(0);
			long start = System.currentTimeMillis();
			try {
				for (int j = 0; j < maxNumberPerThread; j++) {
					count.incrementAndGet();
					Notification n = new DefaultNotification(
							new Device(
									"8482ab925fe45415f2c871c07222a2150ae085eb0a2c47b0ab9b413be583842c"),
							new Payload(String.format("%04d, %2$tD %2$tT", j, new Date())));
					Future<Void> future = apns.push(n);
					future.addListener(new FutureListener<Void>() {
						@Override
						public void operationComplete(Future<Void> future) throws Exception {
							count.decrementAndGet();
							if (future.isSuccess()) {
							} else {
								Throwable throwable = future.cause();
								if (throwable instanceof ErrorPacket) {
									ErrorPacket ep = (ErrorPacket) throwable;
									System.out.println("[Failure] error-response code: "
											+ ep.getStatus() + ", identifier: " + ep.getIdentifier()
											+ ", token: "
											+ ep.getNotification().getDevice().getToken());
								} else {
									throwable.printStackTrace();
								}
							}
						}
					});
				}
				while (count.get() > 0) {
					Thread.sleep(10000);
				}
				long end = System.currentTimeMillis();
				System.out.println(String.format("%1$tD %1$tT: total cost %2$d 秒",
						new Date(), (end - start) / 1000));
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail();
			} finally {
			}
		}
	}

	// @Test
	public void testPush() {
		final AtomicInteger count = new AtomicInteger(0);
		long start = System.currentTimeMillis();
		int maxThread = 2;
		int maxNumberPerThread = 2;
		try {
			for (int i = 0; i < maxThread; i++) {
				count.incrementAndGet();
				Notification[] marray = new Notification[maxNumberPerThread];
				for (int j = 0; j < maxNumberPerThread; j++) {
					marray[j] = new DefaultNotification(
							new Device(
									"8482ab925fe45415f2c871c07222a2150ae085eb0a2c47b0ab9b413be583842c"),
							new Payload(String.format("%02d-%04d, %3$tD %3$tT", i, j,
									new Date())));
				}
				Future<Iterable<ErrorPacket>> future = apns.push(marray);
				future.addListener(new FutureListener<Iterable<ErrorPacket>>() {
					@Override
					public void operationComplete(Future<Iterable<ErrorPacket>> future)
							throws Exception {
						count.decrementAndGet();
						for (ErrorPacket ep : future.get()) {
							logFailure(ep);
						}
					}
				});
			}
			while (count.get() > 0) {
				Thread.sleep(3000);
			}
			long end = System.currentTimeMillis();
			System.out.println(String.format("%1$tD %1$tT: total cost %2$d 秒",
					new Date(), (end - start) / 1000));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		} finally {
		}
	}

	private DefaultCredentials getConf() throws IOException {
		DefaultCredentials conf = new DefaultCredentials(false);
		conf.setCertification(IOUtils.toByteArray(BootstrapFactory.class
				.getResourceAsStream("/push.p12")));
		conf.setPassword("PushTestZMX");
		return conf;
	}

	private void logFailure(ErrorPacket ep) {
		System.out.println(String.format(
				"[Failure] error-response code: %d, identifier: %d, token: %s",
				ep.getStatus(), ep.getIdentifier(), ep.getNotification().getDevice()
						.getToken()));
	}
}
