package com.wanda.epc.device;

import com.alibaba.fastjson.JSON;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.wanda.epc.device.NetSDKDemo.HCNetSDK;
import com.wanda.epc.param.DeviceMessage;
import com.wanda.epc.util.ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;


@Slf4j
@Component
public class HikVisionDoorDevice extends BaseDevice {

    Map<Integer, String> ipMap = new HashMap<>();
    Map<String, Integer> userMap = new HashMap<>();

    Set<String> ipSet = new HashSet<>();

    List<Integer> userIdList = new ArrayList<>();

    static List<Integer> lAlarmHandleList = new ArrayList<>();

    static HCNetSDK hCNetSDK;

    static int iCharEncodeType = 0;//设备字符集
    static int lUserID = -1;//用户句柄 实现对设备登录


    @Resource
    DeviceConfig deviceConfig;

    public static HCNetSDK.FMSGCallBack_V31 fMSFCallBack_V31;


    @PostConstruct
    public void init() {
        deviceParamMap.entrySet().forEach(key -> {
            String[] param = key.getKey().split("_");
            String ip = param[0];
            ipSet.add(ip);
        });

        if (hCNetSDK == null) {
            if (!createSDKInstance()) {
                log.info("Load SDK fail");
                return;
            }
        }
        hCNetSDK.NET_DVR_Init();
        //开启SDK日志打印
        hCNetSDK.NET_DVR_SetLogToFile(3, "./sdklog", false);

//        login_V40("192.168.3.160", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.162", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.151", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.161", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.150", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.153", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.164", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.152", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.163", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.166", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.155", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.154", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.165", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.157", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.156", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.167", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.159", 8000, "admin", "hkws12345");
//        login_V40("192.168.3.158", 8000, "admin", "hkws12345");

        List<DeviceInfo> deviceList = deviceConfig.getDeviceList();
        String defaultUser = deviceList.get(0).getUser();
        String defaultPwd = deviceList.get(0).getPwd();
        Integer defaultPort = deviceList.get(0).getPort();
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            login_V40(deviceInfo.getIp(),
                    deviceInfo.getPort() != null ? deviceInfo.getPort() : defaultPort,
                    deviceInfo.getUser() != null ? deviceInfo.getUser() : defaultUser,
                    deviceInfo.getPwd() != null ? deviceInfo.getPwd() : defaultPwd);
        }
    }

    /**
     * 建立布防上传通道，用于传输数据
     *
     * @param lUserID      唯一标识符
     * @param lAlarmHandle 报警处理器
     */
    public int setupAlarmChan(int lUserID, int lAlarmHandle) {
        // 根据设备注册生成的lUserID建立布防的上传通道，即数据的上传通道
        if (lUserID == -1) {
            log.info("请先注册");
            return lUserID;
        }
        if (lAlarmHandle < 0) {
            // 设备尚未布防,需要先进行布防
            if (fMSFCallBack_V31 == null) {
                fMSFCallBack_V31 = new FMSGCallBack();
                Pointer pUser = null;
                if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                    log.info("设置回调函数失败!", hCNetSDK.NET_DVR_GetLastError());
                }
            }
            // 这里需要对设备进行相应的参数设置，不设置或设置错误都会导致设备注册失败
            HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
            // 智能交通布防优先级：0 - 一等级（高），1 - 二等级（中），2 - 三等级（低）
            m_strAlarmInfo.byLevel = 1;
            // 智能交通报警信息上传类型：0 - 老报警信息（NET_DVR_PLATE_RESULT）, 1 - 新报警信息(NET_ITS_PLATE_RESULT)
            m_strAlarmInfo.byAlarmInfoType = 1;
            // 布防类型(仅针对门禁主机、人证设备)：0 - 客户端布防(会断网续传)，1 - 实时布防(只上传实时数据)
            m_strAlarmInfo.byDeployType = 1;
            // 抓拍，这个类型要设置为 0 ，最重要的一点设置
            m_strAlarmInfo.byFaceAlarmDetection = 0;
            m_strAlarmInfo.write();
            // 布防成功，返回布防成功的数据传输通道号
            lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
            if (lAlarmHandle == -1) {
                log.info("设备布防失败，错误码=========={}", hCNetSDK.NET_DVR_GetLastError());
                // 注销 释放sdk资源
                hCNetSDK.NET_DVR_Logout(lUserID);
                lAlarmHandleList.add(lAlarmHandle);
                return lAlarmHandle;
            } else {
                log.info("设备布防成功");
                return lAlarmHandle;
            }
        }
        return lAlarmHandle;
    }

    /**
     * 报警撤防
     *
     * @param lAlarmHandle 报警处理器
     */
    public int closeAlarmChan(int lAlarmHandle) {
        if (lAlarmHandle > -1) {
            if (hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle)) {
                log.info("撤防成功");
                lAlarmHandle = -1;
                return lAlarmHandle;
            }
            return lAlarmHandle;
        }
        return lAlarmHandle;
    }


    /**
     * @description
     * @params lUserID :NET_DVR_Login_V40等登录接口的返回值
     * lGatewayIndex:门禁序号（楼层编号、锁ID），从1开始，-1表示对所有门（或者梯控的所有楼层）进行操作
     * dwStaic: 命令值：0- 关闭（对于梯控，表示受控），1- 打开（对于梯控，表示开门），2- 常开（对于梯控，表示自由、通道状态），3- 常关（对于梯控，表示禁用），4- 恢复（梯控，普通状态），5- 访客呼梯（梯控），6- 住户呼梯（梯控）
     * @retrun
     * @author LianYanFei
     * @date 2023/4/3
     */
    public static void controlGateway(Integer userId, int lGatewayIndex, int dwStaic) {
        log.info("接受到控门指令：userId：{},lGatewayIndex:{},dwStaic:{}", userId, lGatewayIndex, dwStaic);
        if (userId >= 0) {
            boolean result = hCNetSDK.NET_DVR_ControlGateway(userId, lGatewayIndex, dwStaic);
            if (result) {
                log.info("远程控门成功");
            } else {
                log.info("远程控门失败");
            }
        }
    }

    /**
     * 动态库加载
     *
     * @return
     */
    private static boolean createSDKInstance() {
        if (hCNetSDK == null) {
            synchronized (HCNetSDK.class) {
                String strDllPath = "";
                try {
                    if (osSelect.isWindows())
                        //win系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "\\lib\\HCNetSDK.dll";
                    else if (osSelect.isLinux())
                        //Linux系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "/lib/libhcnetsdk.so";
                    hCNetSDK = (HCNetSDK) Native.loadLibrary(strDllPath, HCNetSDK.class);
                } catch (Exception ex) {
                    log.info("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void sendMessage(DeviceMessage dm) {
        if (dm != null) {
            log.info("发送门禁数据：{}", JSON.toJSONString(dm));
            commonDevice.sendMessage(dm);
        }
    }

    @Override
    public boolean processData() throws Exception {
        Map<Integer, Boolean> online = isOnline();
        //门禁在离线状态
        online.entrySet().forEach(key -> {
            log.info("用户ID：{},在线状态：{}，paramId:{}", key.getKey(), key.getValue(), ipMap.get(key.getKey()).concat("_onlineStatus"));
            List<DeviceMessage> deviceMessageList = deviceParamListMap.get(ipMap.get(key.getKey()).concat("_onlineStatus"));
            log.info("获取数据：{}", JSON.toJSONString(deviceMessageList));
            if (!CollectionUtils.isEmpty(deviceMessageList)) {
                deviceMessageList.forEach(deviceMessage -> {
                    if (key.getValue()) {
                        if (Objects.nonNull(deviceMessage)) {
                            deviceMessage.setValue("1");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("发送门禁设备在线数据==={}", deviceMessage.getOutParamId(), JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                        }
                    } else {
                        if (Objects.nonNull(deviceMessage)) {
                            deviceMessage.setValue("0");
                            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                            log.info("发送门禁设备离线数据==={}", deviceMessage.getOutParamId(), JSON.toJSONString(deviceMessage));
                            sendMessage(deviceMessage);
                        }
                    }
                });
            }
        });
        //门禁开关状态
        for (Integer userId : userIdList) {
            openDoor(userId);
        }
        llegalOpenAlarm();
        openDoorOverTimeAlarm();
        return true;
    }

    public void openDoor(int lUserID) {
        // 获取门禁主机工作状态信息
        HCNetSDK.NET_DVR_ACS_WORK_STATUS_V50 acsWorkStatus = new HCNetSDK.NET_DVR_ACS_WORK_STATUS_V50();
        acsWorkStatus.dwSize = acsWorkStatus.size();
        Pointer pAcsWorkStatus = acsWorkStatus.getPointer();
        if (!hCNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_ACS_WORK_STATUS_V50, 0, pAcsWorkStatus,
                acsWorkStatus.size(), new IntByReference(0))) {
            System.out.println("Failed to get door status! Error code: " + hCNetSDK.NET_DVR_GetLastError());
            return;
        }
        acsWorkStatus.read();
        for (int i = 0; i < acsWorkStatus.byDoorLockStatus.length; i++) {
            //开关门状态，开 关等   门锁状态(继电器开合状态)，0-正常关，1-正常开，2-短路报警，3-断路报警，4-异常报警
            byte lockStatus = acsWorkStatus.byDoorLockStatus[i];
            //门的控制模式，常开 常闭等   门状态(楼层状态)，1-休眠，2-常开状态(自由)，3-常闭状态(禁用)，4-普通状态(受控)
            byte doorStatus = acsWorkStatus.byDoorStatus[i];
            //门的磁吸状态，开 闭等   门磁状态，0-正常关，1-正常开，2-短路报警，3-断路报警，4-异常报警
            byte doorMagneticStatus = acsWorkStatus.byMagneticStatus[i];
            String ip = ipMap.get(lUserID);
            String paramId = ip.concat("_").concat(String.valueOf(i)).concat("_").concat("openStatus");
            List<DeviceMessage> deviceMessageList = deviceParamListMap.get(paramId);
            if (CollectionUtils.isEmpty(deviceMessageList)) {
                continue;
            }
            log.info("用户ID：{},序号：{},门状态：{},门开关状态：{},门的磁吸状态:{}", lUserID, i, doorStatus, lockStatus, doorMagneticStatus);
            log.info("paramId:{},数据长度：{}", paramId, deviceMessageList.size());
            deviceMessageList.forEach(deviceMessage -> {
                if (Objects.nonNull(deviceMessage)) {
                    if (doorStatus == 2 || lockStatus == 1 || doorMagneticStatus == 1) {
                        deviceMessage.setValue("1");
                    } else {
                        deviceMessage.setValue("0");
                    }
                    deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                    log.info("发送门禁设备开关状态数据==={}", deviceMessage.getOutParamId(), JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            });
        }
    }

    /**
     *@description 非法开门报警
     *@author LianYanFei
     *@date 2023/4/18
     */

    public void llegalOpenAlarm() {
        deviceParamMap.entrySet().forEach(key -> {
            if (key.getKey().endsWith("_wD_IllegalOpenAlarm")) {
                log.info("非法开门报警：{}", key.getKey());
                DeviceMessage deviceMessage = deviceParamMap.get(key.getKey());
                if (Objects.nonNull(deviceMessage)) {
                    deviceMessage.setValue("0");
                    deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                    log.info("发送门禁设备非法开门报警数据==={}", deviceMessage.getOutParamId(), JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            }
        });
    }

    /**
     *@description 长时间未关门报警
     *@author LianYanFei
     *@date 2023/4/18
     */
    public void openDoorOverTimeAlarm() {
        log.info("_wD_openDoorOverTimeAlarm");
        deviceParamMap.entrySet().forEach(key -> {
            if (key.getKey().endsWith("_wD_openDoorOverTimeAlarm")) {
                log.info("长时间未关门报警：{}", key.getKey());
                DeviceMessage deviceMessage = deviceParamMap.get(key.getKey());
                if (Objects.nonNull(deviceMessage)) {
                    deviceMessage.setValue("0");
                    deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
                    log.info("发送门禁设备长时间未关门报警数据==={}", deviceMessage.getOutParamId(), JSON.toJSONString(deviceMessage));
                    sendMessage(deviceMessage);
                }
            }
        });
    }


    /**
     * 门禁常开报警
     *
     * @return
     */
    public void wD_openDoorOverTimeAlarm(String ip) {
        DeviceMessage deviceMessage = deviceParamMap.get(ip.concat("_wD_openDoorOverTimeAlarm"));
        if (Objects.nonNull(deviceMessage)) {
            deviceMessage.setValue("0");
            deviceMessage.setUpdateTime(ConvertUtil.getNowDateTime("yyyyMMddHHmmss"));
            log.info("发送门禁常开==={}", deviceMessage.getOutParamId(), JSON.toJSONString(deviceMessage));
            sendMessage(deviceMessage);
        }
    }


    public Map<Integer, Boolean> isOnline() {
        Map<Integer, Boolean> onlineMap = new HashMap<>();
        userIdList.forEach(userId -> {
            boolean isOnLine1 = hCNetSDK.NET_DVR_RemoteControl(0, 20005, null, 0);
            log.info("userId=={},isOnLine=={}", userId, isOnLine1);
            onlineMap.put(userId, isOnLine1);
        });
        return onlineMap;
    }

    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        log.info("接收门禁指令下发：deviceMessage {},状态：{}", JSON.toJSONString(deviceMessage), value);
        if (deviceMessage != null) {
            String outParamId = deviceMessage.getOutParamId();
            String[] param = outParamId.split("_");
            Integer userID = userMap.get(param[0]);
            Integer doorNum = Integer.valueOf(param[1]);
            //0- 关闭（对于梯控，表示受控），1- 打开（对于梯控，表示开门）
            //开
            if (value.equals("2.0")) {
                controlGateway(userID, doorNum, 1);
                //关
            } else if (value.equals("1.0")) {
                controlGateway(userID, doorNum, 0);
            }
            //反馈到iot-project
            commonDevice.feedback(message);
        }

    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }


    public int login_V40(String sDeviceIP, int iPort, String sUserName, String sPassWord) {
        int lUserID = -1;    // 用户句柄
        //设备信息, 输出参数
        HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();
        HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();
        // 注册设备-登录参数，包括设备地址、登录用户、密码等
        m_strLoginInfo.sDeviceAddress = new byte[hCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, sDeviceIP.length());
        m_strLoginInfo.sUserName = new byte[hCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(sUserName.getBytes(), 0, m_strLoginInfo.sUserName, 0, sUserName.length());
        m_strLoginInfo.sPassword = new byte[hCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(sPassWord.getBytes(), 0, m_strLoginInfo.sPassword, 0, sPassWord.length());
        m_strLoginInfo.wPort = Short.parseShort(String.valueOf(iPort));
        m_strLoginInfo.bUseAsynLogin = false; //是否异步登录：0- 否，1- 是
        m_strLoginInfo.write();
        //注册设备
        lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);
        if (lUserID < 0) {
            int errCode = hCNetSDK.NET_DVR_GetLastError();
            log.info("注册失败,设备IP:" + sDeviceIP + " 端口：" + iPort + "错误码：" + errCode);
            IntByReference intByReference = new IntByReference();
            intByReference.setValue(errCode);
            String errMsg = hCNetSDK.NET_DVR_GetErrorMsg(intByReference);
            log.info("错误信息：" + errMsg);
        } else {
            log.info("注册成功,设备IP:" + sDeviceIP + " 端口：" + iPort + "用户ID" + lUserID);
            ipMap.put(lUserID, sDeviceIP);
            userMap.put(sDeviceIP, lUserID);
            userIdList.add(lUserID);
            setupAlarmChan(lUserID, -1);
        }
        return lUserID;
    }

    /**
     * 登出操作
     */

    public void logout() {
        /**登出和清理，释放SDK资源*/
        userIdList.forEach(userId -> {
            if (userId >= 0) {
                if (!hCNetSDK.NET_DVR_Logout(userId)) {
                    log.error("设备注销失败，错误码：{}", hCNetSDK.NET_DVR_GetLastError());
                    return;
                }
                log.info("设备注销成功！！！");
            }
        });
    }

    @PreDestroy
    public void LogoutAndStopListen() {
        //撤销布防上传通道
        for (int i = 0; i < userIdList.size(); i++) {
            int closeAlarmChan = closeAlarmChan(lAlarmHandleList.get(i));
            if (closeAlarmChan > -1) {
                //注销
                hCNetSDK.NET_DVR_Logout(userIdList.get(i));
            }
        }
        logout();
        hCNetSDK.NET_DVR_Cleanup();
    }


}
