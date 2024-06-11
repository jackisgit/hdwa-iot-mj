package com.wanda.epc.device;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author 孙率众
 */
@Service
@Slf4j
public class DeviceHandler extends BaseDevice {

    /**
     * 控制点后缀
     */
    public static final String EQUIP_SWITCH_SET = "_equipSwitchSet";
    /**
     * 开关状态后缀
     */
    public static final String OPEN_STATUS = "_openStatus";
    /**
     * 在线状态后缀
     */
    public static final String ONLINE_STATUS = "_onlineStatus";

    private static final String ARTEMIS_PATH = "/artemis";

    /**
     * 门禁集合
     */
    Set<String> doorIndexSet = new HashSet<>();

    @Value("${epc.host}")
    private String host;
    @Value("${epc.appKey}")
    private String appKey;
    @Value("${epc.appSecret}")
    private String appSecret;
    @Value("${epc.search}")
    private String search;
    @Value("${epc.control}")
    private String control;
    @Value("${epc.states}")
    private String states;

    /**
     * https://ip:port/artemis/api/resource/v1/org/orgList
     * 通过查阅AI Cloud开放平台文档或网关门户的文档可以看到获取组织列表的接口定义,该接口为POST请求的Rest接口, 入参为JSON字符串，接口协议为https。
     * ArtemisHttpUtil工具类提供了doPostStringArtemis调用POST请求的方法，入参可传JSON字符串, 请阅读开发指南了解方法入参，没有的参数可传null
     */
    public String sendHttps(String url, Map<String, Object> paramMap) {
        try {
            ArtemisConfig config = new ArtemisConfig();
            // 代理API网关nginx服务器ip端口
            config.setHost(host);
            // 秘钥appkey
            config.setAppKey(appKey);
            // 秘钥appSecret
            config.setAppSecret(appSecret);
            final String getCamsApi = ARTEMIS_PATH + url;
            String body = JSON.toJSON(paramMap).toString();
            Map<String, String> path = new HashMap<String, String>(2) {
                {
                    put("https://", getCamsApi);
                }
            };
            return ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");
        } catch (Exception e) {
            log.error("调用接口:{}，参数:{}异常", url, JSONObject.toJSONString(paramMap), e);
            return null;
        }
    }

    @PostConstruct
    public void init() {
        // post请求Form表单参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("pageNo", "1");
        paramMap.put("pageSize", "20000");
        String result = sendHttps(search, paramMap);
        log.info("接口:{},参数:{},返回:{}", search, JSONObject.toJSONString(paramMap), result);
        deviceParamListMap.forEach((key, value) -> {
            doorIndexSet.add(key.split("_")[0]);
        });
    }

    @Override
    public boolean processData() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("doorIndexCodes", doorIndexSet);
        String result = sendHttps(states, paramMap);
        log.info("接口:{},参数:{},返回:{}", states, JSONObject.toJSONString(paramMap), result);
        List<Map<String, Object>> list = (List<Map<String, Object>>) JSONPath.read(result, "$.data.authDoorList");
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }
        for (Map<String, Object> map : list) {
            String doorIndexCode = String.valueOf(map.get("doorIndexCode"));
            //门状态 1: 开门状态 2: 关门状态 3: 离线状态 4: 常闭 5: 反锁 6: 常开 7: 常开 8: 常闭
            String doorState = String.valueOf(map.get("doorState"));
            String onlineStatus = "1";
            String openStatus = "0";
            if ("3".equals(doorState)) {
                onlineStatus = "0";
            }
            if ("1,6,7".contains(doorState)) {
                openStatus = "1";
            }
            sendMsg(doorIndexCode + ONLINE_STATUS, onlineStatus);
            sendMsg(doorIndexCode + OPEN_STATUS, openStatus);
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
            //todo 控制
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("doorIndexCodes", strings[0]);
            int controlType = 1;
            if ("1.0".equals(value)) {
                controlType = 2;
            }
            paramMap.put("controlType", controlType);
            String result = sendHttps(control, paramMap);
            log.info("接口:{},参数:{},返回:{}", control, JSONObject.toJSONString(paramMap), result);
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

}
