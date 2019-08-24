package tiny.netty.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多线程事件执行组
 *
 * @author zhaomingming
 */
public abstract class MultiThreadEventExecutorGroup extends AbstractEventExecutorGroup {

    private final CompletableFuture<?> terminationFuture = new CompletableFuture<>();
    private final Collection<EventExecutor> readonlyChildren;
    private final EventExecutor[] children;
    private final EventExecutorChooser chooser;

    protected MultiThreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        this(nThreads, new ThreadPerTaskExecutor(threadFactory), args);
    }

    protected MultiThreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        this.children = new EventExecutor[nThreads];
        AtomicInteger terminatedChildren = new AtomicInteger();
        for (int i = 0; i < nThreads; i++) {
            children[i] = newChild(executor, args);
            children[i].terminationFuture().thenRun(() -> {
                if (terminatedChildren.incrementAndGet() == nThreads) {
                    terminationFuture.complete(null);
                }
            });
        }
        this.readonlyChildren = Set.of(children);
        this.chooser = EventExecutorChooserFactory.INSTANCE.newChooser(children);
    }

    protected abstract EventExecutor newChild(Executor executor, Object... args);

    @Override
    public EventExecutor next() {
        return chooser.next();
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return readonlyChildren.iterator();
    }

    @Override
    public boolean isShuttingDown() {
        return readonlyChildren.stream().allMatch(EventExecutor::isShuttingDown);
    }

    @Override
    public boolean isTerminated() {
        return readonlyChildren.stream().allMatch(EventExecutor::isTerminated);
    }

    @Override
    public CompletableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit timeUnit) {
        readonlyChildren.forEach(child -> child.shutdownGracefully(quietPeriod, timeout, timeUnit));
        return terminationFuture;
    }

    @Override
    public CompletableFuture<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout: " + timeout + " (expected > 0)");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit is null");
        }
        long deadlineTime = System.nanoTime() + timeUnit.toNanos(timeout);
        for (EventExecutor child : children) {
            long availableTime = deadlineTime - System.nanoTime();
            if (!child.awaitTermination(availableTime, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void execute(Runnable task) {
        next().execute(task);
    }
}
