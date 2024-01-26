package com.wanda.epc.device;

import com.alibaba.fastjson.JSON;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @author LianYanFei
 * @description 接收设备上传的报警信息
 * @date 2023/4/7
 */
@Slf4j
public class AlarmDataParse {

    /**
     * 接收设备上传的报警信息，进行上传数据的业务逻辑处理
     *
     * @param lCommand   上传消息类型
     * @param pAlarmer   报警设备信息
     * @param pAlarmInfo 报警信息
     * @param dwBufLen   报警信息缓存大小
     * @param pUser      用户数据
     */
    public static void alarmDataHandle(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        log.info("===============================报警监听中================================");
        String sAlarmType = new String();
        String[] newRow = new String[3];
        //报警时间
        Date today = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String[] sIP = new String[2];

        sAlarmType = new String("lCommand=0x") + Integer.toHexString(lCommand);
        // lCommand是传的报警类型
        switch (lCommand) {
            // 门禁主机类型实时人脸抓拍上传，走这里
            case HCNetSDK.COMM_ALARMHOST_EXCEPTION:
                newRow[0] = dateFormat.format(today);
                // 报警类型
                newRow[1] = sAlarmType;
                // 报警设备IP地址
                sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                newRow[2] = sIP[0];
                log.info("报警主机故障报警信息=========={},报警IP:{}", Arrays.toString(newRow), JSON.toJSONString(sIP));
                break;
            default:
                sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
                strACSInfo.write();
                Pointer pACSInfo = strACSInfo.getPointer();
                pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
                strACSInfo.read();
                sAlarmType = "门禁主机报警信息:卡号：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim() + "，卡类型："
                        + strACSInfo.struAcsEventInfo.byCardType + "，报警主类型：" + strACSInfo.dwMajor + "，报警次类型："
                        + strACSInfo.dwMinor+ "，ip："+sIP[0];
               log.info("其他报警信息:{}",sAlarmType);
               //设备防拆报警
               if (strACSInfo.dwMajor==5 && strACSInfo.dwMinor==14){
                   HikVisionDoorDevice hikVisionDoorDevice = new HikVisionDoorDevice();
                   hikVisionDoorDevice.wD_openDoorOverTimeAlarm(sIP[0]);

               }
                break;
        }

    }
}
