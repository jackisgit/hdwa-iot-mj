package com.wanda.epc.device.dto;

import lombok.Data;

import java.util.List;

/**
 * @author LianYanFei
 * @version 1.0
 * @project iot_epc_mj
 * @description 门禁状态列表结果集
 * @date 2023/7/28 17:24:01
 */
@Data
public class DoorStateResultDto {

    private boolean status;

    private String message;

    private List<DoorStateDto> data;

}
