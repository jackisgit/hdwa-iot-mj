package com.wanda.epc.device;


import com.wanda.epc.param.DeviceMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

/**
 * @author 孙率众
 */
@Slf4j
@Component
@EnableScheduling
public class DeviceHandler extends BaseDevice {

    /**
     * 重连频率，单位：秒
     */
    private static final Integer RECONNECT_SECONDS = 20;
    /**
     * 查询全部设备指令
     */
    private static final String queryAll = "N3000 -user abc -password 123  -GetAllDoorStatus";

    /**
     * 开门指令
     */
    private static final String openDoor = "N3000 -USER \"abc\" -PASSWORD \"123\" -OPEN \"{0}\"";
    @Resource
    CommonDevice commonDevice;
    @Value("${tcp.serverIP}")
    private String serverHost;
    @Value("${tcp.port}")
    private Integer serverPort;
    @Resource
    private ClientChannelInitializer nettyClientHandlerInitializer;
    /**
     * 线程组，用于客户端对服务端的链接、数据读写
     */
    private EventLoopGroup eventGroup = new NioEventLoopGroup();
    /**
     * Netty Client Channel
     */
    private volatile Channel channel;

    @Scheduled(cron = "0/10 * * * * ?")
    public void sendHeartMessage() {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(queryAll);
        }
    }

    public void sendMsg(String msg) {
        channel.writeAndFlush(msg);
    }

    @Override
    public void sendMessage(DeviceMessage dm) {
        commonDevice.sendMessage(dm);
    }

    @Override
    public boolean processData() {
        return false;
    }

    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) {
        commonDevice.feedback(message);
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        if (ObjectUtils.isNotEmpty(deviceMessage) && StringUtils.isNotBlank(deviceMessage.getOutParamId()) &&
                deviceMessage.getOutParamId().endsWith("_equipSwitchSet")) {
            String outParamId = deviceMessage.getOutParamId();
            if (redisUtil.hasKey(outParamId)) {
                return;
            }
            redisUtil.set(outParamId, "0", 5);
            if ("0.0".equals(value)) {
                return;
            }
            try {
                String[] split = outParamId.split("_");
                //分区
                String door = split[0];
                String order = MessageFormat.format(openDoor, door);
                channel.writeAndFlush(order);
            } catch (Exception e) {
                log.error("防盗报警控制命令下发失败", e);
            }
        }
    }

    @Override
    public boolean processData(String... obj) {
        return false;
    }

    /**
     * 启动 Netty Client
     */
    @PostConstruct
    public void start() throws InterruptedException {
        // 创建 Bootstrap 对象，用于 Netty Client 启动
        Bootstrap bootstrap = new Bootstrap();
        // 设置 Bootstrap 的各种属性。
        bootstrap.group(eventGroup) // 设置一个 EventLoopGroup 对象
                .channel(NioSocketChannel.class)  // 指定 Channel 为客户端 NioSocketChannel
                .remoteAddress(serverHost, serverPort) // 指定链接服务器的地址
                .option(ChannelOption.SO_KEEPALIVE, true) // TCP Keepalive 机制，实现 TCP 层级的心跳保活功能
                .option(ChannelOption.TCP_NODELAY, true) // 允许较小的数据包的发送，降低延迟
                .handler(nettyClientHandlerInitializer);
        // 链接服务器，并异步等待成功，即启动客户端
        bootstrap.connect().addListener((ChannelFutureListener) future -> {
            // 连接失败
            if (!future.isSuccess()) {
                log.error("[start][Netty Client 连接服务器({}:{}) 失败]", serverHost, serverPort);
                reconnect();
                return;
            }
            // 连接成功
            channel = future.channel();
        });
    }

    @PreDestroy
    public void preDestroy() {
        eventGroup.shutdownGracefully();
    }

    public void reconnect() {
        eventGroup.schedule(() -> {
            log.info("[reconnect][开始重连]");
            try {
                start();
            } catch (InterruptedException e) {
                log.error("[reconnect][重连失败]", e);
            }
        }, RECONNECT_SECONDS, TimeUnit.SECONDS);
        log.info("[reconnect][{} 秒后将发起重连]", RECONNECT_SECONDS);
    }


}
