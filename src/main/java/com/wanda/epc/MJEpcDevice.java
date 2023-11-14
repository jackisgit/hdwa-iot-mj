package com.wanda.epc;

import com.alibaba.fastjson.JSON;
import com.wanda.epc.device.BaseDevice;
import com.wanda.epc.device.CommonDevice;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
public class MJEpcDevice extends BaseDevice {
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
            List<DeviceMessage> onlineStatus = deviceParamListMap.get(pointNumber.concat("_onlineStatus"));
            if (!CollectionUtils.isEmpty(onlineStatus) && StringUtils.isNotEmpty(connected)) {
                for (DeviceMessage deviceMessage : onlineStatus) {
                    if ("1".equals(connected)) {
                        deviceMessage.setValue("1");
                    } else {
                        deviceMessage.setValue("0");
                    }
                    log.info("门禁采集器发送设备在离线数据：{}", JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            }
            //2.门超时报警 1报警0正常
            List<DeviceMessage> dOpenDoorOverTimeAlarms = deviceParamListMap.get(pointNumber.concat("_wD_openDoorOverTimeAlarm"));
            if (!CollectionUtils.isEmpty(dOpenDoorOverTimeAlarms) && StringUtils.isNotEmpty(openedTimeout)) {
                for (DeviceMessage deviceMessage : dOpenDoorOverTimeAlarms) {
                    if ("1".equals(openedTimeout)) {
                        deviceMessage.setValue("1");
                    } else {
                        deviceMessage.setValue("0");
                    }
                    log.info("门禁采集器发送探头门超时报警数据：{}", JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            }
            //3.非法开门 1报警0正常
            List<DeviceMessage> illegalOpenAlarms = deviceParamListMap.get(pointNumber.concat("_wD_IllegalOpenAlarm"));
            if (!CollectionUtils.isEmpty(illegalOpenAlarms) && StringUtils.isNotEmpty(broken)) {
                for (DeviceMessage deviceMessage : illegalOpenAlarms) {
                    if ("1".equals(broken)) {
                        deviceMessage.setValue("1");
                    } else {
                        deviceMessage.setValue("0");
                    }
                    log.info("门禁采集器发送非法开门报警数据：{}", JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            }
            //4.开关状态 1报警0正常
            List<DeviceMessage> openStatusList = deviceParamListMap.get(pointNumber.concat("_openStatus"));
            if (!CollectionUtils.isEmpty(openStatusList) && StringUtils.isNotEmpty(opened)) {
                for (DeviceMessage deviceMessage : openStatusList) {
                    if ("1".equals(opened)) {
                        deviceMessage.setValue("1");
                    } else {
                        deviceMessage.setValue("0");
                    }
                    log.info("门禁采集器发送开关状态数据：{}", JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            }
            //5.防拆报警 1防拆报警0正常
            List<DeviceMessage> tamperAlarms = deviceParamListMap.get(pointNumber.concat("_tamperAlarm"));
            if (!CollectionUtils.isEmpty(tamperAlarms) && StringUtils.isNotEmpty(antipryAlarm)) {
                for (DeviceMessage deviceMessage : tamperAlarms) {
                    if ("1".equals(antipryAlarm)) {
                        deviceMessage.setValue("1");
                    } else {
                        deviceMessage.setValue("0");
                    }
                    log.info("门禁采集器发送防拆报警数据：{}", JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            }
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
