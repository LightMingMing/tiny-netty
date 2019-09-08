package tiny.netty.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * 通道接口抽象实现
 *
 * @author zhaomingming
 */
public abstract class AbstractChannel implements Channel {
    ;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Unsafe unsafe;
    private final DefaultChannelPipeline pipeline;
    private volatile EventLoop eventLoop;
    private volatile boolean registered;
    private ChannelFuture<?> closeFuture = new CompletableChannelFuture<>(this);

    protected AbstractChannel() {
        unsafe = newUnsafe();
        pipeline = new DefaultChannelPipeline(this);
    }

    @Override
    public EventLoop eventLoop() {
        return eventLoop;
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
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

    @Override
    public ChannelFuture<?> closeFuture() {
        return closeFuture;
    }

    @Override
    public ChannelFuture<?> bind(SocketAddress localAddress) {
        return bind(localAddress, newPromise());
    }

    @Override
    public ChannelFuture<?> bind(SocketAddress localAddress, ChannelFuture<?> promise) {
        // callback outboundHandler
        pipeline.bind(localAddress, promise);
        return promise;
    }

    @Override
    public ChannelFuture<?> deregister() {
        return deregister(newPromise());
    }

    @Override
    public ChannelFuture<?> deregister(ChannelFuture<?> promise) {
        // callback outboundHandler
        pipeline.deregister(promise);
        return promise;
    }

    @Override
    public ChannelFuture<?> close() {
        // callback outboundHandler
        return close(newPromise());
    }

    @Override
    public ChannelFuture<?> close(ChannelFuture<?> promise) {
        // callback outboundHandler
        pipeline.close(promise);
        return promise;
    }

    protected abstract Unsafe newUnsafe();

    protected abstract void doRegister() throws Exception;

    protected abstract void doDeregister() throws Exception;

    protected abstract void doClose() throws Exception;

    protected abstract void doBind(SocketAddress localAddress) throws Exception;

    protected abstract class AbstractUnsafe implements Unsafe {

        boolean firstRegistration = true;

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
                boolean firstRegistered = firstRegistration;
                if (firstRegistration) {
                    firstRegistration = false;
                }
                registered = true;
                // 回调通道处理器的handlerAdd()方法
                pipeline.callHandlerAddedForAllHandlers();

                safeSetSuccess(promise);
                // 回调通道处理器的channelRegistered()方法
                pipeline.fireChannelRegistered();

                // 如果通道是激活的, 回调通道处理器的channelActive()方法
                // 同时如果通道曾经注册过(注册-注销-重新注册), 不回调channelActive()
                if (isActive() && firstRegistered) {
                    pipeline.fireChannelActive();
                }

            } catch (Throwable cause) {
                logger.warn("Failed to register.", cause);
                safeSetFailure(promise, cause);
            }

        }

        @Override
        public void deregister(ChannelFuture<?> promise) {
            if (!eventLoop.inEventLoop()) {
                eventLoop.execute(() -> deregister0(promise, false));
            } else {
                deregister0(promise, false);
            }
            // Avoid NPE
            // AbstractChannel.this.eventLoop = null;
        }

        private void deregister0(ChannelFuture<?> promise, boolean fireChannelActive) {
            try {
                if (!isRegistered()) {
                    promise.completeExceptionally(new IllegalStateException("unregistered to an eventLoop"));
                    return;
                }
                doDeregister();
            } catch (Throwable cause) {
                logger.warn("Unexpected exception occurred while de registering a channel.", cause);
            } finally {
                if (fireChannelActive) {
                    pipeline.fireChannelInactive();
                }
                if (registered) {
                    registered = false;
                    // 回调channelUnregistered()
                    pipeline.fireChannelUnregistered();
                }
                safeSetSuccess(promise);
            }

        }

        @Override
        public void close(ChannelFuture<?> promise) {
            assert eventLoop.inEventLoop();

            if (closeFuture.isDone()) {
                safeSetSuccess(promise);
                return;
            }
            close0(promise);
        }

        private void close0(ChannelFuture<?> promise) {
            // TODO netty里这部分有点复杂, 这里把核心部分弄上去了...
            // 通道关闭时还没有激活, 则不回调channelInactive()方法
            boolean wasActive = isActive();
            try {
                doClose();
                safeSetSuccess(promise);
                closeFuture.complete(null);
            } catch (Throwable cause) {
                logger.warn("Failed to close the channel", cause);
                safeSetFailure(promise, cause);
                closeFuture.complete(null);
            }
            // 在关闭通道前是激活状态, 关闭后是失活状态, 则回调channelInactive()方法
            // TODO 通道close之后, isActive()仍返回true, 这里似乎有些问题... 看下netty
            deregister0(newPromise(), wasActive && !isActive());
        }

        @Override
        public void bind(SocketAddress localAddress, ChannelFuture<?> promise) {
            if (!eventLoop.inEventLoop()) {
                eventLoop.execute(() -> bind0(localAddress, promise));
            } else {
                bind0(localAddress, promise);
            }
        }

        private void bind0(SocketAddress localAddress, ChannelFuture<?> promise) {
            if (isActive()) {
                safeSetFailure(promise, new IllegalStateException("bind already."));
            }
            try {
                doBind(localAddress);
                safeSetSuccess(promise);
                // 回调channelActive()方法
                pipeline.fireChannelActive();
                firstRegistration = false;
            } catch (Throwable cause) {
                logger.warn("Failed to bind the socketAddress : {}", localAddress);
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
