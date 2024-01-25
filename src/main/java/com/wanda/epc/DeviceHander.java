package com.wanda.epc;

import com.wanda.epc.device.BaseDevice;
import com.wanda.epc.device.CommonDevice;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @author LianYanFei
 * @version 1.0
 * @project iot-epc-module
 * @description 变压器采集
 * @date 2023/2/20 14:59:36
 */
@Slf4j
@Service
public class DeviceHander extends BaseDevice {
    public static final String ONLINE_STATUS = "_onlineStatus";
    public static final String W_D_OPEN_DOOR_OVER_TIME_ALARM = "_wD_openDoorOverTimeAlarm";
    public static final String W_D_ILLEGAL_OPEN_ALARM = "_wD_IllegalOpenAlarm";
    public static final String OPEN_STATUS = "_openStatus";
    public static final String TAMPER_ALARM = "_tamperAlarm";
    @Autowired
    private CommonDevice commonDevice;

    @Autowired
    private JdbcTemplate sqlServerJdbcTemple;


    @Override
    public void sendMessage(DeviceMessage dm) {
        //如果数据变化则，发送emqx
        if (dm != null) {
            commonDevice.sendMessage(dm);
        }
    }

    @Override
    @PostConstruct
    public boolean processData() throws Exception {
        //查询车位的状态信息
        String sql = "select * from dbo.F_DoorStateAndAlarm";
        List<Map<String, Object>> maps = sqlServerJdbcTemple.queryForList(sql);
        if (CollectionUtils.isEmpty(maps)) {
            return false;
        }
        for (Map<String, Object> map : maps) {
            //控制器编号
            String controllerNo = map.get("dev_no").toString();
            //门禁编号
            String doorNo = map.get("Door_ID").toString();
            //连接
            String connected = ObjectUtils.isEmpty(map.get("DoorState")) ? null : map.get("DoorState").toString();
            //门开超时
            String openedTimeout = ObjectUtils.isEmpty(map.get("TimeOutAlarm")) ? null : map.get("TimeOutAlarm").toString();
            //防拆报警
            String antipryAlarm = ObjectUtils.isEmpty(map.get("AntipryAlarm")) ? null : map.get("AntipryAlarm").toString();
            //非法开门
            String broken = ObjectUtils.isEmpty(map.get("IllegalCardAlarm")) ? null : map.get("IllegalCardAlarm").toString();
            //门状态
            String opened = ObjectUtils.isEmpty(map.get("OpenDoor")) ? null : map.get("OpenDoor").toString();
            //点位编号
            String pointNumber = controllerNo.concat("_").concat(doorNo);
            //1.连接状态 - 在离线  1在线0离线
            String onlineStatusValue;
            if ("1".equals(connected)) {
                onlineStatusValue = "1";
            } else {
                onlineStatusValue = "0";
            }
            sendMsg(pointNumber.concat(ONLINE_STATUS), onlineStatusValue);
            //2.门超时报警 1报警0正常
            String wdOpenDoorOverTimeAlarmValue;
            if ("1".equals(openedTimeout)) {
                wdOpenDoorOverTimeAlarmValue = "1";
            } else {
                wdOpenDoorOverTimeAlarmValue = "0";
            }
            sendMsg(pointNumber.concat(W_D_OPEN_DOOR_OVER_TIME_ALARM), wdOpenDoorOverTimeAlarmValue);
            //3.非法开门 1报警0正常
            String wdIllegalOpenAlarmValue;
            if ("1".equals(broken)) {
                wdIllegalOpenAlarmValue = "1";
            } else {
                wdIllegalOpenAlarmValue = "0";
            }
            sendMsg(pointNumber.concat(W_D_ILLEGAL_OPEN_ALARM), wdIllegalOpenAlarmValue);
            //4.开关状态 1报警0正常
            String openStatusValue;
            if ("1".equals(opened)) {
                openStatusValue = "1";
            } else {
                openStatusValue = "0";
            }
            sendMsg(pointNumber.concat(OPEN_STATUS), openStatusValue);
            //5.防拆报警 1防拆报警0正常
            String tamperAlarmValue;
            if ("1".equals(antipryAlarm)) {
                tamperAlarmValue = "1";
            } else {
                tamperAlarmValue = "0";
            }
            sendMsg(pointNumber.concat(TAMPER_ALARM),tamperAlarmValue);
        }
        return true;
    }

    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }
}
