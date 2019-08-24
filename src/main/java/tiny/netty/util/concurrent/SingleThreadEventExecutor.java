package tiny.netty.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 单线程事件执行器
 *
 * @author zhaomingming
 */
public abstract class SingleThreadEventExecutor extends AbstractEventExecutor {

    private static final int ST_NOT_STARTED = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_SHUTTING_DOWN = 3;
    private static final int ST_TERMINATED = 4;
    private static VarHandle ST_HANDLE;

    static {
        MethodHandles.Lookup l = MethodHandles.lookup();
        try {
            ST_HANDLE = l.findVarHandle(SingleThreadEventExecutor.class, "state", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CompletableFuture<?> terminationFuture = new CompletableFuture<>();
    private final EventExecutorGroup parent;
    private final Executor executor;
    private final Queue<Runnable> taskQueue;
    private final Semaphore awaitTerminationLock = new Semaphore(0);
    private volatile int state = ST_NOT_STARTED;
    private volatile Thread thread;
    private volatile long gracefullyShutdownQuietPeriod;
    private volatile long gracefullyShutdownTimeout;
    private long lastExecutionTime;
    private long gracefullyShutdownStartTime;

    protected SingleThreadEventExecutor(EventExecutorGroup parent, ThreadFactory factory) {
        this(parent, new ThreadPerTaskExecutor(factory));
    }

    protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        this.parent = parent;
        this.executor = executor;
        taskQueue = new LinkedBlockingQueue<>(16);
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        assert thread != null;
        return this.thread == thread;
    }

    @Override
    public EventExecutorGroup parent() {
        return parent;
    }

    @Override
    public void execute(Runnable task) {
        if (task == null)
            throw new IllegalArgumentException("task is null");
        if (isShuttingDown() || !taskQueue.offer(task)) {
            // TODO reject execution policy
            logger.warn("Reject execution, executor is shutting down or taskQueue is full. Task: {}", task);
            return;
        }
        startThread();
    }

    private void startThread() {
        if (state == ST_NOT_STARTED) {
            if (ST_HANDLE.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
                try {
                    doStartThread();
                } catch (Throwable cause) {
                    ST_HANDLE.compareAndSet(this, ST_STARTED, ST_NOT_STARTED);
                }
            }
        }
    }

    private void doStartThread() {
        assert thread == null;
        executor.execute(() -> {
            thread = Thread.currentThread();
            try {
                lastExecutionTime = naoTime();
                this.run();
            } catch (Throwable cause) {
                logger.warn("Unexpected exception from an event executor:", cause);
            } finally {
                for (; ; ) {
                    int oldState = state;
                    if (oldState >= ST_SHUTTING_DOWN || ST_HANDLE.compareAndSet(this, oldState, ST_SHUTTING_DOWN)) {
                        break;
                    }
                }
                try {
                    for (; ; ) {
                        if (confirmShutdown()) {
                            break;
                        }
                    }
                } finally {
                    try {
                        cleanup();
                    } finally {
                        ST_HANDLE.set(this, ST_TERMINATED);
                        awaitTerminationLock.release();
                        terminationFuture.complete(null);
                    }
                }
            }
        });
    }

    protected boolean confirmShutdown() {
        if (!isShuttingDown()) {
            return false;
        }
        if (!inEventLoop()) {
            throw new IllegalStateException("must be invoked from an event executor");
        }
        if (gracefullyShutdownStartTime == 0) {
            gracefullyShutdownStartTime = naoTime();
        }

        // 执行队列中任务
        if (runAllTasks()) {
            if (gracefullyShutdownQuietPeriod == 0) {
                return true;
            }
            wakeup(true);
            return false;
        }

        long naoTime = naoTime();
        // 是否超时
        if (naoTime - gracefullyShutdownStartTime > gracefullyShutdownTimeout) {
            return true;
        }
        // quietPeriod时间内有没有新任务
        if (naoTime - lastExecutionTime < gracefullyShutdownQuietPeriod) {
            wakeup(true);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
            return false;
        }
        return true;
    }

    protected boolean hasTask() {
        return !taskQueue.isEmpty();
    }

    protected boolean runAllTasks() {
        assert inEventLoop();
        // TODO 加上调度任务
        Runnable task = taskQueue.poll();
        if (task == null) {
            return false;
        }
        while (task != null) {
            safeExecution(task);
            lastExecutionTime = naoTime();
            task = taskQueue.poll();
        }
        return true;
    }

    protected void safeExecution(Runnable task) {
        try {
            task.run();
        } catch (Throwable cause) {
            logger.warn("A task raised an exception. Task: {}", task, cause);
        }
    }

    protected abstract void run();

    protected abstract void cleanup();

    protected void wakeup(boolean inEventLoop) {
    }

    protected long naoTime() {
        return System.nanoTime();
    }

    @Override
    public CompletableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit timeUnit) {
        if (quietPeriod < 0) {
            throw new IllegalArgumentException("quietShutdownPeriod: " + quietPeriod + " (expected > 0)");
        }
        if (timeout < quietPeriod) {
            throw new IllegalArgumentException("timeout: " + timeout + " (expected >= quietShutdownPeriod (" + quietPeriod + ")");
        }
        if (isShuttingDown()) {
            return terminationFuture;
        }
        int oldState;
        for (; ; ) {
            if (isShuttingDown()) {
                return terminationFuture;
            }
            oldState = state;
            int newState = Math.max(oldState, ST_SHUTTING_DOWN);
            if (ST_HANDLE.compareAndSet(this, oldState, newState)) {
                break;
            }
        }

        this.gracefullyShutdownQuietPeriod = timeUnit.toNanos(quietPeriod);
        this.gracefullyShutdownTimeout = timeUnit.toNanos(timeout);
        if (!ensureThreadStarted(oldState)) {
            return terminationFuture;
        }
        wakeup(inEventLoop());
        return terminationFuture;
    }

    private boolean ensureThreadStarted(int oldState) {
        // 为什么不直接使用 thread == null 判断?
        // 线程的创建需要一定开销, 由于thread的赋值发生在线程创建后, 使用thread判断的话, 可能会导致创建多个工作线程
        // 因此要使用状态值, 能够更准确的知道是不是有其它线程在创建任务
        if (oldState == ST_NOT_STARTED) {
            try {
                doStartThread();
            } catch (Throwable cause) {
                ST_HANDLE.set(this, ST_TERMINATED);
                terminationFuture.completeExceptionally(cause);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isShuttingDown() {
        return state >= ST_SHUTTING_DOWN;
    }

    @Override
    public boolean isTerminated() {
        return state == ST_TERMINATED;
    }

    @Override
    public CompletableFuture<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit is null");
        }
        if (awaitTerminationLock.tryAcquire(1, timeout, timeUnit)) {
            awaitTerminationLock.release();
        }
        return isTerminated();
    }
}
