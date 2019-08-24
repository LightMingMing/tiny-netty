package tiny.netty.channel;

import tiny.netty.util.concurrent.EventExecutor;

/**
 * 通道接口
 *
 * @author zhaomingming
 */
public interface Channel {

    // 通道所注册的事件执行器
    EventExecutor executor();

    // 状态值: 是否注册到事件循环器上
    boolean isRegistered();

    // 创建一个新的channelFuture
    ChannelFuture<?> newPromise();
}
