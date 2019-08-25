package tiny.netty.channel;

/**
 * 通道接口
 *
 * @author zhaomingming
 */
public interface Channel {

    // 通道所注册的事件循环器
    EventLoop eventLoop();

    // 状态值: 是否注册到事件循环器上
    boolean isRegistered();

    Unsafe unsafe();

    // 创建一个新的channelFuture
    ChannelFuture<?> newPromise();

    interface Unsafe {

        ChannelFuture<?> register(EventLoop eventLoop, ChannelFuture<?> promise);

        void deregister(ChannelFuture<?> promise);
    }
}
