package com.wanda.epc;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wanda.epc.common.RedisUtil;
import com.wanda.epc.device.BaseDevice;
import com.wanda.epc.device.CommonDevice;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
public class MjDevice extends BaseDevice {
    @Resource
    RedisUtil redisUtil;
    @Autowired
    private CommonDevice commonDevice;
    @Autowired
    private JdbcTemplate sqlServerJdbcTemple;
    @Value("${AcssDoorOpenUrl}")
    private String AcssDoorOpenUrl;

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
        String sql = "select * from dbo.Acss_DoorStatus";
        List<Map<String, Object>> maps = sqlServerJdbcTemple.queryForList(sql);
        if (!CollectionUtils.isEmpty(maps)) {
            for (Map<String, Object> map : maps) {
                //控制器编号
                String controllerNo = map.get("ControllerNo").toString();
                //门禁编号
                String doorNo = map.get("DoorNo").toString();
                //连接
                String connected = ObjectUtils.isEmpty(map.get("Connected")) ? null : map.get("Connected").toString();
                //门开超时
                String openedTimeout = ObjectUtils.isEmpty(map.get("OpenedTimeout")) ? null : map.get("OpenedTimeout").toString();
                //非法开门
                String broken = ObjectUtils.isEmpty(map.get("Broken")) ? null : map.get("Broken").toString();
                //门状态
                String opened = ObjectUtils.isEmpty(map.get("Opened")) ? null : map.get("Opened").toString();
                //点位编号
                String pointNumber = controllerNo.concat("_").concat(doorNo);
                //1.连接状态 - 在离线  1在线0离线
                DeviceMessage connectStatus = deviceParamMap.get(pointNumber.concat("_onlineStatus"));
                if (Objects.nonNull(connectStatus) && StringUtils.isNotEmpty(connected)) {
                    if (connected.equals("1")) {
                        connectStatus.setValue("1");
                        log.info("门禁采集器发送设备在线数据：{}", JSON.toJSONString(connectStatus));
                        sendMessage(connectStatus);
                    } else {
                        connectStatus.setValue("0");
                        log.info("门禁采集器发送设备离线数据：{}", JSON.toJSONString(connectStatus));
                        sendMessage(connectStatus);
                    }
                }

                //2.门超时报警 1报警0正常
                DeviceMessage openDoorOverTimeAlarm = deviceParamMap.get(pointNumber.concat("_wD_openDoorOverTimeAlarm"));
                if (Objects.nonNull(openDoorOverTimeAlarm) && StringUtils.isNotEmpty(openedTimeout)) {
                    if (openedTimeout.equals("1")) {
                        openDoorOverTimeAlarm.setValue("1");
                        log.info("门禁采集器发送探头超时状态数据：{}", JSON.toJSONString(openDoorOverTimeAlarm));
                        sendMessage(openDoorOverTimeAlarm);
                    } else {
                        openDoorOverTimeAlarm.setValue("0");
                        log.info("门禁采集器发送探头超时状态数据：{}", JSON.toJSONString(openDoorOverTimeAlarm));
                        sendMessage(openDoorOverTimeAlarm);
                    }
                }

                //3.非法开门 1报警0正常
                DeviceMessage illegalOpenAlarm = deviceParamMap.get(pointNumber.concat("_tamperAlarm"));
                if (Objects.nonNull(illegalOpenAlarm) && StringUtils.isNotEmpty(broken)) {
                    if (broken.equals("1")) {
                        illegalOpenAlarm.setValue("1");
                        log.info("门禁采集器发送非法开门状态数据：{}", JSON.toJSONString(illegalOpenAlarm));
                        sendMessage(illegalOpenAlarm);
                    } else {
                        illegalOpenAlarm.setValue("0");
                        log.info("门禁采集器发送非法开门状态数据：{}", JSON.toJSONString(illegalOpenAlarm));
                        sendMessage(illegalOpenAlarm);
                    }
                }

                //4.开关状态 1开0关
                DeviceMessage openStatus = deviceParamMap.get(pointNumber.concat("_openStatus"));
                if (Objects.nonNull(openStatus) && StringUtils.isNotEmpty(opened)) {
                    if (opened.equals("11") || opened.equals("12")) {
                        openStatus.setValue("1");
                        log.info("门禁采集器发送开关状态数据：{}", JSON.toJSONString(openStatus));
                        sendMessage(openStatus);
                    } else {
                        openStatus.setValue("0");
                        log.info("门禁采集器发送开关状态数据：{}", JSON.toJSONString(openStatus));
                        sendMessage(openStatus);
                    }
                }
            }
        }
        return true;
    }


    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        commonDevice.feedback(message);
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        if (ObjectUtils.isNotEmpty(deviceMessage) && StringUtils.isNotBlank(deviceMessage.getOutParamId())
                && deviceMessage.getOutParamId().endsWith("equipSwitchSet")) {
            String outParamId = deviceMessage.getOutParamId();
            if (redisUtil.hasKey(outParamId)) {
                return;
            }
            redisUtil.set(outParamId, "0", 5);
            final String[] strings = deviceMessage.getOutParamId().split("_");
            String acssDoorOpenUrl = AcssDoorOpenUrl + "&controllerno=" + strings[0] + "&doorno=" + strings[1];
            final String constent = HttpUtil.get(acssDoorOpenUrl);
            log.info("发送开门指令地址为：{}返回：{}", acssDoorOpenUrl, JSONObject.toJSONString(constent));
        }
    }


    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }
}
