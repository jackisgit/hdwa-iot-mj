package com.wanda.epc.vto;

import lombok.Data;

/**
 * @author LianYanFei
 * @version 1.0
 * @project iot-epc-module
 * @description 设备信息
 * @date 2023/4/12 11:02:25
 */
@Data
public class Device {

    private String ip;

    private int port;

    private String username;

    private String password;
}
