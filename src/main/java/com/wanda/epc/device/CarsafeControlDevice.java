package com.wanda.epc.device;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.wanda.epc.device.dto.DoorStateDto;
import com.wanda.epc.device.dto.DoorStateResultDto;
import com.wanda.epc.device.utils.TokenGenerateUtil;
import com.wanda.epc.param.DeviceMessage;
import com.wanda.epc.util.ConvertUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author LianYanFei
 * @description 车安门禁子系统对接
 * @date 2023/7/27
 */
@Slf4j
@Service
public class CarsafeControlDevice extends BaseDevice {

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
        List<DeviceMessage> deviceMessageList = deviceParamListMap.get(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat("_onlineStatus"));
        deviceMessageList.forEach(deviceMessage -> {
            if (deviceMessage != null) {
                deviceMessage.setValue(doorStateDto.getConnected());
                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessage);
                log.info("采集发送在离线状态数据：{}", JSON.toJSONString(deviceMessage));
            }
        });
    }

    /**
     * @description 开关状态
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void openStatus(DoorStateDto doorStateDto) {
        List<DeviceMessage> deviceMessageList = deviceParamListMap.get(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat("_openStatus"));
        deviceMessageList.forEach(deviceMessage -> {
            if (deviceMessage != null) {
                if (doorStateDto.getOpened().equals("1") || doorStateDto.getOpened().equals("2")) {
                    deviceMessage.setValue("1");
                } else {
                    deviceMessage.setValue("0");
                }
                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessage);
                log.info("采集发送在开关状态数据：{}", JSON.toJSONString(deviceMessage));
            }
        });
    }

    /**
     * @description 故障状态
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void faultStatus(DoorStateDto doorStateDto) {
        List<DeviceMessage> deviceMessageList = deviceParamListMap.get(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat("_faultStatus"));
        deviceMessageList.forEach(deviceMessage -> {
            if (deviceMessage != null) {
                if (doorStateDto.getConnected().equals("0")) {
                    deviceMessage.setValue("1");
                } else {
                    deviceMessage.setValue("0");
                }
                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessage);
                log.info("采集发送故障状态数据：{}", JSON.toJSONString(deviceMessage));
            }
        });
    }

    /**
     * @description 防拆报警
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void tamperAlarm(DoorStateDto doorStateDto) {
        List<DeviceMessage> deviceMessageList = deviceParamListMap.get(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat("_tamperAlarm"));
        deviceMessageList.forEach(deviceMessage -> {
            if (deviceMessage != null) {
                if (doorStateDto.getBroken().equals("1")) {
                    deviceMessage.setValue("1");
                } else {
                    deviceMessage.setValue("0");
                }
                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessage);
                log.info("采集发送防拆报警数据：{}", JSON.toJSONString(deviceMessage));
            }
        });
    }

    /**
     * @description 超时未关门报警
     * @author LianYanFei
     * @date 2023/7/28
     */
    public void openDoorOverTimeAlarm(DoorStateDto doorStateDto) {
        List<DeviceMessage> deviceMessageList = deviceParamListMap.get(doorStateDto.getControllerno().concat("_").concat(doorStateDto.getDoorno()).concat("_openDoorOverTimeAlarm"));
        deviceMessageList.forEach(deviceMessage -> {
            if (deviceMessage != null) {
                if (doorStateDto.getOpenedtimeout().equals("1")) {
                    deviceMessage.setValue("1");
                } else {
                    deviceMessage.setValue("0");
                }
                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessage);
                log.info("采集发送超时未关门报警数据：{}", JSON.toJSONString(deviceMessage));
            }
        });
    }


    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        DeviceMessage deviceMessage = super.controlParamMap.get(meter + "-" + funcid);
        if (deviceMessage != null && deviceMessage.getOutParamId() != null && deviceMessage.getOutParamId().endsWith("control")) {
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
                    List<DeviceMessage> deviceMessageList = deviceParamListMap.get(controllerNo.concat("_").concat(doorNo).concat("_openStatus"));
                    deviceMessageList.forEach(deviceMessage1 -> {
                        if (deviceMessage1 != null) {
                            deviceMessage1.setValue("1");
                            deviceMessage1.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            sendMessage(deviceMessage1);
                            log.info("开门发送开关状态数据：{}", JSON.toJSONString(deviceMessage1));
                        }
                    });
                }
            }
        }
        commonDevice.feedback(message);
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
            String eval = (String) JSONPath.eval(result, "$.flag");
            log.info("开门结果集：{}", result);
            if (eval.equals("1")) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
