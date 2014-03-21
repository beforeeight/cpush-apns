package com.cyou.cpush.apns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.cyou.cpush.apns.notification.Notification;
import com.cyou.cpush.apns.util.HexStrUtil;

public class ApnsOutboundHandler extends MessageToByteEncoder<Notification> {
	public ApnsOutboundHandler() {

	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Notification msgContext,
			ByteBuf out) throws Exception {
		int identifier = msgContext.getIdentifier();
		byte[] token = HexStrUtil.hexStr2Bytes(msgContext.getDevice().getToken());
		byte[] content = msgContext.getPayload().toJson().getBytes();
		out.writeByte(1);
		out.writeInt(identifier).writeInt(0);
		/* token */
		out.writeShort(token.length);
		out.writeBytes(token);
		/* Payload */
		out.writeShort(content.length);
		out.writeBytes(content);
	}

}
