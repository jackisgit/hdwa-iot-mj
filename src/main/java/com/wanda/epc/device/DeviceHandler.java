package com.wanda.epc.device;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
    /**
     * 开门超时报警后缀
     */
    public static final String WD_OPENDOOROVERTIMEALARM = "_wD_openDoorOverTimeAlarm";
    /**
     * 非法开门报警后缀
     */
    public static final String WD_ILLEGALOPENALARM = "_wD_IllegalOpenAlarm";
    /**
     * 门通道查询URI（获取门开关状态）
     */
    private final String deviceMonitorTree = "/api/E8Door/man-device-info/device-monitor-tree";

    /**
     * 查询门禁超时和非法刷卡接口
     */
    private final String accessOvertimeAndIllegalCardSwiping = "/api/E8Door/man-abnormal-record/get-page-list";

    /**
     * 门禁控制URI（开门）
     */
    private final String deviceControl = "/api/E8Door/man-device-info/device-control";
    @Value("${epc.host}")
    private String host;
    @Value("${epc.key}")
    private String key;
    @Value("${epc.password}")
    private String password;

    /**
     * 构建请求头
     *
     * @param timestamp
     * @param sign
     * @param paramstr
     * @return
     */
    private Map<String, String> buildHeader(Long timestamp, String sign, boolean paramstr) {
        Map<String, String> header = new HashMap<>();
        header.put("key", key);
        header.put("timestamp", String.valueOf(timestamp));
        header.put("sign", sign);
        if (paramstr) {
            //如果为ge请求则按照接口说明增加随机数，这里写死123
            header.put("paramstr", "123");
        }
        return header;
    }

    @Override
    public boolean processData() {
        try {
            //门通道查询数据
            String url = host + deviceMonitorTree;
            long currentTimeMillis = System.currentTimeMillis();
            String sign = getOrDeleteSign(url, currentTimeMillis);
            Map<String, String> header = buildHeader(currentTimeMillis, sign, true);
            log.info("门通道数据采集接口:{},请求头:{}", url, header);
            String result = HttpRequest.get(url).addHeaders(header).timeout(2000).execute().body();
            log.info("门通道数据采集接口:{},返回值:{}", url, result);
            DeviceMonitorTreeDTO deviceMonitorTreeDTO = JSONObject.parseObject(result, DeviceMonitorTreeDTO.class);
            if (deviceMonitorTreeDTO.getSuccess()) {
                List<DeviceMonitorTreeChildListDTO> list = new ArrayList<>();
                List<DeviceMonitorTreeChildListDTO> DeviceMonitorTreeChildListDTOS = deviceMonitorTreeDTO.getResult();
                if (CollectionUtils.isEmpty(DeviceMonitorTreeChildListDTOS)) {
                    return false;
                }
                for (DeviceMonitorTreeChildListDTO deviceMonitorTreeChildListDTO : DeviceMonitorTreeChildListDTOS) {
                    List<DeviceMonitorTreeChildListDTO> childList = deviceMonitorTreeChildListDTO.getChildList();
                    if (CollectionUtils.isEmpty(childList)) {
                        continue;
                    }
                    list.addAll(childList);
                }
                if (CollectionUtils.isEmpty(list)) {
                    return false;
                }
                for (DeviceMonitorTreeChildListDTO deviceMonitorTreeChildListDTO : list) {
                    String openStatus = deviceMonitorTreeChildListDTO.getId() + OPEN_STATUS;
                    String onlineStatus = deviceMonitorTreeChildListDTO.getId() + ONLINE_STATUS;
                    String doorStaus = "0";
                    if (deviceMonitorTreeChildListDTO.getDoorStatus() == 1 || deviceMonitorTreeChildListDTO.getDoorStatus() == 2) {
                        doorStaus = "1";
                    }
                    sendMsg(openStatus, doorStaus);
                    sendMsg(onlineStatus, String.valueOf(deviceMonitorTreeChildListDTO.getOnlineStatus()));
                }
            }
        } catch (Exception e) {
            log.error("采集门通道数据失败", e);
        }

        //非法开门数据
        try {
            String url = host + accessOvertimeAndIllegalCardSwiping;
            long currentTimeMillis = System.currentTimeMillis();
            int pageIndex = 1;
            int maxResultCount = 1;
            Map<String, Object> params = new HashMap<>();
            params.put("pageIndex", pageIndex);
            params.put("maxResultCount", maxResultCount);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.MINUTE, -1);
            Map<String, Object> query = new HashMap<>();
            //当前时间减去1分钟，查最近1分钟有没有报警记录
            query.put("startTime", DateUtil.format(calendar.getTime(), "yyyy-MM-dd HH:mm:ss"));
            query.put("endTime", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            //查询异常类型：1非法开门 2超时开门
            query.put("abnormalType", 1);
            query.put("isViewFullData", false);
            String sign = postSign(url + "maxResultCount=" + maxResultCount + "&pageIndex=" + pageIndex, currentTimeMillis);
            Map<String, String> header = buildHeader(currentTimeMillis, sign, false);
            deviceParamListMap.forEach((s, deviceMessages) -> {
                if (!s.contains(WD_ILLEGALOPENALARM)) {
                    return;
                }
               /* query.put("doorId", 0);
                query.put("doorName", "");
                query.put("deviceName", "");
                query.put("areaId", 0);
                query.put("areaName", "");*/
                String deviceId = s.split("_")[0];
                query.put("deviceId", deviceId);
                params.put("queryDto", query);
                log.info("非法开门数据采集接口:{},请求头:{},参数:{}", url, header, params);
                String body = Base64.encode(JSONObject.toJSONString(params));
                String result = HttpRequest.post(url).body(body).contentType("application/json;charset=UTF-8").addHeaders(header).timeout(2000).execute().body();
                log.info("非法开门数据采集接口:{},返回值:{}", url, result);
                JSONObject jsonObject = JSON.parseObject(result);
                if (jsonObject.getBoolean("success")) {
                    JSONObject resultJson = jsonObject.getJSONObject("result");
                    JSONArray item = resultJson.getJSONArray("item");
                    String value = "0";
                    if (item.size() > 0) {
                        value = "1";
                    }
                    sendMsg(deviceId + WD_ILLEGALOPENALARM, value);
                }
            });
        } catch (Exception e) {
            log.error("采集非法开门数据失败", e);
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
            control(strings[0], value);
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    /**
     * 控制
     *
     * @param doorId
     * @param value
     */
    public void control(String doorId, String value) {
        ControlDoorDTO controlDoorDTO = new ControlDoorDTO();
        List<ControlDTO> controlDTOs = new ArrayList<>();
        ControlDTO controlDTO = new ControlDTO();
        controlDTO.setDoorId(doorId);
        controlDTOs.add(controlDTO);
        controlDoorDTO.setControlList(controlDTOs);
        Integer type = 0;
        String result;
        if ("1.0".equals(value)) {
            type = 1;
        }
        controlDoorDTO.setType(type);
        String url = host + deviceControl;
        long currentTimeMillis = System.currentTimeMillis();
        String sign = postSign(url + "type=" + type, currentTimeMillis);
        Map<String, String> header = buildHeader(currentTimeMillis, sign, false);
        String body = Base64.encode(JSONObject.toJSONString(controlDoorDTO));
        log.info("控制接口:{},参数:{},请求头:{}", url, body, JSONObject.toJSONString(header));
        result = HttpRequest.post(url).body(body).contentType("application/json;charset=UTF-8").addHeaders(header).timeout(2000).execute().body();
        log.info("控制接口:{},返回值:{}", url, result);
        Object read = JSONPath.read(result, "$.success");
        if (ObjectUtils.isNotEmpty(read) && !"false".equals(String.valueOf(read))) {
            sendMsg(doorId + OPEN_STATUS, value);
        }
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
