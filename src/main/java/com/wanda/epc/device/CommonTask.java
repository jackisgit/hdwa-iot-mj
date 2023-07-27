package com.wanda.epc.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @program: iot_epc
 * @description: 变压器采集
 * @author: liuruishuo
 * @create: 2022-11-08 17:07
 **/
@Configuration
@EnableScheduling
public class CommonTask {

    @Autowired
    private CarsafeControlDevice carsafeControlDevice;

    @Scheduled(cron = "0/30 * * * * ?")
    public boolean processData() throws Exception {
        return carsafeControlDevice.processData();
    }

}
