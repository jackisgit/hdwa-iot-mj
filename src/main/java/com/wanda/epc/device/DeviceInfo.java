package com.wanda.epc.device;

import lombok.Data;

import java.io.Serializable;

/**
 * 日期：2023/6/19
 * 作者：flag
 * 功能：门禁设备信息
 */

@Data
public class DeviceInfo implements Serializable {

    static final long serialVersionUID = 1L;

    String ip;

    Integer port;

    String user;

    String pwd;

}
