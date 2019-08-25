package tiny.netty.channel;

import java.util.concurrent.CompletableFuture;

/**
 * ChannelFuture实现类
 *
 * @author zhaomingming
 * @see java.util.concurrent.CompletableFuture
 */
public class CompletableChannelFuture<V> extends CompletableFuture<V> implements ChannelFuture<V> {

    private final Channel channel;

    public CompletableChannelFuture(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel channel() {
        return channel;
    }
}
