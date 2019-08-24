package tiny.netty.util.concurrent;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 事件执行组: 一组事件执行器
 *
 * @author zhaomingming
 */
public interface EventExecutorGroup extends Executor, Iterable<EventExecutor> {

    EventExecutor next();

    @Override
    Iterator<EventExecutor> iterator();

    boolean isShuttingDown();

    boolean isTerminated();

    CompletableFuture<?> shutdownGracefully();

    CompletableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit timeUnit);

    CompletableFuture<?> terminationFuture();

    boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException;
}
