package com.wanda.epc.device;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private DeviceHandler deviceHandler;
    @Resource
    private NioClientHandler nioClientHandler;

    @RequestMapping("/test")
    public void test(String msg) {
        deviceHandler.sendMsg(msg);
    }
}
