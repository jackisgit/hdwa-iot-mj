package com.wanda.epc.device.service;

public class BaseConstants {
    /**会话标记*/
    public static final String SESSION_FLAG = "user";
    /**考勤*/
    public static final String ATT = "att";
    /**门禁*/
    public static final String ACC = "acc";
    /**消费*/
    public static final String POS = "pos";
    /**梯控*/
    public static final String ELE = "ele";
    /**信息屏*/
    public static final String INS = "ins";
    /**门禁实时记录缓存key*/
    public static final String ADMS_ACC_LOG = "adms:accLog:";
    /**消费记录缓存key*/
    public static final String ADMS_POS_LOG = "adms:posLog:";
    /**考勤设备上传缓存key*/
    public static final String ADMS_ATT_LOG = "adms:att:";
    /**设备上传数据*/
    public static final String ADMS_UPLOAD_DATA = "adms:uploadData:";
    /**dll文件缓存目录*/
    public static final String BIOSECURITY_DLL_FILE = "biosecurity_dll_file";
    /**人员访客预约二维码地址**/
    public static final String CLOUD_QRCODE_URL = "cloud.qrcode.url";

    /**中控云平台websocketurl**/
    public static final String ZKTECO_CLOUD_WEBSOCKET_URL = "zkteco:cloud:websocket:url";

    /**云文件服务器域名**/
    public static final String CLOUD_CFS_FILEURL = "cloud.cfs.fileUrl";

    public static final String RESOURCE_TYPE_SYSTEM = "system";
    public static final String RESOURCE_TYPE_MENU = "menu";
    public static final String RESOURCE_TYPE_BUTTON = "button";
    public static final String RESOURCE_TYPE_DATA = "data";

    /*标准产品编码*/
    public static final String PRODUCT_STANDARD_TYPE = "standard";

    /**user 用户菜单，就是云平台的企业用户*/
    public static final String PERMISSION_USER = "user";
    /**admin 管理员菜单，zk后端数据运营，即超级管理员*/
    public static final String PERMISSION_ADMIN = "admin";


    public static class BaseBioType{
        //指纹比对
        public static final short FP_TEMPLATE_TYPE = 2;//以模版传入
        public static final short FP_IMAGE_WIDTH = 0;//图像宽度，FP_TEMPLATE_TYPE=0或2时，不需要传入次参数，默认为0
        public static final short FP_IMAGE_HEIGHT = 0;//图像高度，FP_TEMPLATE_TYPE=0或2时，不需要传入次参数，默认为0

        public static final Short VALID_FLAG_ENABLE = 1;//是否可用标记

        /** ----------------------指纹登记 start--------------------------- */
        public static final Short FP_BIO_TYPE = 1;//生物识别模板表模板类型
        public static final String FP_BIO_VERSION = "10";//生物识别模板表的版本(9.0和10.0)
        /** ----------------------指纹登记 end----------------------------- */

        //面部模版
        public static final Short FACE_BIO_TYPE = 2;
        public static final String FACE_BIO_VERSION = "7";//面部版本7

        //指静脉模版
        public static final Short VEIN_BIO_TYPE = 7;
        public static final String VEIN_BIO_VERSION = "3";//指静脉版本3.0

        //掌静脉模版
        public static final Short PALM_BIO_TYPE = 8;
        public static final String PALM_BIO_VERSION = "5";//掌静脉版本5.0
    }
    public static final Short SEND_SUCCESS = 0;//发送成功
    public static final Short SEND_FAILED = -1;//发送失败
    public static final Short SEND_WAIT = 1;//等待发送
    public static final Short SEND_CONTINUE_FOR_FAILED = -2;//尝试发送失败，等待继续发送
    public static final Short SEND_MAX_COUNT = 3;//尝试最大发送次数(邮件/短息)
    //备份相关
    public static final Short SUCCESS = 0;//成功
    public static final Short FAILED = -1;//失败

    /**单门禁，基础门禁产品定义*/
    public static final String ZKBIO_ACCESS = "ZKBioAccess";
    /**标准主流百傲瑞达产品定义*/
    public static final String ZKBIO_SECURITY = "ZKBioSecurity";

    //zkcloud AES key
    public static final String  ZKCLOUD_AES_KEY  = "xmzkteco5000";

    //zkcloud AES initvector
    public static final String  ZKCLOUD_AES_INIT_VECTOR  = "zkteco";
}
