package com.wanda.epc.device.dto;

import lombok.Data;

/**
 *@description 门禁列表dto
 *@author LianYanFei
 *@date 2023/7/28
 */
@Data
public class DoorStateDto {
    /**
     *控制板编号
     */
    private String controllerno;

    /**
     *读头编号
     */
    private String doorno;

    /**
     * 门名称
     */
    private String roomname;

    /**
     * 控制板名称
     */
    private String controllername;

    /**
     * 连接状态(1连接,0:掉线)
     */
    private String connected;

    /**
     * 连接状态名
     */
    private String connectedname;

    /**
     * 撬设备状态(-1未知 0:正常 1:撬设备)
     */
    private String broken;

    /**
     *撬设备状态名
     */
    private String brokenname;

    /**
     * 超时未关门状态(-1未知 0:正常 1:超时未关门)
     */
    private String openedtimeout;

    /**
     * 超时未关门状态名
     */
    private String openedtimeoutname;

    /**
     * 门状态(-1 未知 0:关门  1:开门 2:强制开门)
     */
    private String opened;

    /**
     * 地址(ip尾数..读头编号或ip尾数.can地址..读头编号)
     */
    private String openedname;




}
