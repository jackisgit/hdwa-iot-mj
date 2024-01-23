package com.wanda.epc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @program: iot_epc
 * @description: 汇纳客流采集
 * @author: liuruishuo
 * @create: 2022-11-08 17:07
 **/
@Configuration
@EnableScheduling
public class CommonTask {

    @Autowired
    private DaHuaDoorDevice device;

    @Scheduled(cron = "${epc.cron:0 0/1 * * * ?}")
    public boolean processData() throws Exception {
        return device.processData();
    }

}
