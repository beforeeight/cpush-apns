package com.cyou.cpush.apns.core;

import io.netty.bootstrap.SSLBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ConcurrentHashMap;

import com.cyou.cpush.apns.conf.Credentials;

/**
 * Hello world!
 * 
 */
public class BootstrapFactory {

	private static ConcurrentHashMap<Credentials, SSLBootstrap> bootstrapMap = new ConcurrentHashMap<Credentials, SSLBootstrap>();
	private static EventLoopGroup workerGroup = new NioEventLoopGroup();

	public static SSLBootstrap create(final Credentials conf) {
		SSLBootstrap bootstrap = bootstrapMap.get(conf);
		if (bootstrap == null) {
			bootstrap = new SSLBootstrap(SecureSslContextFactory.getSSLContext(conf));
			bootstrap.group(workerGroup);
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.remoteAddress(conf.getGateway().host(), conf.getGateway()
					.port());
			bootstrap = bootstrapMap.putIfAbsent(conf, bootstrap);
		}
		return bootstrapMap.get(conf);
	}
}
