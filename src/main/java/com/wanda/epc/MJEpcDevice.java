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
import java.util.Objects;

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
            DeviceMessage connectStatus = deviceParamMap.get(pointNumber.concat("_onlineStatus"));
            if (Objects.nonNull(connectStatus) && StringUtils.isNotEmpty(connected)) {
                if ("1".equals(connected)) {
                    connectStatus.setValue("1");
                } else {
                    connectStatus.setValue("0");
                }
                log.info("门禁采集器发送设备在离线数据：{}", JSON.toJSONString(connectStatus));
                sendMessage(connectStatus);
            }
            //2.门超时报警 1报警0正常
            DeviceMessage openDoorOverTimeAlarm = deviceParamMap.get(pointNumber.concat("_wD_openDoorOverTimeAlarm"));
            if (Objects.nonNull(openDoorOverTimeAlarm) && StringUtils.isNotEmpty(openedTimeout)) {
                if ("1".equals(openedTimeout)) {
                    openDoorOverTimeAlarm.setValue("1");
                } else {
                    openDoorOverTimeAlarm.setValue("0");
                }
                log.info("门禁采集器发送探头门超时报警数据：{}", JSON.toJSONString(openDoorOverTimeAlarm));
                sendMessage(openDoorOverTimeAlarm);
            }
            //3.非法开门 1报警0正常
            DeviceMessage illegalOpenAlarm = deviceParamMap.get(pointNumber.concat("_wD_IllegalOpenAlarm"));
            if (Objects.nonNull(illegalOpenAlarm) && StringUtils.isNotEmpty(broken)) {
                if ("1".equals(broken)) {
                    illegalOpenAlarm.setValue("1");
                } else {
                    illegalOpenAlarm.setValue("0");
                }
                log.info("门禁采集器发送非法开门报警数据：{}", JSON.toJSONString(illegalOpenAlarm));
                sendMessage(illegalOpenAlarm);
            }
            //4.开关状态 1报警0正常
            DeviceMessage openStatus = deviceParamMap.get(pointNumber.concat("_openStatus"));
            if (Objects.nonNull(openStatus) && StringUtils.isNotEmpty(opened)) {
                if ("1".equals(opened)) {
                    openStatus.setValue("1");
                } else {
                    openStatus.setValue("0");
                }
                log.info("门禁采集器发送开关状态数据：{}", JSON.toJSONString(openStatus));
                sendMessage(openStatus);
            }
            //5.防拆报警 1防拆报警0正常
            DeviceMessage tamperAlarm = deviceParamMap.get(pointNumber.concat("_tamperAlarm"));
            if (Objects.nonNull(tamperAlarm) && StringUtils.isNotEmpty(antipryAlarm)) {
                if ("1".equals(antipryAlarm)) {
                    tamperAlarm.setValue("1");
                } else {
                    tamperAlarm.setValue("0");
                }
                log.info("门禁采集器发送防拆报警数据：{}", JSON.toJSONString(tamperAlarm));
                sendMessage(tamperAlarm);
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
