package tiny.netty.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 事件执行器
 *
 * @author zhaomingming
 */
public interface EventExecutor extends EventExecutorGroup {

    EventExecutorGroup parent();

    @Override
    EventExecutor next();

    boolean inEventLoop();

    boolean inEventLoop(Thread thread);

    @Override
    boolean isShuttingDown();

    @Override
    boolean isTerminated();

    @Override
    CompletableFuture<?> shutdownGracefully();

    @Override
    CompletableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit timeUnit);

    @Override
    CompletableFuture<?> terminationFuture();

    @Override
    boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException;
}
