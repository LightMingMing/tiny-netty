package tiny.netty.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 事件执行器抽象实现类
 *
 * @author zhaomingming
 */
public abstract class AbstractEventExecutor implements EventExecutor {

    static final long DEFAULT_QUIET_SHUTDOWN_PERIOD = 2;
    static final long DEFAULT_SHUTDOWN_TIMEOUT = 15;

    private final Collection<EventExecutor> self = List.of(this);

    @Override
    public EventExecutor next() {
        return this;
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return self.iterator();
    }

    @Override
    public boolean inEventLoop() {
        return inEventLoop(Thread.currentThread());
    }

    @Override
    public CompletableFuture<?> shutdownGracefully() {
        return shutdownGracefully(DEFAULT_QUIET_SHUTDOWN_PERIOD, DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
    }
}
