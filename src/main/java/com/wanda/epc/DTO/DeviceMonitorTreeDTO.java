package com.wanda.epc.DTO;
import lombok.Data;

import java.util.List;

@Data
public class DeviceMonitorTreeDTO {

    private Integer code;
    private Boolean success;
    private List<DeviceMonitorTreeChildListDTO> result;
    private String message;
    private Integer sessionId;

}
