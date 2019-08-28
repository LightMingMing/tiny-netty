package tiny.netty.channel;

import java.net.SocketAddress;

/**
 * 通道出站处理器
 *
 * @author zhaomingming
 */
public interface ChannelOutboundHandler extends ChannelHandler {

    ChannelFuture<?> bind(SocketAddress localAddress, ChannelFuture<?> promise) throws Exception;

    ChannelFuture<?> close(ChannelFuture<?> promise) throws Exception;
}
