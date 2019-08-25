package tiny.netty.channel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * 通道相关的异步事件的结果及其后置处理
 *
 * @author zhaomingming
 */
public interface ChannelFuture<V> extends Future<V> {
    Channel channel();

    boolean complete(V value);

    boolean completeExceptionally(Throwable ex);

    CompletableFuture<Void> thenRun(Runnable action);

    CompletableFuture<Void> thenRunAsync(Runnable action);
}
