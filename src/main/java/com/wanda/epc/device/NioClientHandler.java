package com.wanda.epc.device;

import com.wanda.epc.common.SpringUtil;
import com.wanda.epc.param.DeviceMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author 孙率众
 */
@Slf4j
@Component
public class NioClientHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) {
        log.info("接收服务器消息:{}", msg);
        try {
            final DeviceHandler deviceHandler = SpringUtil.getBean(DeviceHandler.class);
            final String[] strings = msg.split(":\n");
            final String[] split = strings[1].split("\n");
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
                if (data[2] == "0") {
                    gzzt = "1";
                } else if (data[2] == "1") {
                    kgzt = "1";
                } else if (data[2] == "2") {
                    kgzt = "0";
                }
                List<DeviceMessage> onlienStautsList = deviceHandler.deviceParamListMap.get(data[0] + "_onlineStatus");
                if (!CollectionUtils.isEmpty(onlienStautsList)) {
                    for (DeviceMessage deviceMessage : onlienStautsList) {
                        deviceMessage.setValue(zxzt);
                        deviceHandler.sendMessage(deviceMessage);
                    }
                }
                List<DeviceMessage> faultStatusList = deviceHandler.deviceParamListMap.get(data[0] + "_faultStatus");
                if (!CollectionUtils.isEmpty(faultStatusList)) {
                    for (DeviceMessage deviceMessage : faultStatusList) {
                        deviceMessage.setValue(gzzt);
                        deviceHandler.sendMessage(deviceMessage);
                    }
                }
                List<DeviceMessage> openStatusList = deviceHandler.deviceParamListMap.get(data[0] + "_openStatus");
                if (!CollectionUtils.isEmpty(openStatusList)) {
                    for (DeviceMessage deviceMessage : openStatusList) {
                        deviceMessage.setValue(kgzt);
                        deviceHandler.sendMessage(deviceMessage);
                    }
                }
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
