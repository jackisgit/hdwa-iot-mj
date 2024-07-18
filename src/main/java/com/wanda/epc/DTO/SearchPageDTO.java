package com.wanda.epc.DTO;

import lombok.Data;

import java.util.Date;

@Data
public class SearchPageDTO {

    private Integer id;
    private String deviceCode;
    private Integer unitType;
    private Integer unitSeq;
    private Integer channelSeq;
    private String channelCode;
    private String channelSn;
    private String channelName;
    private String ownerCode;
    private Integer isOnline;
    private Integer stat;
    private Integer isVirtual;
    private Date createTime;

}