package tiny.netty.channel;

import java.net.SocketAddress;

/**
 * 通道接口
 *
 * @author zhaomingming
 */
public interface Channel {

    // 通道所注册的事件循环器
    EventLoop eventLoop();

    // 通道管道
    ChannelPipeline pipeline();

    // 状态值: 是否注册到事件循环器上
    boolean isRegistered();

    // 状态值: 是否激活
    boolean isActive();

    Unsafe unsafe();

    ChannelFuture<?> closeFuture();

    // 创建一个新的channelFuture
    ChannelFuture<?> newPromise();

    ChannelFuture<?> deregister();

    ChannelFuture<?> deregister(ChannelFuture<?> promise);

    interface Unsafe {

        ChannelFuture<?> register(EventLoop eventLoop, ChannelFuture<?> promise);

        void deregister(ChannelFuture<?> promise);

        void close(ChannelFuture<?> promise);

        void bind(SocketAddress localAddress, ChannelFuture<?> promise);
    }
}
