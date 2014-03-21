package com.cyou.cpush.apns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import com.cyou.cpush.apns.core.NotificationsPromise;

public class ApnsInboundHandler extends ChannelInboundHandlerAdapter {
	private static final InternalLogger log = InternalLoggerFactory
			.getInstance(ApnsInboundHandler.class);

	private static final byte COMMAND_VALUE = 8;

	NotificationsPromise promise;

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("channel has been closed and deregister.");
		}
		super.channelInactive(ctx);
	}

	public ApnsInboundHandler(NotificationsPromise promise) {
		this.promise = promise;
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (msg instanceof ByteBuf) {
			ByteBuf in = (ByteBuf) msg;
			if (in.isReadable(6)) {
				byte command = in.readByte();
				if (command == COMMAND_VALUE) {
					byte code = in.readByte();
					int identifier = in.readInt();
					if (log.isDebugEnabled()) {
						log.debug(String.format(
								"read some error from APNS server, code: %d,  identifier: %d",
								code, identifier));
					}
					long progress = (((long) identifier) << 32) + code;
					promise.setProgress(progress, -1);
				}
			}
		}
		ctx.fireChannelRead(msg);
	}

}
