package com.wanda.epc.device;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * netty编解码逻辑
 *
 * @author 孙率众
 */
@Component
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        //编码格式
        pipeline.addLast(new StringEncoder(Charset.forName("GBK")));
        //解码格式
        pipeline.addLast(new StringDecoder(Charset.forName("GBK")));
        //客户端的逻辑
        pipeline.addLast(new NioClientHandler());
    }
}
