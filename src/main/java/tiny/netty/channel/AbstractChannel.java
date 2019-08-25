package tiny.netty.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通道接口抽象实现
 *
 * @author zhaomingming
 */
public abstract class AbstractChannel implements Channel {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Unsafe unsafe;
    private volatile EventLoop eventLoop;
    private volatile boolean registered;

    protected AbstractChannel() {
        unsafe = newUnsafe();
    }

    @Override
    public EventLoop eventLoop() {
        return eventLoop;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public ChannelFuture<?> newPromise() {
        return new CompletableChannelFuture(this);
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract Unsafe newUnsafe();

    protected abstract void doRegister() throws Exception;

    protected abstract class AbstractUnsafe implements Unsafe {
        @Override
        public ChannelFuture<?> register(EventLoop eventLoop, ChannelFuture<?> promise) {
            AbstractChannel.this.eventLoop = eventLoop;
            if (!eventLoop.inEventLoop()) {
                // 事件循环器串行处理
                // TODO 考虑任务没有接收情况下, 将通到关闭, 并且promise设为失败
                eventLoop.execute(() -> register0(promise));
            } else {
                register0(promise);
            }
            return promise;
        }

        private void register0(ChannelFuture<?> promise) {
            if (isRegistered()) {
                promise.completeExceptionally(new IllegalStateException("registered to an evenLoop already"));
                return;
            }
            try {
                // 具体实现交给子类, 如NioChannel, EpollChannel, KQueueChannel
                // 而公共部分, 也就是回调, 则在这里实现.
                doRegister();
                registered = true;
                // TODO 回调通道处理器的handlerAdd()方法

                safeSetSuccess(promise);
                // TODO 回调通道处理器的channelRegistered()方法

                // TODO 如果通道是激活的, 回调通道处理器的channelActive()方法
                //  同时如果通道曾经注册过(注册-注销-重新注册), 不回调channelActive()

            } catch (Throwable cause) {
                logger.warn("Failed to register.", cause);
                safeSetFailure(promise, cause);
            }

        }

        private void safeSetSuccess(ChannelFuture<?> promise) {
            if (!promise.complete(null)) {
                logger.warn("Failed to mark a promise as success because it is done already: {}", promise);
            }
        }

        private void safeSetFailure(ChannelFuture<?> promise, Throwable cause) {
            if (!promise.completeExceptionally(cause)) {
                logger.warn("Failed to make a promise as failure because it is done already: {}", promise, cause);
            }
        }
    }
}
