/**
  * Copyright 2024 json.cn
  */
package com.wanda.epc.DTO;
import lombok.Data;

import java.util.List;

/**
 * Auto-generated: 2024-04-02 8:53:42
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/
 */
@Data
public class DeviceMonitorTreeChildListDTO {

    private Long id;
    private Long deviceId;
    private Integer noCode;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer nodeType;
    private Integer onlineStatus;
    private String iconType;
    private List<String> childList;
    private Integer doorNo;
    private Integer manDeviceNo;
    private Integer doorStatus;
}
