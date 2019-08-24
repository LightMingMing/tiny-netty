package tiny.netty.channel;

import java.util.concurrent.Future;

/**
 * 通道相关异步事件的结果
 * 1. 注册
 *
 * @author zhaomingming
 */
public interface ChannelFuture<V> extends Future<V> {
    Channel channel();
}
