package com.wanda.epc.device;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private DeviceHandler deviceHandler;

    @RequestMapping("/test")
    public void test(){
        deviceHandler.sendMsg("孙率众zzz");
    }
}
