package com.wanda.epc.device;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.wanda.epc.device.dto.DoorStateDto;
import com.wanda.epc.device.dto.DoorStateResultDto;
import com.wanda.epc.device.utils.TokenGenerateUtil;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author LianYanFei
 * @description 车安门禁子系统对接
 * @date 2023/7/27
 */
@Slf4j
@Service
public class CarsafeControlDevice extends BaseDevice {

    public static final String FAULT_STATUS = "_faultStatus";
    public static final String OPEN_STATUS = "_openStatus";
    public static final String ONLINE_STATUS = "_onlineStatus";
    public static final String TAMPER_ALARM = "_tamperAlarm";
    public static final String OPEN_DOOR_OVER_TIME_ALARM = "_openDoorOverTimeAlarm";
    public static final String EQUIP_SWITCH_SET = "equipSwitchSet";
    @Value("${access.secret}")
    private String privateKey;

    @Value("${access.doorOpenUrl}")
    private String doorOpenUrl;

    @Value("${access.doorStatusUrl}")
    private String doorStatusUrl;

    @Override
    public void sendMessage(DeviceMessage dm) {
        if (dm != null) {
            commonDevice.sendMessage(dm);
        }
    }

    @Override
    public boolean processData() throws Exception {
        List<DoorStateDto> doorStateDtoList = doorStateList();
        if (!CollectionUtils.isEmpty(doorStateDtoList)) {
            doorStateDtoList.forEach(this::olineStatus);
            doorStateDtoList.forEach(this::openStatus);
            doorStateDtoList.forEach(this::faultStatus);
            doorStateDtoList.forEach(this::tamperAlarm);
            doorStateDtoList.forEach(this::openDoorOverTimeAlarm);
        }
        return true;
    }

    /**
     * @description 采集在离线
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void olineStatus(DoorStateDto doorStateDto) {
        sendMsg(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat(ONLINE_STATUS), doorStateDto.getConnected());
    }

    /**
     * @description 开关状态
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void openStatus(DoorStateDto doorStateDto) {
        String value;
        if (doorStateDto.getOpened().equals("1") || doorStateDto.getOpened().equals("2")) {
            value = "1";
        } else {
            value = "0";
        }
        sendMsg(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat(OPEN_STATUS), value);
    }

    /**
     * @description 故障状态
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void faultStatus(DoorStateDto doorStateDto) {
        String value;
        if (doorStateDto.getConnected().equals("0")) {
            value = "1";
        } else {
            value = "0";
        }
        sendMsg(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat(FAULT_STATUS), value);
    }

    /**
     * @description 防拆报警
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void tamperAlarm(DoorStateDto doorStateDto) {
        String value;
        if (doorStateDto.getBroken().equals("1")) {
            value = "1";
        } else {
            value = "0";
        }
        sendMsg(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat(TAMPER_ALARM), value);
    }

    /**
     * @description 超时未关门报警
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void openDoorOverTimeAlarm(DoorStateDto doorStateDto) {
        String value;
        if (doorStateDto.getOpenedtimeout().equals("1")) {
            value = "1";
        } else {
            value = "0";
        }
        sendMsg(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat(OPEN_DOOR_OVER_TIME_ALARM), value);
    }

    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        commonDevice.feedback(message);
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        if (deviceMessage != null && deviceMessage.getOutParamId() != null && deviceMessage.getOutParamId().endsWith(EQUIP_SWITCH_SET)) {
            //如果为开
            if (value.equals("1.0")) {
                String outParamId = deviceMessage.getOutParamId();
                String[] split = outParamId.split("_");
                String controllerNo = split[0];
                String doorNo = split[1];
                log.info("控制板编号[{}], 读头编号{}]设备下发控制,控制值为{}", controllerNo, doorNo, value);
                Boolean opened = openDoor(controllerNo, doorNo);
                log.info("控制板编号[{}], 读头编号{}]设备下发控制,返回结果为[{}]", controllerNo, doorNo, opened);
                String openParamId = outParamId.replace("control", "openStatus");
                if (opened) {
                    sendMsg(controllerNo.concat("_").concat(doorNo).concat(OPEN_STATUS),"1");
                }
            }
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    /**
     * @description 门禁开门
     * @author LianYanFei
     * @date 2023/7/28
     */
    public Boolean openDoor(String controllerNo, String doorNo) {
        try {
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("controllerno", controllerNo);
            dataMap.put("doorno", doorNo);
            String dataStr = JSONObject.toJSONString(dataMap);
            Object data = JSONObject.parse(dataStr); //从String转换成Object类型 "data":{}与"data":"{}" 区别
            // 获取当前日期时间
            LocalDateTime currentDateTime = LocalDateTime.now();
            // 定义日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 格式化当前日期时间为指定格式的字符串
            String nowDateTime = currentDateTime.format(formatter);
            Map<String, Object> validateMap = new HashMap<String, Object>();
            validateMap.put("from", "UT");// caller
            validateMap.put("timestamp", nowDateTime);// timestemp
            validateMap.put("nonce", String.valueOf(randomMath()));// nonce
            String signing = TokenGenerateUtil.signing(validateMap);
            log.info("门禁开门signing: {}", signing);
            signing = signing + data;  //此处追加data进行加密
            String sign = TokenGenerateUtil.encode(signing, privateKey);
            validateMap.put("sign", sign);
            validateMap.put("branchno", "1");
            validateMap.put("queryFormat", null);
            validateMap.put("data", data);
            String postString = JSONObject.toJSONString(validateMap);
            log.info("开门请求参数：{}", postString);
            String result = TokenGenerateUtil.sendPost(doorOpenUrl, postString);
            log.info("开门结果集：{}", result);
            JSONObject jsonObject = (JSONObject) JSONPath.eval(result, "$.data");
            Integer flag = jsonObject.getInteger("flag");
            if (flag == 1) {
                return true;
            }
        } catch (Exception e) {
            log.error("开门结果异常", e);
        }
        return false;
    }

    /**
     * @description 门禁列表
     * @author LianYanFei
     * @date 2023/7/28
     */
    public List<DoorStateDto> doorStateList() {
        try {
            // 获取当前日期时间
            LocalDateTime currentDateTime = LocalDateTime.now();
            // 定义日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 格式化当前日期时间为指定格式的字符串
            String nowDateTime = currentDateTime.format(formatter);
            Map<String, Object> validateMap = new HashMap<String, Object>();
            validateMap.put("from", "UT");// caller
            validateMap.put("timestamp", nowDateTime);// timestemp
            validateMap.put("nonce", String.valueOf(randomMath()));// nonce
            String signing = TokenGenerateUtil.signing(validateMap);
            log.info("门禁列表signing:{}", signing);
            String sign = TokenGenerateUtil.encode(signing, privateKey);
            validateMap.put("sign", sign);
            validateMap.put("branchno", "1");
            validateMap.put("queryFormat", null);
            validateMap.put("data", null);
            String postString = JSONObject.toJSONString(validateMap);
            log.info("门禁门状态列表请求参数：{}", postString);
            String result = TokenGenerateUtil.sendPost(doorStatusUrl, postString);
            log.info("门禁门状态列表结果集：{}", result);
            if (StringUtils.isNotBlank(result)) {
                DoorStateResultDto doorStateResultDto = JSON.parseObject(result, DoorStateResultDto.class);
                List<DoorStateDto> doorStateDtoList = doorStateResultDto.getData();
                return doorStateDtoList;
            }
        } catch (Exception e) {
            log.error("门禁门状态列表结果集异常", e);
        }
        return null;
    }

    public int randomMath() {
        // 创建一个Random对象
        Random random = new Random();

        // 生成0到99之间的随机数
        int randomNumber = random.nextInt(100); // 0到99的范围，不包括100

        return randomNumber;
    }
}
