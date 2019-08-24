package tiny.netty.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事件执行器选择工厂
 *
 * @author zhaomingming
 */
public class EventExecutorChooserFactory {

    public static final EventExecutorChooserFactory INSTANCE = new EventExecutorChooserFactory();

    private EventExecutorChooserFactory() {
    }

    private static boolean isPowerOfTwo(int i) {
        return (i & -i) == i;
    }

    public EventExecutorChooser newChooser(EventExecutor[] executors) {
        if (executors == null) {
            throw new IllegalArgumentException("executors is null");
        }
        return isPowerOfTwo(executors.length) ? new PowerOfTwoEventExecutorChooser(executors) : new GenericEventExecutorChooser(executors);
    }

    static abstract class AbstractEventExecutorChooser implements EventExecutorChooser {
        final EventExecutor[] executors;
        final AtomicInteger idx;

        AbstractEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
            this.idx = new AtomicInteger(executors.length);
        }
    }

    static class GenericEventExecutorChooser extends AbstractEventExecutorChooser {

        GenericEventExecutorChooser(EventExecutor[] eventExecutors) {
            super(eventExecutors);
        }

        @Override
        public EventExecutor next() {
            return executors[idx.getAndIncrement() % executors.length];
        }
    }

    static class PowerOfTwoEventExecutorChooser extends AbstractEventExecutorChooser {

        PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
            super(executors);
        }

        @Override
        public EventExecutor next() {
            return executors[idx.getAndIncrement() & (executors.length - 1)];
        }
    }
}
