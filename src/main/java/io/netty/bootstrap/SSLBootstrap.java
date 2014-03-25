package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class SSLBootstrap extends AbstractBootstrap<SSLBootstrap, Channel>
		implements HandlerBootstarp<ChannelHandler> {

	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(SSLBootstrap.class);
	private static final String SSL_HANDLER = "SSL_HANDLER";
	private volatile SocketAddress remoteAddress;

	private volatile SSLContext sslContext;

	private ThreadLocal<ChannelHandler[]> initHandlers = new ThreadLocal<ChannelHandler[]>();

	public SSLBootstrap(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	private SSLBootstrap(SSLBootstrap bootstrap) {
		super(bootstrap);
		remoteAddress = bootstrap.remoteAddress;
		sslContext = bootstrap.sslContext;
	}

	/**
	 * The {@link SocketAddress} to connect to once the {@link #connect()} method
	 * is called.
	 */
	public SSLBootstrap remoteAddress(SocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}

	/**
	 * @see {@link #remoteAddress(SocketAddress)}
	 */
	public SSLBootstrap remoteAddress(String inetHost, int inetPort) {
		remoteAddress = new InetSocketAddress(inetHost, inetPort);
		return this;
	}

	/**
	 * @see {@link #remoteAddress(SocketAddress)}
	 */
	public SSLBootstrap remoteAddress(InetAddress inetHost, int inetPort) {
		remoteAddress = new InetSocketAddress(inetHost, inetPort);
		return this;
	}

	public SSLContext sslContext() {
		return sslContext;
	}

	public void sslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * Connect a {@link Channel} to the remote peer.
	 */
	public ChannelFuture connect() {
		EventLoop eventLoop = null;
		return connect(eventLoop);
	}

	public ChannelFuture connect(EventLoop eventLoop) {
		validate();
		SocketAddress remoteAddress = this.remoteAddress;
		if (remoteAddress == null) {
			throw new IllegalStateException("remoteAddress not set");
		}
		return doConnect(remoteAddress, localAddress(), eventLoop);
	}

	@Override
	public ChannelFuture connect(ChannelHandler... handlers) {
		EventLoop eventLoop = null;
		return connect(eventLoop, handlers);
	}

	public ChannelFuture connect(EventLoop eventLoop, ChannelHandler... handlers) {
		if (handlers != null) {
			initHandlers.set(handlers);
		}
		ChannelFuture future = connect(eventLoop);
		if (handlers != null) {
			initHandlers.remove();
		}
		return future;
	}

	/**
	 * Connect a {@link Channel} to the remote peer.
	 */
	public ChannelFuture connect(String inetHost, int inetPort) {
		return connect(new InetSocketAddress(inetHost, inetPort));
	}

	/**
	 * Connect a {@link Channel} to the remote peer.
	 */
	public ChannelFuture connect(InetAddress inetHost, int inetPort) {
		return connect(new InetSocketAddress(inetHost, inetPort));
	}

	/**
	 * Connect a {@link Channel} to the remote peer.
	 */
	public ChannelFuture connect(SocketAddress remoteAddress) {
		if (remoteAddress == null) {
			throw new NullPointerException("remoteAddress");
		}

		validate();
		return doConnect(remoteAddress, localAddress());
	}

	/**
	 * Connect a {@link Channel} to the remote peer.
	 */
	public ChannelFuture connect(SocketAddress remoteAddress,
			SocketAddress localAddress) {
		if (remoteAddress == null) {
			throw new NullPointerException("remoteAddress");
		}
		validate();
		return doConnect(remoteAddress, localAddress);
	}

	final ChannelFuture initAndRegister(EventLoop eventLoop) {
		if (eventLoop == null) {
			return super.initAndRegister();
		} else {
			final Channel channel = channelFactory().newChannel();
			try {
				init(channel);
			} catch (Throwable t) {
				channel.unsafe().closeForcibly();
				return channel.newFailedFuture(t);
			}

			ChannelFuture regFuture = eventLoop.register(channel);
			if (regFuture.cause() != null) {
				if (channel.isRegistered()) {
					channel.close();
				} else {
					channel.unsafe().closeForcibly();
				}
			}
			// If we are here and the promise is not failed, it's one of the following
			// cases:
			// 1) If we attempted registration from the event loop, the registration
			// has
			// been completed at this point.
			// i.e. It's safe to attempt bind() or connect() now beause the channel
			// has
			// been registered.
			// 2) If we attempted registration from the other thread, the registration
			// request has been successfully
			// added to the event loop's task queue for later execution.
			// i.e. It's safe to attempt bind() or connect() now:
			// because bind() or connect() will be executed *after* the scheduled
			// registration task is executed
			// because register(), bind(), and connect() are all bound to the same
			// thread.

			return regFuture;
		}
	}

	/**
	 * @see {@link #connect()}
	 */
	private ChannelFuture doConnect(final SocketAddress remoteAddress,
			final SocketAddress localAddress, EventLoop eventLoop) {
		final ChannelFuture regFuture = initAndRegister(eventLoop);
		final Channel channel = regFuture.channel();
		if (regFuture.cause() != null) {
			return regFuture;
		}

		final ChannelPromise promise = channel.newPromise();
		if (regFuture.isDone()) {
			doConnect0(regFuture, channel, remoteAddress, localAddress, promise);
		} else {
			regFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					doConnect0(regFuture, channel, remoteAddress, localAddress, promise);
				}
			});
		}
		return promise;
	}

	private ChannelFuture doConnect(final SocketAddress remoteAddress,
			final SocketAddress localAddress) {
		return doConnect(remoteAddress, localAddress, null);
	}

	private static void doConnect0(final ChannelFuture regFuture,
			final Channel channel, final SocketAddress remoteAddress,
			final SocketAddress localAddress, final ChannelPromise promise) {

		// This method is invoked before channelRegistered() is triggered. Give user
		// handlers a chance to set up
		// the pipeline in its channelRegistered() implementation.
		channel.eventLoop().execute(new Runnable() {
			@Override
			public void run() {
				if (regFuture.isSuccess()) {
					if (localAddress == null) {
						channel.connect(remoteAddress, promise);
					} else {
						channel.connect(remoteAddress, localAddress, promise);
					}
					promise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
				} else {
					promise.setFailure(regFuture.cause());
				}
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	void init(Channel channel) throws Exception {
		ChannelPipeline p = channel.pipeline();
		SSLEngine engine = sslContext.createSSLEngine();
		engine.setUseClientMode(true);
		SslHandler sslHandler = new SslHandler(engine);
		p.addFirst(SSL_HANDLER, sslHandler);
		if (handler() != null) {
			p.addLast(handler());
		}
		ChannelHandler[] handlers = initHandlers.get();
		if (handlers != null) {
			p.addLast(handlers);
		}
		final Map<ChannelOption<?>, Object> options = options();
		synchronized (options) {
			for (Entry<ChannelOption<?>, Object> e : options.entrySet()) {
				try {
					if (!channel.config().setOption((ChannelOption<Object>) e.getKey(),
							e.getValue())) {
						logger.warn("Unknown channel option: " + e);
					}
				} catch (Throwable t) {
					logger.warn("Failed to set a channel option: " + channel, t);
				}
			}
		}

		final Map<AttributeKey<?>, Object> attrs = attrs();
		synchronized (attrs) {
			for (Entry<AttributeKey<?>, Object> e : attrs.entrySet()) {
				channel.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
			}
		}
	}

	@Override
	public SSLBootstrap validate() {
		super.validate();
		return this;
	}

	@Override
	public SSLBootstrap clone() {
		return new SSLBootstrap(this);
	}

	@Override
	public String toString() {
		if (remoteAddress == null) {
			return super.toString();
		}

		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length() - 1);
		buf.append(", remoteAddress: ");
		buf.append(remoteAddress);
		buf.append(')');

		return buf.toString();
	}
}
