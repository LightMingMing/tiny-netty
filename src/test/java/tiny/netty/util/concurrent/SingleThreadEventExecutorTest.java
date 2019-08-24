package tiny.netty.util.concurrent;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 测试
 *
 * @author zhaomingming
 */
public class SingleThreadEventExecutorTest {

    @Test
    public void testShutdown() throws InterruptedException {
        SimpleSingleThreadEventExecutor executor = new SimpleSingleThreadEventExecutor();

        executor.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        assertThat(executor.isShuttingDown()).isTrue();

        executor.awaitTermination(1050, TimeUnit.MILLISECONDS);
        assertThat(executor.isTerminated()).isTrue();
        assertThat(executor.isCleanup()).isTrue();
    }

    @Test
    public void testTaskExecute() throws InterruptedException {
        SimpleSingleThreadEventExecutor executor = new SimpleSingleThreadEventExecutor();

        CompletableFuture<?> taskFuture = new CompletableFuture<>();
        executor.execute(() -> taskFuture.complete(null));

        executor.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        assertThat(executor.isShuttingDown()).isTrue();

        executor.awaitTermination(1050, TimeUnit.MILLISECONDS);
        assertThat(taskFuture.isDone()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
        assertThat(executor.isCleanup()).isTrue();
    }

    static class SimpleSingleThreadEventExecutor extends SingleThreadEventExecutor {

        volatile boolean cleanup = false;

        private SimpleSingleThreadEventExecutor() {
            super(null, new DefaultThreadFactory("test"));
        }

        @Override
        protected void run() {
            for (; ; ) {
                runAllTasks();
                if (isShuttingDown()) {
                    if (confirmShutdown()) {
                        break;
                    }
                }
            }
        }

        @Override
        protected void cleanup() {
            cleanup = true;
        }

        public boolean isCleanup() {
            return cleanup;
        }
    }

}