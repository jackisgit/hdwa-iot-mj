package com.wanda.epc.device;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.wanda.epc.DTO.ControlDTO;
import com.wanda.epc.DTO.ControlDoorDTO;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author 孙率众
 */
@Service
@Slf4j
public class DeviceHandler extends BaseDevice {

    public static final String EQUIP_SWITCH_SET = "_equipSwitchSet";
    public static final String OPEN_STATUS = "_openStatus";
    public static final String ONLINE_STATUS = "_onlineStatus";
    public static final String ALARM_STATUS = "_alarmStatus";
    /**
     * 门通道查询URI（获取门开关状态）
     */
    private final String getDeviceRealStatus = "/api/E8Door/man-device-info/get-device-real-status";
    /**
     * 门禁控制URI（开门）
     */
    private final String deviceControl = "/api/E8Door/man-device-info/device-control";
    List<Integer> list = new ArrayList<>();
    @Value("${epc.host}")
    private String host;
    @Value("${epc.key}")
    private String key;
    @Value("${epc.password}")
    private String password;

    @PostConstruct
    public void init() {
        deviceParamListMap.entrySet().forEach(entry -> {
            list.add(Integer.valueOf(entry.getKey().split("_")[0]));
        });
    }

    /**
     * 构建请求头
     *
     * @param timestamp
     * @param sign
     * @return
     */
    private Map<String, String> buildHeader(Long timestamp, String sign) {
        Map<String, String> header = new HashMap<>();
        header.put("key", key);
        header.put("timestamp", String.valueOf(timestamp));
        header.put("sign", sign);
        return header;
    }

    @Override
    public void sendMessage(DeviceMessage dm) {
        commonDevice.sendMessage(dm);
    }

    @Override
    public boolean processData() {
        String url = host + getDeviceRealStatus;
        long currentTimeMillis = System.currentTimeMillis();
        String sign = getSign(url, currentTimeMillis);
        Map<String, String> header = buildHeader(currentTimeMillis, sign);
        String result = HttpRequest.post(url).body(Base64.encode(JSONObject.toJSONString(list))).addHeaders(header).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url, result);
        Object read = JSONPath.read(result, "$.success");
        if (ObjectUtils.isNotEmpty(read) && !"false".equals(String.valueOf(read))) {
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
            ControlDoorDTO controlDoorDTO = new ControlDoorDTO();
            ControlDTO controlDTO = new ControlDTO();
            controlDTO.setDeviceId(strings[0]);
            controlDTO.setDoorId(strings[0]);
            Integer type = 0;
            String result;
            if ("1.0".equals(value)) {
                type = 1;
            }
            controlDoorDTO.setType(type);
            String url = host + deviceControl;
            long currentTimeMillis = System.currentTimeMillis();
            String sign = getSign(url + "type=" + type, currentTimeMillis);
            Map<String, String> header = buildHeader(currentTimeMillis, sign);
            result = HttpRequest.post(url).body(Base64.encode(JSONObject.toJSONString(controlDoorDTO))).addHeaders(header).timeout(2000).execute().body();
            log.info("接口:{},返回值:{}", url, result);
            Object read = JSONPath.read(result, "$.success");
            if (ObjectUtils.isNotEmpty(read) && !"false".equals(String.valueOf(read))) {
                sendMsg(strings[0] + OPEN_STATUS, value);
            }
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    /**
     * 鉴权
     *
     * @param url
     * @param timestamp
     * @return
     * @throws Exception
     */
    private String getSign(String url, Long timestamp) {
        //1.拼接上毫秒时间戳
        url = url + "&timestamp=" + timestamp;
        //2.字符串转换为大写
        url = url.toUpperCase(Locale.ROOT);
        //3.大写字符串拼接secret
        url = url + password;
        //4.MD5加密
        url = SecureUtil.md5(url);
        //5.字符串转换为大写
        url = url.toUpperCase(Locale.ROOT);
        return url;
    }
}
