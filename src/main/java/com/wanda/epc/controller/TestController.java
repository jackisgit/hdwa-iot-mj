package com.wanda.epc.controller;

import com.wanda.epc.device.DeviceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class TestController {
    @Resource
    private DeviceHandler deviceHandler;


    @PostMapping("/receive")
    public void receive() throws Exception {
    }

}
