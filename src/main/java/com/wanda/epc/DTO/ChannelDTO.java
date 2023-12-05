package com.wanda.epc.DTO;

import lombok.Data;

@Data
public class ChannelDTO {

    private String channelCode;
    private String channelName;
    private Integer channelSeq;
    private String deviceCode;
    private String deviceModel;
    private String deviceName;
    private String deviceType;
    private Boolean escFlag;
    private Boolean flag;
    private Integer id;
    private String onlineStatus;
    private String orgCode;
    private String orgName;
    private Integer status;
    private Integer validFlag;
    private Integer workMode;

}
