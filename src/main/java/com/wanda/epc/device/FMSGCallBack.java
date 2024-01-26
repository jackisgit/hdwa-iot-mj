package com.wanda.epc.device;

import com.sun.jna.Pointer;
import org.springframework.stereotype.Component;


/**
 * @author jiangxin
 * @create 2022-08-15-17:26
 */
@Component
public class FMSGCallBack implements HCNetSDK.FMSGCallBack_V31 {
    //报警信息回调函数 int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser

    // lCommand 上传消息类型，这个是设备上传的数据类型，比如现在测试的门禁设备，回传回来的是 COMM_ALARM_ACS = 0x5002; 门禁主机报警信息
    // pAlarmer 报警设备信息
    // pAlarmInfo  报警信息 根据 lCommand 来选择接收的报警信息数据结构
    // dwBufLen 报警信息缓存大小
    // pUser  用户数据
    @Override
    public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        AlarmDataParse.alarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
        return true;
    }


}
