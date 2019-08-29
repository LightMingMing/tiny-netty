package tiny.netty.channel;

/**
 * 通道管道, 维持着一组通道处理器
 * <p>每个添加的{@link ChannelHandler}被会被封装为{@link ChannelHandlerContext},以双向链表的形式存储于管道中</p>
 *
 * @author zhaomingming
 */
public interface ChannelPipeline extends ChannelInboundInvoker, ChannelOutboundInvoker {

    Channel channel();

    // 添加至管道头部
    ChannelPipeline addFirst(String name, ChannelHandler handler);

    // 添加至管道尾部
    ChannelPipeline addLast(String name, ChannelHandler handler);

    // 删除通道处理器
    ChannelPipeline remove(String name);

    // 删除通道处理器
    ChannelPipeline remove(ChannelHandler handler);

    // 获取通道处理器上下文
    ChannelHandlerContext context(String name);

    // 获取通道处理器上下文
    ChannelHandlerContext context(ChannelHandler handler);

    // 获取通道处理器
    ChannelHandler get(String name);

    // 获取通道处理器
    <T extends ChannelHandler> T get(Class<T> handlerType);

    @Override
    ChannelPipeline fireChannelRegistered();

    @Override
    ChannelPipeline fireChannelUnregistered();

    @Override
    ChannelPipeline fireChannelActive();

    @Override
    ChannelPipeline fireChannelInactive();

    @Override
    ChannelPipeline fireExceptionCaught(Throwable cause);

}
