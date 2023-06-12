package com.wanda.epc;

import com.alibaba.fastjson.JSON;
import com.netsdk.common.Res;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.module.LoginModule;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.wanda.epc.device.BaseDevice;
import com.wanda.epc.device.CommonDevice;
import com.wanda.epc.param.DeviceMessage;
import com.wanda.epc.util.ConvertUtil;
import com.wanda.epc.util.PingUtil;
import com.wanda.epc.vto.AlarmAccessDataCB;
import com.wanda.epc.vto.DefaultDisConnect;
import com.wanda.epc.vto.DefaultHaveReconnect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.*;
import java.util.*;

/**
 * @author LianYanFei
 * @version 1.0
 * @project iot-epc-module
 * @description 大华门禁设备
 * @date 2023/4/11 09:55:13
 */
@Slf4j
@Service
public class DaHuaDoorDevice extends BaseDevice {


    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    private CommonDevice commonDevice;


    List<NetSDKLib.LLong> LoginHandleList = new ArrayList<>();

    HashMap<NetSDKLib.LLong, String> loginMap = new HashMap<>();

    HashMap<String, NetSDKLib.LLong> userMap = new HashMap<>();


    Set<String> ipSet = new HashSet<>();


    @Value("${m_strUser}")
    private String m_strUser;

    @Value("${m_strPassword}")
    private String m_strPassword;

    @Value("${m_nPort}")
    private int m_nPort;


    @PostConstruct
    public void init() {
        loginMap.clear();
        userMap.clear();
        LoginModule.init(DefaultDisConnect.GetInstance(), DefaultHaveReconnect.getINSTANCE());
        deviceParamMap.entrySet().forEach(key -> {
            String ip = key.getKey();
            String[] param = ip.split("_");
            String doorIp = param[0];
            ipSet.add(doorIp);
        });
        devicesLogin(ipSet);
    }

    @Override
    public void sendMessage(DeviceMessage dm) {
        if (dm != null) {
            commonDevice.sendMessage(dm);
        }
    }

    @Override
    public boolean processData() throws Exception {
        Queue<String> allIp = new LinkedList<String>();
        deviceParamMap.entrySet().forEach(entry -> {
            List<String> ipList = Arrays.asList(entry.getKey().split("_"));
            if (ipList.size() == 2) {
                String online = ipList.get(1);
                if ("onlineStatus".equals(online)) {
                    allIp.offer(ipList.get(0));
                }
            }
        });
        ping(allIp);
        doorStatus();
        return true;
    }

    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        log.info("接收到门禁指令下发：{},状态：{}", JSON.toJSONString(deviceMessage), value);
        if (deviceMessage != null) {
            String outParamId = deviceMessage.getOutParamId();
            String[] param = outParamId.split("_");
            String ip = param[0];
            String doorNum = param[1];
            NetSDKLib.LLong userId = userMap.get(ip);
            //开
            if (value.equals("1.0")) {
                openDoor(userId, Integer.valueOf(doorNum));
                //关门
            } else {
                closeDoor(userId, Integer.valueOf(doorNum));
            }
        }
        commonDevice.feedback(message);
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    /**
     * 开始监听
     * @param loginHandler
     * @return
     */
    private boolean startListen(NetSDKLib.LLong loginHandler) {

        LoginModule.netsdk.CLIENT_SetDVRMessCallBack(AlarmAccessDataCB.getInstance(), null);

        if (!LoginModule.netsdk.CLIENT_StartListenEx(loginHandler)) {
            JOptionPane.showMessageDialog(null, ToolKits.getErrorCodeShow(),
                    Res.string().getErrorMessage(), JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            System.out.println("CLIENT_StartListenEx success.");
        }
        return true;
    }

    /**
     * 停止监听
     * @param loginHandler
     * @return
     */
    private boolean stopListen(NetSDKLib.LLong loginHandler) {
        if (!LoginModule.netsdk.CLIENT_StopListen(loginHandler)) {
            JOptionPane.showMessageDialog(null, Res.string().getStopListenFailed() + "," + ToolKits.getErrorCodeShow(),
                    Res.string().getErrorMessage(), JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            System.out.println("CLIENT_StopListen success.");
        }
        return true;
    }

    /**
     * 开门
     */
    private void openDoor(NetSDKLib.LLong m_hLoginHandle, int channelId) {
        NetSDKLib.NET_CTRL_ACCESS_OPEN openInfo = new NetSDKLib.NET_CTRL_ACCESS_OPEN();
        openInfo.nChannelID = channelId;
        openInfo.emOpenDoorType = NetSDKLib.EM_OPEN_DOOR_TYPE.EM_OPEN_DOOR_TYPE_REMOTE;
        Pointer pointer = new Memory(openInfo.size());
        ToolKits.SetStructDataToPointer(openInfo, pointer, 0);
        boolean ret = LoginModule.netsdk.CLIENT_ControlDeviceEx(m_hLoginHandle,
                NetSDKLib.CtrlType.CTRLTYPE_CTRL_ACCESS_OPEN, pointer, null, 10000);
        if (!ret) {
            log.error("远程开门失败");
        }
    }


    /**
     * 关门
     */
    private void closeDoor(NetSDKLib.LLong m_hLoginHandle, int channelId) {
        final NetSDKLib.NET_CTRL_ACCESS_CLOSE close = new NetSDKLib.NET_CTRL_ACCESS_CLOSE();
        close.nChannelID = channelId; // 对应的门编号 - 如何开全部的门
        close.write();
        boolean result = LoginModule.netsdk.CLIENT_ControlDeviceEx(m_hLoginHandle,
                NetSDKLib.CtrlType.CTRLTYPE_CTRL_ACCESS_CLOSE, close.getPointer(), null, 5000);
        close.read();
        if (!result) {
            log.error("远程关门失败");
        }
    }


    /**
     * 门禁状态
     */
    private void doorStatus() {
        int cmd = NetSDKLib.NET_DEVSTATE_DOOR_STATE;
        NetSDKLib.NET_DOOR_STATUS_INFO doorStatus = new NetSDKLib.NET_DOOR_STATUS_INFO();
        IntByReference reference = new IntByReference(0);
        doorStatus.write();
        //查询对应门状态
        LoginHandleList.forEach(handle -> {
            boolean result = LoginModule.netsdk.CLIENT_QueryDevState(handle, cmd, doorStatus.getPointer(), doorStatus.size(), reference, 3000);
            doorStatus.read();
            if (!result) {
                log.error("查询门禁状态失败：{}", Integer.toHexString(LoginModule.netsdk.CLIENT_GetLastError()));
            }
            String stateType[] = {"未知", "门打开", "门关闭", "门异常打开"};
            NetSDKLib.LLong myKey = new NetSDKLib.LLong(handle.longValue());
            log.info("查询门禁状态用户ID：{},门禁状态：{}，param:{}", handle, stateType[doorStatus.emStateType], loginMap.get(myKey) + "_" + doorStatus.nChannel + "_openStatus");
            List<DeviceMessage> deviceMessageList = deviceParamListMap.get(loginMap.get(myKey) + "_" + doorStatus.nChannel + "_openStatus");
            if (!CollectionUtils.isEmpty(deviceMessageList)) {
                deviceMessageList.forEach(deviceMessage -> {
                    if ("门打开".equals(stateType[doorStatus.emStateType])) {
                        if (Objects.nonNull(deviceMessage)) {
                            deviceMessage.setValue("1");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("门禁设备采集发送门打开状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                        }
                    }
                    if ("门关闭".equals(stateType[doorStatus.emStateType])) {
                        if (Objects.nonNull(deviceMessage)) {
                            deviceMessage.setValue("0");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("门禁设备采集发送门开状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                        }
                    }
                    log.info("doorStatus-Channel:{},type:{}", doorStatus.nChannel, stateType[doorStatus.emStateType]);
                });
            }
        });
    }


    /**
     * 设备登录
     */
    private void devicesLogin(Set<String> devices) {
        log.info("登录信息：{}", devices.size());
        for (String ip : devices) {
            try {
                NetSDKLib.LLong loginHandle = LoginModule.login(ip, m_nPort, m_strUser, m_strPassword);
                boolean login = loginHandle.longValue() == 0 ? false : true;
                if (login) {
                    log.info("设备ip:{}登录成功 账号：{} 密码：{} 端口：{} 用户ID：{}", ip, m_strUser, m_strPassword, m_nPort, loginHandle);
                    NetSDKLib.LLong myKey = new NetSDKLib.LLong(loginHandle.longValue());
                    LoginHandleList.add(myKey);
                    loginMap.put(myKey, ip);
                    userMap.put(ip, myKey);
                    startListen(loginHandle);
                } else {
                    log.info("设备ip:{}登录失败 账号：{} 密码：{} 端口：{}", ip, m_strUser, m_strPassword, m_nPort);
                }
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 设备退出
     */
    private void devicesLogOut() {
        LoginHandleList.forEach(handle -> {
            try {
                LoginModule.logout(handle);
                stopListen(handle);
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 销毁
     */
    @PreDestroy
    public void destroy() {
        log.info("SDK实例销毁!");
        devicesLogOut();
        LoginModule.cleanup();

    }


    /**
     * ping
     * @param allIp
     */
    private void ping(Queue<String> allIp) {
        log.info("门禁设备开始采集在线离线状态,ip数量{}", allIp.size());
        PingUtil pingUtil = new PingUtil(allIp);
        pingUtil.setIpsOK("");
        pingUtil.setIpsNO("");
        pingUtil.startPing();
        String ipsOK = pingUtil.getIpsOK();
        String ipsNo = pingUtil.getIpsNO();
        if (StringUtils.isNotBlank(ipsOK)) {
            List<String> ipList = Arrays.asList(ipsOK.split(","));
            log.info("门禁在线状态数量：{}", ipList.size());
            ipList.forEach(ip -> {
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(ip.concat("_onlineStatus"));
                if (!CollectionUtils.isEmpty(deviceMessageList)) {
                    deviceMessageList.forEach(deviceMessage -> {
                        if (deviceMessage != null) {
                            deviceMessage.setValue("1");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("门禁设备采集发送在线状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                            log.info("门禁设备在线状态为在线" + "ip为" + ip);
                        }
                    });
                }
            });
        }
        if (StringUtils.isNotBlank(ipsNo)) {
            List<String> ipList = Arrays.asList(ipsNo.split(","));
            log.info("门禁离线状态数量：{}", ipList.size());
            Queue<String> queue = new LinkedList<String>();
            ipList.forEach(ip -> {
                queue.offer(ip);
                reload(queue);
            });
        }
    }

    /**
     * 重试
     * @param allIp
     */
    private void reload(Queue<String> allIp) {
        log.info("开始进行设备ip ping重试=============》");
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
                log.info("重试离线param:{}", ip.concat("_onlineStatus"));
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(ip.concat("_onlineStatus"));
                if (!CollectionUtils.isEmpty(deviceMessageList)) {
                    deviceMessageList.forEach(deviceMessage -> {
                        if (deviceMessage != null) {
                            deviceMessage.setValue("1");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("重试设备采集发送在线状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                        }
                    });
                }
            });
        }
        if (StringUtils.isNotBlank(ipsNo)) {
            List<String> ipList = Arrays.asList(ipsNo.split(","));
            log.info("重试离线状态数量：{}", ipList.size());
            ipList.forEach(ip -> {
                log.info("重试离线param:{}", ip.concat("_onlineStatus"));
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(ip.concat("_onlineStatus"));
                if (!CollectionUtils.isEmpty(deviceMessageList)) {
                    deviceMessageList.forEach(deviceMessage -> {
                        if (deviceMessage != null) {
                            deviceMessage.setValue("0");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("重试设备采集发送离线状态数据：{}", JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                        }
                    });
                }
            });
        }
    }
}
