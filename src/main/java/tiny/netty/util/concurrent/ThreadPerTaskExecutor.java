package tiny.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * 每一个任务生成一个线程
 *
 * @author zhaomingming
 */
public class ThreadPerTaskExecutor implements Executor {

    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new IllegalArgumentException("threadFactory is null");
        }
        this.threadFactory = threadFactory;
    }

    @Override
    public void execute(Runnable task) {
        threadFactory.newThread(task).start();
    }
}
