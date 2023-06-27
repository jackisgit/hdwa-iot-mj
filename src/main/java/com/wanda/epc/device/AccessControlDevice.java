package com.wanda.epc.device;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONPath;
import com.google.common.collect.Lists;
import com.wanda.epc.device.constant.AccessConstant;
import com.wanda.epc.device.dto.DeviceStatusDto;
import com.wanda.epc.device.dto.DoorEventDto;
import com.wanda.epc.device.dto.DoorsDto;
import com.wanda.epc.device.utils.HttpClientUtil;
import com.wanda.epc.device.utils.TokenGenerateUtil;
import com.wanda.epc.param.DeviceMessage;
import com.wanda.epc.util.ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: iot_epc
 * @description: 海康门禁子系统采集
 * @author: LianYanFei
 * @create: 2022-10-11 16:56
 **/
@Slf4j
@Service
public class AccessControlDevice extends BaseDevice {

    @Value("${access.userInfoUrl}")
    private String userInfoUrl;

    @Value("${access.doorsUrl}")
    private String doorsUrl;

    @Value("${access.doorStatusUrl}")
    private String doorStatusUrl;

    @Value("${access.doorEventsUrl}")
    private String doorEventsUrl;

    @Value("${access.syncContolUrl}")
    private String syncContolUrl;

    @Value("${access.appKey}")
    public String appKey;

    @Value("${access.secret}")
    public String secret;

    //设备防拆报警
    private static Integer tamperAlarm = 66305;

    //开门超时
    private static Integer openTimeoutAlarm = 198400;

    @Autowired
    CommonDevice commonDevice;

    @Override
    public boolean processData() {
        log.info("海康威视门禁子系统采集开始");
        //1.获取用户uuid
        String userUid = getUserUid();
        //2.获取所有门禁点uuid
        List<String> doorUuid = getDoors(userUid);

        List<List<String>> partition = Lists.partition(doorUuid, 50);
        partition.forEach(list -> {
            String doorUuidList = StringUtils.join(list, ",");
            //3.获取门禁状态
            List<DeviceStatusDto> doorStatus = getDoorStatus(doorUuidList, userUid);
            doorStatus.forEach(deviceStatus -> {
                String outParam = deviceStatus.getDoorUuid() + "_doorStatus";
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(outParam);
                if (!CollectionUtils.isEmpty(deviceMessageList)) {
                    for (DeviceMessage deviceMessage : deviceMessageList) {
                        if (deviceMessage != null && StringUtils.isNotEmpty(deviceStatus.getDoorStatus())) {
                            if (deviceMessage.getParamName().equals("runStatus")) {
                                String runStatus = deviceStatus.getDoorStatus().equals("2") ? "0" : "1";
                                deviceMessage.setValue(runStatus);
                                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                                sendMessage(deviceMessage);
                            } else {
                                deviceMessage.setValue(deviceStatus.getDoorStatus());
                                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                                sendMessage(deviceMessage);
                            }
                        } else {
                            log.info("门禁地址为空：{}", outParam);
                        }
                    }
                }
                //门禁在线状态
                String outParamOnline = deviceStatus.getDoorUuid() + "_onlineStatus";
                if (deviceStatus.getDeviceUuid().equals("84d1945e4f024653b20a6b23f4015158")) {
                    System.out.println(1);
                }
                List<DeviceMessage> deviceMessageOnlineList = deviceParamListMap.get(outParamOnline);
                if (!CollectionUtils.isEmpty(deviceMessageOnlineList)) {
                    for (DeviceMessage deviceMessageOnline : deviceMessageOnlineList) {
                        if (deviceMessageOnline != null) {
                            deviceMessageOnline.setValue("1");
                            deviceMessageOnline.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            sendMessage(deviceMessageOnline);
                            log.info("在线态采集：{}", outParam);
                        }
                    }
                }
            });
//            log.info("门禁对象信息：{}", doorStatus);
        });

        //4.获取历史事件
        List<DoorEventDto> doorEvents = getDoorEvents(userUid);
        for (String uuid : doorUuid) {

        }

        for (DoorEventDto doorEventDto : doorEvents) {
            //设备防拆报警
            if (doorEventDto.getDeviceType() == tamperAlarm) {
                String outParamOnline = doorEventDto.getDoorUuid() + "_tamperAlarm";
                DeviceMessage deviceMessageOnline = deviceParamMap.get(outParamOnline);
                if (deviceMessageOnline != null) {
                    deviceMessageOnline.setValue("1");
                    deviceMessageOnline.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                    sendMessage(deviceMessageOnline);
                    log.info("采集到门禁{}产生{}", doorEventDto.getDoorUuid(), "设备防拆报警");
                }
                //删除报警的设备
                doorUuid.remove(doorEventDto.getDoorUuid());

            }
            //开门超时报警
            if (doorEventDto.getDeviceType() == openTimeoutAlarm) {
                String outParamOnline = doorEventDto.getDoorUuid() + "_openTimeoutAlarm";
                DeviceMessage deviceMessageOnline = deviceParamMap.get(outParamOnline);
                if (deviceMessageOnline != null) {
                    deviceMessageOnline.setValue("1");
                    deviceMessageOnline.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                    sendMessage(deviceMessageOnline);
                    log.info("采集到门禁{}产生{}", doorEventDto.getDoorUuid(), "开门超时报警");
                }
                doorUuid.remove(doorEventDto.getDoorUuid());
            }
        }
        for (String uuid : doorUuid) {
            DeviceMessage deviceMessageTamper = deviceParamMap.get(uuid + "_tamperAlarm");
            if (deviceMessageTamper != null) {
                deviceMessageTamper.setValue("0");
                deviceMessageTamper.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessageTamper);
            }
            DeviceMessage deviceMessageTimeout = deviceParamMap.get(uuid + "_openTimeoutAlarm");
            if (deviceMessageTimeout != null) {
                deviceMessageTimeout.setValue("0");
                deviceMessageTimeout.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessageTimeout);
            }
        }
        return true;
    }


    /**
     * 指令下发 反控命令 0:常开 1:关门 2:开门 3:常关
     * TODO 不完善待补充
     *
     * @param meter
     * @param funcid
     * @param value
     * @return
     */
    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws InterruptedException {
        commonDevice.feedback(message);
        //同步反控门禁点
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        log.info("收到{}的控制请求{}", meter + "-" + funcid, deviceMessage);
        if (deviceMessage == null) {
            return;
        }
        String outParamId = deviceMessage.getOutParamId();
        String[] UUIDSplit = outParamId.split("_");
        String uuid = UUIDSplit[0];
        int v = Double.valueOf(value).intValue();
        String userUid = getUserUid();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AccessConstant.BEIJING_CBD_MJ_APPKEY, appKey);
        map.put(AccessConstant.BEIJING_CBD_MJ_TIME, System.currentTimeMillis());
        map.put(AccessConstant.BEIJING_CBD_MJ_USER_UUID, userUid);
        map.put(AccessConstant.BEIJING_CBD_MJ_DOOR_UUID, uuid);
        map.put(AccessConstant.BEIJING_CBD_MJ_COMMAND, v);
        String params = JSON.toJSONString(map);
        log.info("分页获取门禁点 请求参数：【" + params + "】");
        String token = TokenGenerateUtil.buildToken(syncContolUrl + "?" + params, null, secret);
        log.info("获取token：【" + token + "】");
        String result = HttpClientUtil.postJson(syncContolUrl + "?token=" + token, null, null, params);
        log.info("控制门禁：【" + uuid + "设备结果：" + result + "】");
        processSingleDoorStatus(uuid, userUid);
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }


    @Override
    public void sendMessage(DeviceMessage dm) {
        //更新redis

        //如果数据变化则，发送emqx

        if (dm != null) {
            commonDevice.sendMessage(dm);
        }
    }

    /**
     * 获取用户uuid
     *
     * @return
     */
    private String getUserUid() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AccessConstant.BEIJING_CBD_MJ_APPKEY, appKey);
        map.put(AccessConstant.BEIJING_CBD_MJ_TIME, System.currentTimeMillis());
        String params = JSON.toJSONString(map);
        String token = TokenGenerateUtil.buildToken(userInfoUrl + "?" + params, null, secret);
        String result = HttpClientUtil.postJson(userInfoUrl + "?token=" + token, null, null, params);
        String uuid = (String) JSONPath.eval(result, "$.data");
        return uuid;
    }


    /**
     * 分页获取门禁点(需要全量数据)
     *
     * @param userUuid
     * @return
     */
    private List<String> getDoors(String userUuid) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AccessConstant.BEIJING_CBD_MJ_APPKEY, appKey);
        map.put(AccessConstant.BEIJING_CBD_MJ_TIME, System.currentTimeMillis());
        map.put(AccessConstant.BEIJING_CBD_MJ_PAGE_NO, 1);
        map.put(AccessConstant.BEIJING_CBD_MJ_PAGE_SIZE, 500);
        map.put(AccessConstant.BEIJING_CBD_MJ_USER_UUID, userUuid);
        String params = JSON.toJSONString(map);
//        log.info("分页获取门禁点 请求参数：【" + params + "】");
        String token = TokenGenerateUtil.buildToken(doorsUrl + "?" + params, null, secret);
        String result = HttpClientUtil.postJson(doorsUrl + "?token=" + token, null, null, params);
//        log.info("分页获取门禁点 请求返回结果：{}", result);
        JSONArray list = (JSONArray) JSONPath.eval(result, "$.data.list");
        List<DoorsDto> deviceUuidList = list.toJavaList(DoorsDto.class);
//        log.info("==================门禁点：{}",JSON.toJSONString(deviceUuidList));
        List<String> deviceList = deviceUuidList.stream().map(dto -> dto.getDoorUuid()).collect(Collectors.toList());
//        log.info("分页获取门禁点：{}", JSON.toJSONString(deviceList));
        return deviceList;
    }


    /**
     * 获取门禁状态
     *
     * @param doorUidList
     * @param userUuid
     * @return
     */
    private List<DeviceStatusDto> getDoorStatus(String doorUidList, String userUuid) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AccessConstant.BEIJING_CBD_MJ_APPKEY, appKey);
        map.put(AccessConstant.BEIJING_CBD_MJ_TIME, System.currentTimeMillis());
        map.put(AccessConstant.BEIJING_CBD_MJ_USER_UUID, userUuid);
        map.put(AccessConstant.BEIJING_CBD_MJ_DOOR_UUIDS, doorUidList);
        String params = JSON.toJSONString(map);
//        log.info("获取门禁设备状态 请求参数：【" + params + "】");
        String token = TokenGenerateUtil.buildToken(doorStatusUrl + "?" + params, null, secret);
        String result = HttpClientUtil.postJson(doorStatusUrl + "?token=" + token, null, null, params);
//        log.info("门禁设备状态 请求返回结果：{}", result);
        JSONArray list = (JSONArray) JSONPath.eval(result, "$.data");
        List<DeviceStatusDto> deviceStatusList = list.toJavaList(DeviceStatusDto.class);
//        log.info("门禁设备状态：{}", JSON.toJSONString(deviceStatusList));
        return deviceStatusList;
    }


    /**
     * 获取门禁事件
     *
     * @param userUuid
     * @return
     */
    private List<DoorEventDto> getDoorEvents(String userUuid) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AccessConstant.BEIJING_CBD_MJ_APPKEY, appKey);
        map.put(AccessConstant.BEIJING_CBD_MJ_TIME, System.currentTimeMillis());
        map.put(AccessConstant.BEIJING_CBD_MJ_PAGE_NO, 1);
        map.put(AccessConstant.BEIJING_CBD_MJ_PAGE_SIZE, 50);//一分钟查一次50条，预估可以满足需求
        map.put(AccessConstant.BEIJING_CBD_MJ_USER_UUID, userUuid);
        String params = JSON.toJSONString(map);
        log.info("获取门禁设事件 请求参数：【" + params + "】");
        String token = TokenGenerateUtil.buildToken(doorEventsUrl + "?" + params, null, secret);
        String result = HttpClientUtil.postJson(doorEventsUrl + "?token=" + token, null, null, params);
        log.info("门禁事件 请求返回结果：{}", result);
        JSONArray list = (JSONArray) JSONPath.eval(result, "$.data.list");
        List<DoorEventDto> eventDtoList = list.toJavaList(DoorEventDto.class);
        log.info("门禁事件：{}", JSON.toJSONString(eventDtoList));
        return eventDtoList;
    }


    /**
     * 获取单个门禁状态
     *
     * @param doorUidList
     * @param userUuid
     * @return
     */
    private List<DeviceStatusDto> processSingleDoorStatus(String doorUidList, String userUuid) throws InterruptedException {
        Thread.sleep(2000);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AccessConstant.BEIJING_CBD_MJ_APPKEY, appKey);
        map.put(AccessConstant.BEIJING_CBD_MJ_TIME, System.currentTimeMillis());
        map.put(AccessConstant.BEIJING_CBD_MJ_USER_UUID, userUuid);
        map.put(AccessConstant.BEIJING_CBD_MJ_DOOR_UUIDS, doorUidList);
        String params = JSON.toJSONString(map);
        String token = TokenGenerateUtil.buildToken(doorStatusUrl + "?" + params, null, secret);
        String result = HttpClientUtil.postJson(doorStatusUrl + "?token=" + token, null, null, params);
        JSONArray list = (JSONArray) JSONPath.eval(result, "$.data");
        List<DeviceStatusDto> deviceStatusList = list.toJavaList(DeviceStatusDto.class);
        deviceStatusList.forEach(deviceStatus -> {
            String outParam = deviceStatus.getDoorUuid() + "_doorStatus";
            DeviceMessage deviceMessage = deviceParamMap.get(outParam);
            if (deviceMessage != null && StringUtils.isNotEmpty(deviceStatus.getDoorStatus())) {
                deviceMessage.setValue(deviceStatus.getDoorStatus());
                deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                sendMessage(deviceMessage);
                log.info("{}门禁开关状态采集：{}", outParam, deviceStatus.getDoorStatus());
            }
        });
        return deviceStatusList;
    }

}
