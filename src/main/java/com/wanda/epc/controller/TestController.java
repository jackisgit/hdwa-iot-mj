package com.wanda.epc.controller;

import com.wanda.epc.device.MjHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class TestController {
    @Resource
    private MjHandler mjHandler;

    @RequestMapping("/collect")
    public void collect() throws Exception {
        mjHandler.processData();
    }

    @RequestMapping("/control/{deviceId}/{deviceType}/{zoneId}/{armingAction}")
    public void control(@PathVariable int deviceId, @PathVariable int deviceType, @PathVariable int zoneId, @PathVariable int armingAction) throws Exception {
    }
}
