package tiny.netty.util.concurrent;

import org.junit.Test;
import tiny.netty.util.concurrent.SingleThreadEventExecutorTest.SimpleEventExecutor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 测试
 *
 * @author zhaomingming
 */
public class MultiThreadEventExecutorGroupTest {

    @Test
    public void testShutdown() throws InterruptedException {
        EventExecutorGroup group = new SimpleEventExecutorGroup(3);

        EventExecutor first = group.next();
        EventExecutor second = group.next();
        EventExecutor third = group.next();
        assertThat(first).isEqualTo(group.next());
        assertThat(second).isEqualTo(group.next());
        assertThat(third).isEqualTo(group.next());

        group.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        group.awaitTermination(1050, TimeUnit.MILLISECONDS);

        assertThat(first.isTerminated()).isTrue();
        assertThat(second.isTerminated()).isTrue();
        assertThat(third.isTerminated()).isTrue();
        assertThat(group.isTerminated()).isTrue();
        assertThat(group.terminationFuture()).isCompleted();
    }

    static class SimpleEventExecutorGroup extends MultiThreadEventExecutorGroup {

        SimpleEventExecutorGroup(int nThreads) {
            super(nThreads, new DefaultThreadFactory("test"));
        }

        @Override
        protected EventExecutor newChild(EventExecutorGroup parent, ThreadFactory threadFactory) {
            return new SimpleEventExecutor(this, threadFactory);
        }

    }
}