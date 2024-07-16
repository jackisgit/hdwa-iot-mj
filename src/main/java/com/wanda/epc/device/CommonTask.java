package com.wanda.epc.device;

import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        deviceHandler.getOnlineStatus();
    }

    @Scheduled(cron = "40 10 * * * ?")
    public void getToken() throws Exception {
        deviceHandler.init();
    }


    @Scheduled(cron = "${epc.clearAlarmCron:0/60 * * * * ?}")
    public boolean processData() throws Exception {
        log.info("子系统不返回报警恢复指令，自动处理");
        Iterator<Map.Entry<String, List<DeviceMessage>>> iterator = BaseDevice.deviceParamListMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<DeviceMessage>> next = iterator.next();
            if (next.getKey().endsWith(DeviceHandler.ALARM_STATUS)) {
                for (DeviceMessage deviceMessage : next.getValue()) {
                    deviceMessage.setValue("0");
                    commonDevice.sendMessage(deviceMessage);
                }
            }
        }
        return true;
    }


}
