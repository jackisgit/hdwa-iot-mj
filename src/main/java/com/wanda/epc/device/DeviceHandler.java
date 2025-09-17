package com.wanda.epc.device;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.wanda.epc.DTO.*;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author 孙率众
 */
@Service
@Slf4j
public class DeviceHandler extends BaseDevice {

    public static final String EQUIP_SWITCH_SET = "_equipSwitchSet";
    public static final String OPEN_STATUS = "_openStatus";
    public static final String ONLINE_STATUS = "_onlineStatus";
    public static final String ALARM_STATUS = "_alarmStatus";
    /**
     * 获取公钥URI
     */
    private final String publicKeyURI = "/evo-apigw/evo-oauth/{0}/oauth/public-key";
    /**
     * 认证（获取token）URI
     */
    private final String tokenURI = "/evo-apigw/evo-oauth/{0}/oauth/extend/token";
    /**
     * 设备树搜索URI（获取在线状态）
     */
    private final String treeSearchURI = "/evo-apigw/evo-accesscontrol/{0}/resource/tree/search";
    /**
     * 子系统分页查询（获取在线状态）
     */
    private final String subsystemPage = "/evo-apigw/evo-brm/{0}/device/channel/subsystem/page";
    /**
     * 门通道查询URI（获取门开关状态）
     */
    private final String channelsURI = "/evo-apigw/evo-accesscontrol/{0}/card/accessControl/channelControl/channels";
    /**
     * 门禁控制URI（开门）
     */
    private final String openDoorURI = "/evo-apigw/evo-accesscontrol/{0}/card/accessControl/channelControl/openDoor";
    /**
     * 门禁控制URI（关门）
     */
    private final String closeDoorURI = "/evo-apigw/evo-accesscontrol/{0}/card/accessControl/channelControl/closeDoor";
    /**
     * 报警订阅事件URI（设置报警回调采集器的URL地址）
     */
    private final String mqinfoURI = "/evo-apigw/evo-event/{0}/subscribe/mqinfo";
    @Resource
    private CommonDevice commonDevice;
    private Map<String, String> header = new HashMap<>();
    @Value("${epc.host}")
    private String host;
    @Value("${epc.clientId}")
    private String clientId;
    @Value("${epc.clientSecret}")
    private String clientSecret;
    @Value("${epc.grantType}")
    private String grantType;
    @Value("${epc.username}")
    private String username;
    @Value("${epc.password}")
    private String password;
    @Value("${epc.version}")
    private String version;
    @Value("${epc.service}")
    private String service;
    @Value("${epc.localhost}")
    private String localhost;
    @Value("${server.port}")
    private String serverPort;
    private String publicKey;
    private String encryptedText;
    @Value("${epc.getOnlineStatusPath}")
    private String getOnlineStatusPath;
    // ========================================门禁========================================
    private String Authorization;

    public void getOnlineStatus() {
        String url = host + MessageFormat.format(treeSearchURI, version);
        log.info("接口:{}", url);
        String result = HttpRequest.post(url).body("{\"typeCode\":\"01;0;8;7\"}").addHeaders(header).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url, result);
        Object read = JSONPath.read(result, getOnlineStatusPath);
        List<SearchDTO> searchDTOS = JSONArray.parseArray(String.valueOf(read), SearchDTO.class);
        if (CollectionUtils.isEmpty(searchDTOS)) {
            return;
        }
        List<String> idList = new ArrayList<>();
        searchDTOS.forEach(searchDTO -> {
            String id = searchDTO.getId().replace("ACC_", "");
            String value = "0";
            if ("1".equals(searchDTO.getOnline())) {
                value = "1";
            }
            sendMsg(id + ONLINE_STATUS, value);
            idList.add(id);
        });
        getOpenStatus(idList);
    }

    public void getOnlineStatus2() {
        String url = host + MessageFormat.format(subsystemPage, "1.2.0");
        String result = HttpRequest.post(url).body("{\"deviceCategory\":\"8\",\"deviceType\":\"\",\"pageSize\":200,\"pageNum\":1}").addHeaders(header).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url, result);
        Object read = JSONPath.read(result, getOnlineStatusPath);
        List<SearchPageDTO> searchPageDTOS = JSONArray.parseArray(String.valueOf(read), SearchPageDTO.class);
        log.info("接口:{},解析返回值:{}解析后长度为:{}", url, JSONObject.toJSONString(searchPageDTOS), searchPageDTOS.size());
        if (CollectionUtils.isEmpty(searchPageDTOS)) {
            return;
        }
        List<String> idList = new ArrayList<>();
        searchPageDTOS.forEach(searchPageDTO -> {
            String value = "0";
            if (1 == searchPageDTO.getIsOnline()) {
                value = "1";
            }
            final String channelCode = searchPageDTO.getChannelCode();
            log.info("待发送,点位:{}，值:{}", channelCode + ONLINE_STATUS, value);
            sendMsg(channelCode + ONLINE_STATUS, value);
            idList.add(channelCode);
        });
        getOpenStatus(idList);
    }

    public void getOpenStatus(List<String> idList) {
        ChannelCodesDTO channelCodesDTO = new ChannelCodesDTO();
        channelCodesDTO.setChannelCodes(idList);
        channelCodesDTO.setAllowSmartLock(false);
        String orderStr = JSONObject.toJSONString(channelCodesDTO);
        String url = host + MessageFormat.format(channelsURI, version);
        log.info("接口:{}", url);
        String result = HttpRequest.post(url).addHeaders(header).body(orderStr).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url, result);
        Object read = JSONPath.read(result, "$.data");
        List<ChannelDTO> channelDTOS = JSONArray.parseArray(String.valueOf(read), ChannelDTO.class);
        if (CollectionUtils.isEmpty(channelDTOS)) {
            return;
        }
        channelDTOS.forEach(channelDTO -> {
            String id = channelDTO.getChannelCode();
            String value = "0";
            if (1 == channelDTO.getStatus()) {
                value = "1";
            }
            sendMsg(id + OPEN_STATUS, value);
        });
    }

    /**
     * 初始化获取公钥、token、注册回调地址
     *
     * @throws Exception
     */
    @PostConstruct
    public void init() throws Exception {
        String url = host + MessageFormat.format(publicKeyURI, version);
        log.warn("接口:{}", url);
        String publicKeyResult = HttpRequest.get(url).timeout(2000).execute().body();
        log.warn("接口:{},返回值:{}", url, publicKeyResult);
        publicKey = String.valueOf(JSONPath.read(publicKeyResult, "$.data.publicKey"));
        encryptedText = baseEncrypt(publicKey, password);
        getToken();
        //发送回调接口参数
        asyncSendCallbackParams();
    }

    private void asyncSendCallbackParams() {
        CompletableFuture.runAsync(() -> {
            int maxRetries = 3;
            int retryCount = 0;
            boolean success = false;

            while (retryCount < maxRetries && !success) {
                try {
                    sendCallbackParamsWithRetry();
                    success = true;
                    log.warn("回调接口参数发送成功");
                } catch (Exception e) {
                    retryCount++;
                    log.warn("回调接口参数发送超时，第{}次重试", retryCount);
                    if (retryCount >= maxRetries) {
                        log.error("回调接口参数发送失败，已达最大重试次数: {}", maxRetries, e);
                    }
                }

                // 添加重试间隔，避免频繁重试
                if (!success && retryCount < maxRetries) {
                    try {
                        Thread.sleep(2000); // 2秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("重试间隔被中断", ie);
                        break;
                    }
                }
            }
        });
    }

    /**
     * 发送回调接口参数（带超时检测）
     */
    private void sendCallbackParamsWithRetry() {
        String monitor = "http://" + localhost + ":" + serverPort + "/receive";
        String subsystemName = localhost + "_" + serverPort;
        String subsystemMagic = localhost + "_" + serverPort;
        String mqinfo = "{\"param\":{\"monitors\":[{\"monitor\":\"" + monitor + "\",\"monitorType\":\"url\",\"events\":" +
                "[{\"category\":\"alarm\",\"subscribeAll\":1,\"domainSubscribe\":2},{\"category\":\"business\",\"subscribeAll\":1,\"domainSubscribe\":2}" +
                ",{\"category\":\"state\",\"subscribeAll\":1,\"domainSubscribe\":2},{\"category\":\"perception\",\"subscribeAll\":1,\"domainSubscribe\":2}]}]" +
                ",\"subsystem\":{\"subsystemType\":0,\"name\":\"" + subsystemName + "\",\"magic\":\"" + subsystemMagic + "\"}}}";

        String url2 = host + MessageFormat.format(mqinfoURI, version);
        log.warn("接口:{},参数:{}", url2, mqinfo);

        HttpRequest.post(url2)
                .body(mqinfo)
                .addHeaders(header)
                .timeout(30000)
                .execute()
                .body();
    }

    /**
     * 获取token并设置请求头
     */
    public void getToken() {
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("client_id", clientId);
        tokenMap.put("client_secret", clientSecret);
        tokenMap.put("grant_type", grantType);
        tokenMap.put("username", username);
        tokenMap.put("password", encryptedText);
        tokenMap.put("public_key", publicKey);
        String tokenMapStr = JSONObject.toJSONString(tokenMap);
        String url = host + MessageFormat.format(tokenURI, version);
        log.warn("接口:{},参数:{}", url, tokenMapStr);
        String tokenResult = HttpUtil.post(url, tokenMapStr, 10000);
        log.warn("接口:{},返回值:{}", url, tokenResult);
        Authorization = "bearer " + JSONPath.read(tokenResult, "$.data.access_token");
        header.put("Authorization", Authorization);
    }

    @Override
    public void sendMessage(DeviceMessage dm) {
        commonDevice.sendMessage(dm);
    }

    @Override
    public boolean processData() throws Exception {
        return true;
    }

    /**
     * 收到报警
     *
     * @param alarmDTO
     */
    public void receive(AlarmDTO alarmDTO) {
        String nodeCode = alarmDTO.getInfo().getNodeCode();
        String alarmStat = alarmDTO.getInfo().getAlarmStat();
        if (!"1".equals(alarmStat)) {
            alarmStat = "0";
        }
        sendMsg(nodeCode + ALARM_STATUS, alarmStat);
    }


    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        commonDevice.feedback(message);
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        String outParamId = deviceMessage.getOutParamId();
        if (ObjectUtils.isNotEmpty(deviceMessage) && StringUtils.isNotBlank(outParamId)
                && outParamId.endsWith(EQUIP_SWITCH_SET)) {
            if (redisUtil.hasKey(outParamId)) {
                return;
            }
            redisUtil.set(outParamId, "0", 5);
            final String[] strings = outParamId.split("_");
            ChannelCodeListDTO channelCodeListDTO = new ChannelCodeListDTO();
            List<String> list = new ArrayList<>();
            list.add(strings[0]);
            channelCodeListDTO.setChannelCodeList(list);
            String orderStr = JSONObject.toJSONString(channelCodeListDTO);
            String url;
            String result;
            if ("1.0".equals(value)) {
                url = host + MessageFormat.format(openDoorURI, version);
                log.info("接口:{}", url);
                result = HttpRequest.post(url).body(orderStr).addHeaders(header).timeout(2000).execute().body();
            } else {
                url = host + MessageFormat.format(closeDoorURI, version);
                log.info("接口:{}", url);
                result = HttpRequest.post(url).body(orderStr).addHeaders(header).timeout(2000).execute().body();
            }
            log.info("接口:{},返回值:{}", url, result);
            Object read = JSONPath.read(result, "$.success");
            if (ObjectUtils.isNotEmpty(read) && !"false".equals(String.valueOf(read))) {
                sendMsg(strings[0] + OPEN_STATUS, value);
            }
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    public void clearAlarm() {
        log.info("子系统不返回报警恢复指令，自动处理");
        for (Map.Entry<String, List<DeviceMessage>> next : BaseDevice.deviceParamListMap.entrySet()) {
            if (next.getKey().endsWith(DeviceHandler.ALARM_STATUS) && !CollectionUtils.isEmpty(next.getValue())) {
                for (DeviceMessage deviceMessage : next.getValue()) {
                    deviceMessage.setValue("0");
                    commonDevice.sendMessage(deviceMessage);
                }
            }
        }
    }

    private String baseEncrypt(String publicKey, String password) throws Exception {
        byte[] decoded = Base64.decode(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        //**此处Base64编码，开发者可以使用自己的库**
        return Base64.encode(cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
    }

}
