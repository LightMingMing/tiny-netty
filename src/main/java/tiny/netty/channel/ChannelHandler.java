package tiny.netty.channel;

/**
 * 通道处理器
 *
 * @author zhaomingming
 */
public interface ChannelHandler {

    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;
}
