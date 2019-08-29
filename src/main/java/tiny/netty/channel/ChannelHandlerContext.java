package tiny.netty.channel;

import tiny.netty.util.concurrent.EventExecutor;

import java.net.SocketAddress;

/**
 * 通道处理器上下文
 *
 * @author zhaomingming
 */
public interface ChannelHandlerContext extends ChannelInboundInvoker, ChannelOutboundInvoker {

    ChannelHandler handler();

    Channel channel();

    ChannelPipeline pipeline();

    EventExecutor executor();

    String name();

    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelFuture<?> bind(SocketAddress localAddress, ChannelFuture<?> promise);

    @Override
    ChannelFuture<?> close(ChannelFuture<?> promise);
}
