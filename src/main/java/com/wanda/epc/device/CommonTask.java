package com.wanda.epc.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author: 孙率众
 **/
@Configuration
@EnableScheduling
public class CommonTask {

    @Autowired
    private DeviceHandler deviceHandler;

    @Scheduled(cron = "${cron.cron:0/30 * * * * ?}")
    public void getOnlineStatus() throws Exception {
        deviceHandler.processData();
    }

}
