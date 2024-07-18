package com.wanda.epc.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

/**
 * @author: 孙率众
 **/
@Configuration
@EnableScheduling
@Slf4j
public class CommonTask {

    @Resource
    private DeviceHandler deviceHandler;
    @Resource
    private CommonDevice commonDevice;

    @Scheduled(cron = "${cron.cron:0/30 * * * * ?}")
    public void getOnlineStatus() throws Exception {
        deviceHandler.getOnlineStatus2();
    }

    @Scheduled(cron = "40 10 * * * ?")
    public void getToken() throws Exception {
        deviceHandler.init();
    }


    @Scheduled(cron = "${epc.clearAlarmCron:0/60 * * * * ?}")
    public boolean clearAlarm() {
        deviceHandler.clearAlarm();
        return true;
    }


}
