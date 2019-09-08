package tiny.netty.channel;

import java.net.SocketAddress;

/**
 * 出站方法调用
 *
 * @author zhaomingming
 */
public interface ChannelOutboundInvoker {

    ChannelFuture<?> bind(SocketAddress localAddress);

    ChannelFuture<?> bind(SocketAddress localAddress, ChannelFuture<?> promise);

    ChannelFuture<?> deregister();

    ChannelFuture<?> deregister(ChannelFuture<?> promise);

    ChannelFuture<?> close();

    ChannelFuture<?> close(ChannelFuture<?> promise);

    ChannelFuture<?> newPromise();
}
