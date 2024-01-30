package com.wanda.epc.device;

import lombok.Data;

/**
 * @program: iot_epc
 * @description: 门禁历史事件dto
 * @author: 孙率众
 * @create: 2022-10-12 17:05
 **/
@Data
public class DoorEventDto {
    /**
     *
     */
    private String doorName;

    /**
     * 门禁点UUID
     */
    private String doorUuid;

    /**
     * 门禁事件UUID
     */
    private String eventUuid;

    /**
     * 门禁事件类型
     */
    private Integer eventType;

    /**
     * 发生时间
     */
    private Long eventTime;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 读卡器类型
     */
    private Integer deviceType;

    /**
     * 卡号
     */
    private String cardNo;

    /**
     * 人员ID
     */
    private Integer personId;

    /**
     * 人员名称
     */
    private String personName;

    /**
     * 部门UUID
     */
    private String deptUuid;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 联动图片URL
     */
    private String picUrl;

    /**
     * 联动录像URL
     */
    private String videoUrl;


}
