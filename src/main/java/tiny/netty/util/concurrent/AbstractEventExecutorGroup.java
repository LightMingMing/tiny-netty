package tiny.netty.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static tiny.netty.util.concurrent.AbstractEventExecutor.DEFAULT_QUIET_SHUTDOWN_PERIOD;
import static tiny.netty.util.concurrent.AbstractEventExecutor.DEFAULT_SHUTDOWN_TIMEOUT;

/**
 * 事件执行组抽象实现类
 *
 * @author zhaomingming
 */
public abstract class AbstractEventExecutorGroup implements EventExecutorGroup {

    @Override
    public CompletableFuture<?> shutdownGracefully() {
        return shutdownGracefully(DEFAULT_QUIET_SHUTDOWN_PERIOD, DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
    }
}
