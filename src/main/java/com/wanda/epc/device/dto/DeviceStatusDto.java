package com.wanda.epc.device.dto;

import lombok.Data;

/**
 * @program: iot_epc
 * @description: 门禁状态dto
 * @author: LianYanFei
 * @create: 2022-10-12 15:07
 **/
@Data
public class DeviceStatusDto {

    /**
     * 门禁点UUID
     */
    private String doorUuid;

    /**
     * 门禁点名称
     */
    private String doorName;

    /**
     * 门禁点状态 0:未知状态 1:开门状态 2：关门状态 3：常开状态 4：长关状态
     */
    private String doorStatus;

    /**
     * 门禁设备UUID
     */
    private String deviceUuid;

    /**
     * 门禁设备名称
     */
    private String deviceName;

    /**
     * 门禁设备类型
     */
    private Integer deviceType;

    /**
     * 门禁点序号
     */
    private Integer doorNo;


}
