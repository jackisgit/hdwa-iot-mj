package com.wanda.epc.device;

import lombok.Data;

/**
 * @program: iot_epc
 * @description: 广场门禁设备信息dto
 * @author: 孙率众
 * @create: 2022-10-12 14:31
 **/
@Data
public class DoorsDto {

    /**
     * 门禁点UUID
     */
    private String doorUuid;

    /**
     * 门禁点名称
     */
    private String doorName;

    /**
     * 控制中心UUID
     */
    private String unitUuid;

    /**
     * 区域UUID
     */
    private String regionUuid;

    /**
     * 门禁点序号
     */
    private Integer doorNo;

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
     * 备注
     */
    private String remark;
}
