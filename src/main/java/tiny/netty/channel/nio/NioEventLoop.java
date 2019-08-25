package tiny.netty.channel.nio;

import tiny.netty.channel.AbstractEventLoop;
import tiny.netty.channel.ChannelFuture;
import tiny.netty.util.concurrent.EventExecutorGroup;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nio事件循环器
 *
 * @author zhaomingming
 */
public class NioEventLoop extends AbstractEventLoop {

    private final Selector selector;
    private final AtomicBoolean wakeup = new AtomicBoolean(false);

    public NioEventLoop(EventExecutorGroup parent, Executor executor, SelectorProvider selectorProvider) {
        super(parent, executor);
        try {
            selector = selectorProvider.openSelector();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open a selector");
        }
    }

    @Override
    public ChannelFuture<?> register(ChannelFuture<?> promise) {
        return promise.channel().unsafe().register(this, promise);
    }

    Selector selector() {
        return selector;
    }

    @Override
    protected void run() {
        // selector.wakeup() 使当前阻塞线程的selector.select()操作立即返回 如果没有阻塞的选择操作那么下次select()方法会立即返回
        // selector.selectNow() 会清除掉wakeup()方法的影响
        for (; ; ) {
            try {
                // TODO netty 这一段代码是真的难看懂.....
                // 以下根据个人理解做了简化, 可能有问题但影响应该不大
                if (hasTask()) {
                    selector.selectNow(this::processSelectedKey);
                } else {
                    // TODO 调度任务
                    // 1. 如果异步 select.wakeup()发生在这里 ok
                    wakeup.set(false);
                    // 2. 如果异步 select.wakeup()发生在这里, 可能在下次select()时, 不会立即返回
                    // 因此需要在选择后再进行一次判断, 如果是true, 调用selector.wakeup()方法
                    selector.select(this::processSelectedKey, 1000);
                    // 3. 如果异步 select.wakeup()发生在这里, 应该会多一次selector.wakeup()操作
                }
                // TODO 为什么selectNow之后也要恢复wakeup状态 这里不是很懂
                if (wakeup.get()) {
                    selector.wakeup();
                }
                // 4. 如果异步 select.wakeup()发生在这里, ok
            } catch (Throwable cause) {
                logger.warn("Raised an exception in select", cause);
            } finally {
                runAllTasks();
            }
            if (isShuttingDown()) {
                if (confirmShutdown()) {
                    closeAll();
                    return;
                }
            }
        }
    }

    private void processSelectedKey(SelectionKey key) {
        // TODO 处理选择的Key
    }

    private void closeAll() {
        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractNioChannel> channels = new ArrayList<>();
        for (SelectionKey key : keys) {
            key.cancel();
            channels.add((AbstractNioChannel) key.attachment());
        }
        channels.forEach(channel -> channel.unsafe().deregister(channel.newPromise()));
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && wakeup.compareAndSet(false, true)) {
            selector.wakeup();
        }
    }

    @Override
    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            logger.warn("Failed to close a select");
        }
    }
}
