package com.wanda.epc.DTO;

import lombok.Data;

import java.util.List;

@Data
public class StatusDTO {

    private List<String> channelCodes;

    private Boolean allowSmartLock;

}
