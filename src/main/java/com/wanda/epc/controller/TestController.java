package com.wanda.epc.controller;

import com.alibaba.fastjson.JSONObject;
import com.wanda.epc.DTO.AlarmDTO;
import com.wanda.epc.device.DeviceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class TestController {
    @Resource
    private DeviceHandler deviceHandler;


    @PostMapping("/receive")
    public void receive(@RequestBody AlarmDTO alarmDTO) throws Exception {
        log.info("收到数据:{}", JSONObject.toJSONString(alarmDTO));
        deviceHandler.receive(alarmDTO);
    }

}
