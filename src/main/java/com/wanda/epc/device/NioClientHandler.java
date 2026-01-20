package com.wanda.epc.device;

import com.wanda.epc.common.SpringUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * @author 孙率众
 */
@Slf4j
@Component
public class NioClientHandler extends SimpleChannelInboundHandler<String> {

    public static final String ONLINE_STATUS = "_onlineStatus";
    public static final String FAULT_STATUS = "_faultStatus";
    public static final String OPEN_STATUS = "_openStatus";

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) {
        log.info("接收服务器消息:{}", msg);
        try {
            final String[] split = msg.split("\n");
            DeviceHandler deviceHandler = SpringUtil.getBean(DeviceHandler.class);
            for (String string : split) {
                final String[] data = string.split(",");
                if (data.length != 3) {
                    continue;
                }
                //故障状态
                String gzzt = "0";
                //在线状态
                String zxzt = "1";
                //开关状态
                String kgzt = "0";
                String data0 = data[0].replace(" ", "").replace("\r", "");
                String data2 = data[2].replace(" ", "").replace("\r", "");
                if ("0".equals(data2)) {
                    gzzt = "1";
                } else if ("1".equals(data2)) {
                    kgzt = "1";
                }
                log.info("设备:{},在线状态:{},故障状态:{},开关状态:{},判断值:{}", data0,zxzt, gzzt, kgzt, data2);
                deviceHandler.sendMsg(data0 + ONLINE_STATUS, zxzt);
                deviceHandler.sendMsg(data0 + FAULT_STATUS, gzzt);
                deviceHandler.sendMsg(data0 + OPEN_STATUS, kgzt);
            }
        } catch (Exception e) {
            log.error("客户端接收消息异常", e);
        }
    }

    /**
     * 连接关闭!
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = socket.getAddress().getHostAddress();
        int port = socket.getPort();
        log.error("{}连接关闭！", ip + ":" + port);
        final DeviceHandler deviceHandler = SpringUtil.getBean(DeviceHandler.class);
        deviceHandler.reconnect();
        super.channelInactive(ctx);
    }

    /**
     * 客户端主动连接服务端
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        try {
            super.channelActive(ctx);
        } catch (Exception e) {
            log.error("客户端连接异常", e);
        }
    }

    /**
     * 发生异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        try {
            super.userEventTriggered(ctx, evt);
        } catch (Exception e) {
            log.error("客户端异常", e);
        }
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                ctx.close();
            }
        }
    }
}
