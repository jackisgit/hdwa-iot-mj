package com.wanda.epc.device;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONPath;
import com.google.common.collect.Lists;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: iot_epc
 * @description: 海康门禁子系统采集
 * @author: 孙率众
 * @create: 2022-10-11 16:56
 **/
@Slf4j
@Service
public class AccessControlDevice extends BaseDevice {

    public static final String TAMPER_ALARM = "_tamperAlarm";
    public static final String OPEN_TIMEOUT_ALARM = "_openTimeoutAlarm";
    public static final String ONLINE_STATUS = "_onlineStatus";
    public static final String DOOR_STATUS = "_doorStatus";
    //设备防拆报警
    private static Integer tamperAlarm = 66305;
    //开门超时
    private static Integer openTimeoutAlarm = 198400;
    @Value("${access.appKey}")
    public String appKey;
    @Value("${access.secret}")
    public String secret;
    @Autowired
    CommonDevice commonDevice;
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
                String outParam = deviceStatus.getDoorUuid() + DOOR_STATUS;
                sendMsg(outParam, deviceStatus.getDoorStatus());
                //门禁在线状态
                String outParamOnline = deviceStatus.getDoorUuid() + ONLINE_STATUS;
                sendMsg(outParamOnline, "1");
            });
        });

        //4.获取历史事件
        List<DoorEventDto> doorEvents = getDoorEvents(userUid);
        for (DoorEventDto doorEventDto : doorEvents) {
            //设备防拆报警
            if (doorEventDto.getDeviceType().equals(tamperAlarm)) {
                String outParamOnline = doorEventDto.getDoorUuid() + TAMPER_ALARM;
                sendMsg(outParamOnline, "1");
                //删除报警的设备
                doorUuid.remove(doorEventDto.getDoorUuid());

            }
            //开门超时报警
            if (doorEventDto.getDeviceType().equals(openTimeoutAlarm)) {
                String outParamOnline = doorEventDto.getDoorUuid() + OPEN_TIMEOUT_ALARM;
                sendMsg(outParamOnline, "1");
                doorUuid.remove(doorEventDto.getDoorUuid());
            }
        }
        for (String uuid : doorUuid) {
            sendMsg(uuid + TAMPER_ALARM, "0");
            sendMsg(uuid + OPEN_TIMEOUT_ALARM, "0");
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
     */
    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws InterruptedException {
        commonDevice.feedback(message);
        //同步反控门禁点
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        log.info("收到{}的控制请求{}", meter + "-" + funcid, deviceMessage);
        if (deviceMessage != null) {
            String outParamId = deviceMessage.getOutParamId();
            String[] UUIDSplit = outParamId.split("_");
            String uuid = UUIDSplit[0];
            int v = Double.valueOf(value).intValue();
            String userUid = getUserUid();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(com.wanda.epc.device.CollectConstant.MJ_APPKEY, appKey);
            map.put(CollectConstant.MJ_TIME, System.currentTimeMillis());
            map.put(CollectConstant.MJ_USER_UUID, userUid);
            map.put(CollectConstant.MJ_DOOR_UUID, uuid);
            map.put(CollectConstant.MJ_COMMAND, v);
            String params = JSON.toJSONString(map);
            log.info("分页获取门禁点 请求参数：【" + params + "】");
            String token = TokenGenerateUtil.buildToken(syncContolUrl + "?" + params, null, secret);
            log.info("获取token：【" + token + "】");
            String result = HttpClientUtil.postJson(syncContolUrl + "?token=" + token, null, null, params);
            log.info("控制门禁：【" + uuid + "设备结果：" + result + "】");
            processSingleDoorStatus(uuid, userUid);
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }


    @Override
    public void sendMessage(DeviceMessage dm) {
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
        map.put(CollectConstant.MJ_APPKEY, appKey);
        map.put(CollectConstant.MJ_TIME, System.currentTimeMillis());
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
        map.put(CollectConstant.MJ_APPKEY, appKey);
        map.put(CollectConstant.MJ_TIME, System.currentTimeMillis());
        map.put(CollectConstant.MJ_PAGE_NO, 1);
        map.put(CollectConstant.MJ_PAGE_SIZE, 500);
        map.put(CollectConstant.MJ_USER_UUID, userUuid);
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
        map.put(CollectConstant.MJ_APPKEY, appKey);
        map.put(CollectConstant.MJ_TIME, System.currentTimeMillis());
        map.put(CollectConstant.MJ_USER_UUID, userUuid);
        map.put(CollectConstant.MJ_DOOR_UUIDS, doorUidList);
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
        map.put(CollectConstant.MJ_APPKEY, appKey);
        map.put(CollectConstant.MJ_TIME, System.currentTimeMillis());
        map.put(CollectConstant.MJ_PAGE_NO, 1);
        map.put(CollectConstant.MJ_PAGE_SIZE, 50);//一分钟查一次50条，预估可以满足需求
        map.put(CollectConstant.MJ_USER_UUID, userUuid);
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
        map.put(CollectConstant.MJ_APPKEY, appKey);
        map.put(CollectConstant.MJ_TIME, System.currentTimeMillis());
        map.put(CollectConstant.MJ_USER_UUID, userUuid);
        map.put(CollectConstant.MJ_DOOR_UUIDS, doorUidList);
        String params = JSON.toJSONString(map);
        String token = TokenGenerateUtil.buildToken(doorStatusUrl + "?" + params, null, secret);
        String result = HttpClientUtil.postJson(doorStatusUrl + "?token=" + token, null, null, params);
        JSONArray list = (JSONArray) JSONPath.eval(result, "$.data");
        List<DeviceStatusDto> deviceStatusList = list.toJavaList(DeviceStatusDto.class);
        deviceStatusList.forEach(deviceStatus -> {
            String outParam = deviceStatus.getDoorUuid() + DOOR_STATUS;
            sendMsg(outParam, deviceStatus.getDoorStatus());
        });
        return deviceStatusList;
    }

}
