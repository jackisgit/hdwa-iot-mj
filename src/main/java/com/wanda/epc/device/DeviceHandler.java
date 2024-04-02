package com.wanda.epc.device;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.wanda.epc.DTO.ControlDTO;
import com.wanda.epc.DTO.ControlDoorDTO;
import com.wanda.epc.DTO.DeviceMonitorTreeChildListDTO;
import com.wanda.epc.DTO.DeviceMonitorTreeDTO;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    /**
     * 门通道查询URI（获取门开关状态）
     */
    private final String deviceMonitorTree = "/api/E8Door/man-device-info/device-monitor-tree";
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

    public static void main(String[] args) {
        long currentTimeMillis = System.currentTimeMillis();
        System.out.println(currentTimeMillis);
        String str = "HTTP://192.168.12.51:50014/API/E8/PARK-FIX-TEMP-INFO/319578430836805?ID=123456781696908563000ABCDabcd1234";
        str = SecureUtil.md5(str);
        //5.字符串转换为大写
        str = str.toUpperCase(Locale.ROOT);
        System.out.println(str);
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
        header.put("paramstr", "123");
        return header;
    }

    @Override
    public void sendMessage(DeviceMessage dm) {
        commonDevice.sendMessage(dm);
    }

    @Override
    public boolean processData() {
        String url = host + deviceMonitorTree;
        long currentTimeMillis = System.currentTimeMillis();
        String sign = getOrDeleteSign(url, currentTimeMillis);
        Map<String, String> header = buildHeader(currentTimeMillis, sign);
        log.info("接口:{},请求头:{}", url, header);
        //String result = HttpRequest.post(url).addHeaders(header).timeout(2000).execute().body();
        String result = HttpRequest.get(url).addHeaders(header).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url, result);
        DeviceMonitorTreeDTO deviceMonitorTreeDTO = JSONObject.parseObject(result, DeviceMonitorTreeDTO.class);
        log.info("返回值2:{}", JSONObject.toJSONString(deviceMonitorTreeDTO));
        if (deviceMonitorTreeDTO.getSuccess()) {
            List<DeviceMonitorTreeChildListDTO> DeviceMonitorTreeChildListDTOS = deviceMonitorTreeDTO.getResult();
            if (CollectionUtils.isEmpty(DeviceMonitorTreeChildListDTOS)) {
                return false;
            }
            for (DeviceMonitorTreeChildListDTO deviceMonitorTreeChildListDTO : DeviceMonitorTreeChildListDTOS) {
                String openStatus = deviceMonitorTreeChildListDTO.getDeviceId() + OPEN_STATUS;
                String onlineStatus = deviceMonitorTreeChildListDTO.getDeviceId() + ONLINE_STATUS;
                String doorStaus = "0";
                if (deviceMonitorTreeChildListDTO.getDoorStatus() == 1 || deviceMonitorTreeChildListDTO.getDoorStatus() == 2) {
                    doorStaus = "1";
                }
                sendMsg(openStatus, doorStaus);
                sendMsg(onlineStatus, String.valueOf(deviceMonitorTreeChildListDTO.getOnlineStatus()));
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
            String sign = postSign(url + "type=" + type, currentTimeMillis);
            Map<String, String> header = buildHeader(currentTimeMillis, sign);
            log.info("接口:{},参数:{},请求头:{}", url, JSONObject.toJSONString(controlDoorDTO), JSONObject.toJSONString(header));
            result = HttpRequest.post(url).body(Base64.encode(JSONObject.toJSONString(controlDoorDTO))).addHeaders(header).timeout(2000).execute().body();
            //result = HttpRequest.post(url).body(JSONObject.toJSONString(controlDoorDTO)).addHeaders(header).timeout(2000).execute().body();
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
     * get/delete请求鉴权
     * 第1步:  使用请求的完整url拼接时间戳后转换为大写
     * 第2步:  将第一步的大写字符串拼接secret
     * 第3步:  将第二部的字符串进行MD5加密
     * 第4步:  对第三步的字符串转换大写得到sign值
     *
     * @param url
     * @param timestamp
     * @return
     * @throws Exception
     */
    private String getOrDeleteSign(String url, Long timestamp) {
        //1.拼接上毫秒时间戳
        url = url + timestamp;
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

    /**
     * post请求鉴权
     * <p>
     * 第1步：对请求的json数据按照key升序排序，并把排序后的key和它对应的value拼接成一个字符串: key1=value1&key2=value2最后拼接上毫秒时间戳key1=value1&key2=value2&timestamp=1696901995996
     * 第2步: 使用请求url拼接第1步数据
     * 第3步: 将第2步拼接字符串转换为大写
     * 第4步: 将第3步的大写字符串拼接secret
     * 第5步: 对第4步的字符串进行MD5加密
     * 第6步：将第5步字符串转换大写得到sign值
     *
     * @param url
     * @param timestamp
     * @return
     * @throws Exception
     */
    private String postSign(String url, Long timestamp) {
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
