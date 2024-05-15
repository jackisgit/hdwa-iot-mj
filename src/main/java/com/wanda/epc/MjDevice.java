package com.wanda.epc;

import cn.hutool.http.HttpUtil;
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
    public static final String ONLINE_STATUS = "_onlineStatus";
    public static final String W_D_OPEN_DOOR_OVER_TIME_ALARM = "_wD_openDoorOverTimeAlarm";
    public static final String W_D_ILLEGAL_OPEN_ALARM = "_wD_IllegalOpenAlarm";
    public static final String OPEN_STATUS = "_openStatus";
    public static final String EQUIP_SWITCH_SET = "equipSwitchSet";
    @Resource
    RedisUtil redisUtil;
    @Autowired
    private CommonDevice commonDevice;
    @Autowired
    private JdbcTemplate sqlServerJdbcTemple;
    @Value("${epc.AcssDoorOpenUrl}")
    private String AcssDoorOpenUrl;

    @Override
    public void sendMessage(DeviceMessage dm) {
    }

    @Override
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
                String onlineStatusValue = "0";
                if (connected.equals("1")) {
                    onlineStatusValue = "1";
                }
                sendMsg(pointNumber.concat(ONLINE_STATUS), onlineStatusValue);
                String wdOpenDoorOverTimeAlarmvalue = "0";
                if (openedTimeout.equals("1")) {
                    wdOpenDoorOverTimeAlarmvalue = "1";
                }
                sendMsg(pointNumber.concat(W_D_OPEN_DOOR_OVER_TIME_ALARM), wdOpenDoorOverTimeAlarmvalue);
                String wdIllegalOpenAlarmValue = "0";
                if (broken.equals("1")) {
                    wdIllegalOpenAlarmValue = "1";
                }
                sendMsg(pointNumber.concat(W_D_ILLEGAL_OPEN_ALARM), wdIllegalOpenAlarmValue);
                String openStatusValue = "0";
                if (opened.equals("11") || opened.equals("12")) {
                    openStatusValue = "1";
                }
                sendMsg(pointNumber.concat(OPEN_STATUS), openStatusValue);
            }
        }
        return true;
    }


    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        commonDevice.feedback(message);
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        if (ObjectUtils.isNotEmpty(deviceMessage) && StringUtils.isNotBlank(deviceMessage.getOutParamId())
                && deviceMessage.getOutParamId().endsWith(EQUIP_SWITCH_SET)) {
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
