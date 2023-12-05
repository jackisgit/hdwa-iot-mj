package com.wanda.epc.DTO;

import lombok.Data;

@Data
public class SearchDTO {

    private Integer channelSeq;
    private Integer channelType;
    private String deviceModel;
    private String deviceName;
    private Boolean enable;
    private String id;
    private Boolean isParent;
    private String name;
    private String online;
    private String orgName;
    private Boolean parent;

}
