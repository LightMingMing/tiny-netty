package tiny.netty.channel;

import java.net.SocketAddress;

/**
 * 通道出站处理器
 *
 * @author zhaomingming
 */
public interface ChannelOutboundHandler extends ChannelHandler {

    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelFuture<?> promise) throws Exception;

    void deregister(ChannelHandlerContext ctx, ChannelFuture<?> promise) throws Exception;

    void close(ChannelHandlerContext ctx, ChannelFuture<?> promise) throws Exception;
}
