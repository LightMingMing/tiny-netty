package tiny.netty.channel;

/**
 * 通道处理器方法默认实现
 *
 * @author zhaomingming
 */
public class ChannelHandlerAdapter implements ChannelHandler {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }
}
