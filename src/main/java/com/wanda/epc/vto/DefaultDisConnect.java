package com.wanda.epc.vto;

import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.fDisConnect;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备断线回调函数，空实现。 建议回调函数使用单例模式
 *
 * @author 47081
 */
@Slf4j
public class DefaultDisConnect implements fDisConnect {
    private static DefaultDisConnect INSTANCE;

    private DefaultDisConnect() {
        // TODO Auto-generated constructor stub
    }

    public static DefaultDisConnect GetInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DefaultDisConnect();
        }
        return INSTANCE;
    }

    @Override
    public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        log.info("Device[{}] Port[{}] DisConnectCallBack!", pchDVRIP, nDVRPort);

    }
}
