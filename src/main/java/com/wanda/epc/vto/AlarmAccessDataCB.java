package com.wanda.epc.vto;

import com.alibaba.fastjson.JSON;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author LianYanFei
 * @version 1.0
 * @project iot-epc-module
 * @description 门禁事件回调
 * @date 2023/4/12 18:05:03
 */
@Slf4j
public class AlarmAccessDataCB implements NetSDKLib.fMessCallBack {
    private AlarmAccessDataCB() {
    }

    @Override
    public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, Pointer dwUser) {
        log.info("门禁报警事件command = " + lCommand);
        switch (lCommand) {
            case NetSDKLib.NET_ALARM_ACCESS_CTL_NOT_CLOSE:
                NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO msg = new NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                log.info("门禁事件消息：{}", JSON.toJSONString(msg));
                break;
        }

        return true;
    }

    private static class fAlarmDataCBHolder {
        private static AlarmAccessDataCB instance = new AlarmAccessDataCB();
    }

    public static AlarmAccessDataCB getInstance() {
        return AlarmAccessDataCB.fAlarmDataCBHolder.instance;
    }
}
