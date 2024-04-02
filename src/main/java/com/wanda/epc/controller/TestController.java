package com.wanda.epc.controller;

import com.wanda.epc.device.DeviceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class TestController {
    @Resource
    private DeviceHandler deviceHandler;


    @GetMapping("/processData")
    public void processData() {
        deviceHandler.processData();
    }

    @GetMapping("/control/{doorId}/{value}")
    public void control(@PathVariable("doorId") String doorId, @PathVariable("value") String value) {
        deviceHandler.control(doorId, value);
    }

}
