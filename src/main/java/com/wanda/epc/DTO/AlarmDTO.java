package com.wanda.epc.DTO;

import lombok.Data;

@Data
public class AlarmDTO {

    private String id;
    private String category;
    private String method;
    private Info info;
    private String domainId;
    private String subsystem;

    @Data
    public class Info {
        private String deviceCode;
        private String deviceName;
        private String channelSeq;
        private String unitType;
        private String unitSeq;
        private String channelName;
        private String alarmCode;
        private String alarmStat;
        private String alarmType;
        private String alarmGrade;
        private String alarmDate;
        private String alarmPicture;
        private String alarmPictureSize;
        private String memo;
        private String nodeType;
        private String nodeCode;
    }
}
