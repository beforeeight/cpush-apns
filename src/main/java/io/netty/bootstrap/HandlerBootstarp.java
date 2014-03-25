package io.netty.bootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;

public interface HandlerBootstarp<T extends ChannelHandler> {

	public ChannelFuture connect(T... handlers);

}
