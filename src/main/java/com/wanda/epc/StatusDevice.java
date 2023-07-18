package com.wanda.epc;

import com.alibaba.fastjson.JSON;
import com.wanda.epc.device.BaseDevice;
import com.wanda.epc.device.CommonDevice;
import com.wanda.epc.param.DeviceMessage;
import com.wanda.epc.util.ConvertUtil;
import com.wanda.epc.util.PingUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author LianYanFei
 * @version 1.0
 * @project iot_epc
 * @description 门禁在线状态
 * @date 2023/1/15 16:29:54
 */
@Slf4j
@Service
public class StatusDevice extends BaseDevice {



    @Autowired
    CommonDevice commonDevice;


    @Override
    public void sendMessage(DeviceMessage dm) {
        if (dm != null) {
            commonDevice.sendMessage(dm);
        }
    }


    @Override
    public boolean processData() throws Exception {
        //当前时间
        Date beginTime = new Date();
        //当前时间 转为 长整型 Long
        Long begin = beginTime.getTime();
        Queue<String> nvrIp = new LinkedList<String>();
        deviceParamMap.entrySet().forEach(entry -> {
            List<String> ipList = Arrays.asList(entry.getKey().split("_"));
            if (ipList.size() == 2) {
                nvrIp.offer(ipList.get(0));
            }
        });
        //nvr设备
        nvrPing(nvrIp);
        //摄像头
        // ping(allIp);
        //获取结束时间
        Date finishTime = new Date();
        //结束时间 转为 Long 类型
        Long end = finishTime.getTime();
        long timeLag = end - begin;
        //天
        long day = timeLag / (24 * 60 * 60 * 1000);
        //小时
        long hour = (timeLag / (60 * 60 * 1000) - day * 24);
        long minute = ((timeLag / (60 * 1000)) - day * 24 * 60 - hour * 60);
        log.info("数据采集共执行{}分钟=========", minute);
        return true;
    }

    private void nvrPing(Queue<String> allIp) {
        log.info("设备开始采集在线离线状态,ip数量{}", allIp.size());
        PingUtil pingUtil = new PingUtil(allIp);
        pingUtil.setIpsOK("");
        pingUtil.setIpsNO("");
        pingUtil.startPing();
        String ipsOK = pingUtil.getIpsOK();
        String ipsNo = pingUtil.getIpsNO();
        if (StringUtils.isNotBlank(ipsOK)) {
            List<String> ipList = Arrays.asList(ipsOK.split(","));
            log.info("在线状态数量：{}", ipList.size());
            ipList.forEach(ip -> {
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(ip.concat("_onlineStatus"));
                if (!CollectionUtils.isEmpty(deviceMessageList)) {
                    deviceMessageList.forEach(deviceMessage -> {
                        if (deviceMessage != null) {
                            deviceMessage.setValue("1");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("设备采集发送在线状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                            log.info("设备在线状态为在线" + "ip为" + ip);
                        }
                    });
                }
            });
        }
        if (StringUtils.isNotBlank(ipsNo)) {
            List<String> ipList = Arrays.asList(ipsNo.split(","));
            log.info("离线状态数量：{}", ipList.size());
            Queue<String> queue = new LinkedList<String>();
            ipList.forEach(ip -> {
                queue.offer(ip);
                reload(queue, "1");
            });
        }
    }


    public void reload(Queue<String> allIp, String type) {
        log.info("开始进行设备ip ping重试=============》");
        //1.nvr设备 2.摄像头设备
        if ("1".equals(type)) {
            log.info("开始重试设备离线数据");
        }
        PingUtil pingUtil = new PingUtil(allIp);
        pingUtil.setIpsOK("");
        pingUtil.setIpsNO("");
        pingUtil.startPing();
        String ipsOK = pingUtil.getIpsOK();
        String ipsNo = pingUtil.getIpsNO();
        if (StringUtils.isNotBlank(ipsOK)) {
            List<String> ipList = Arrays.asList(ipsOK.split(","));
            log.info("重试在线状态数量：{}", ipList.size());
            ipList.forEach(ip -> {
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(ip.concat("_onlineStatus"));
                if (!CollectionUtils.isEmpty(deviceMessageList)) {
                    deviceMessageList.forEach(deviceMessage -> {
                        if (deviceMessage != null) {
                            deviceMessage.setValue("1");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("重试设备采集发送在线状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                            log.info("重试设备在线状态为在线" + "ip为" + ip);
                        }
                    });
                }
            });
        }
        if (StringUtils.isNotBlank(ipsNo)) {
            List<String> ipList = Arrays.asList(ipsNo.split(","));
            log.info("重试离线状态数量：{}", ipList.size());
            ipList.forEach(ip -> {
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(ip.concat("_onlineStatus"));
                if (!CollectionUtils.isEmpty(deviceMessageList)) {
                    deviceMessageList.forEach(deviceMessage -> {
                        if (deviceMessage != null) {
                            deviceMessage.setValue("0");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("重试设备采集发送离线状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                            log.info("重试设备在线状态为离线" + "ip为" + ip);
                        }
                    });
                }
            });
        }
    }


    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) {
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }


}
