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
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 孙率众
 */
@Service
@Slf4j
public class DeviceHandler extends BaseDevice {

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
    private String Authorization;

    /**
     * 获取公钥URI
     */
    private String publicKeyURI = "/evo-apigw/evo-oauth/{0}/oauth/public-key";

    /**
     * 认证（获取token）URI
     */
    private String tokenURI = "/evo-apigw/evo-oauth/{0}/oauth/extend/token";

    /**
     * 设备树搜索URI（获取在线状态）
     */
    private String treeSearchURI = "/evo-apigw/evo-accesscontrol/{0}/resource/tree/search";

    /**
     * 门通道查询URI（获取门开关状态）
     */
    private String channelsURI = "/evo-apigw/evo-accesscontrol/{0}/card/accessControl/channelControl/channels";

    /**
     * 门禁控制URI（开门）
     */
    private String openDoorURI = "/evo-apigw/evo-accesscontrol/{0}/card/accessControl/channelControl/openDoor";
    /**
     * 门禁控制URI（关门）
     */
    private String closeDoorURI = "/evo-apigw/evo-accesscontrol/{0}/card/accessControl/channelControl/closeDoor";

    // ========================================门禁========================================
    /**
     * 报警订阅事件URI（设置报警回调采集器的URL地址）
     */
    private String mqinfoURI = "/evo-apigw/evo-event/{0}/subscribe/mqinfo";

    public static void main(String[] args) {
        String param = "{\"channelCodes\":[\"1002725$7$0$1\", \"1002726$7$0$1\"],\"allowSmartLock\":false}";
        System.out.println(param);
    }

    public void getOnlineStatus() {
        String url = host + MessageFormat.format(treeSearchURI, version);
        log.info("接口:{}", url);
        String result = HttpRequest.post(url).body("{\"typeCode\":\"01;0;8;7\"}").addHeaders(header).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url, result);
        Object read = JSONPath.read(result, "$.data.children");
        List<SearchDTO> searchDTOS = JSONArray.parseArray(String.valueOf(read), SearchDTO.class);
        if (CollectionUtils.isEmpty(searchDTOS)) {
            return;
        }
        List<String> idList = new ArrayList<>();
        searchDTOS.forEach(searchDTO -> {
            String id = searchDTO.getId().replace("ACC_", "");
            String value = "1";
            if (!"1".equals(searchDTO.getOnline())) {
                value = "0";
            }
            sendMsg(id + "_onlineStatus", value);
            idList.add(id);
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
            sendMsg(id + "_openStatus", value);
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
        log.info("接口:{}", url);
        String publicKeyResult = HttpRequest.get(url).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url, publicKeyResult);
        publicKey = String.valueOf(JSONPath.read(publicKeyResult, "$.data.publicKey"));
        encryptedText = baseEncrypt(publicKey, password);
        getToken();
        String monitor = "http://" + localhost + ":" + serverPort + "/receive";
        String subsystemName = localhost + "_" + serverPort;
        String subsystemMagic = localhost + "_" + serverPort;
        String mqinfo = "{\"param\":{\"monitors\":[{\"monitor\":\"" + monitor + "\",\"monitorType\":\"url\",\"events\":" +
                "[{\"category\":\"alarm\",\"subscribeAll\":1,\"domainSubscribe\":2},{\"category\":\"business\",\"subscribeAll\":1,\"domainSubscribe\":2}" +
                ",{\"category\":\"state\",\"subscribeAll\":1,\"domainSubscribe\":2},{\"category\":\"perception\",\"subscribeAll\":1,\"domainSubscribe\":2}]}]" +
                ",\"subsystem\":{\"subsystemType\":0,\"name\":\"" + subsystemName + "\",\"magic\":\"" + subsystemMagic + "\"}}}";
        String url2 = host + MessageFormat.format(mqinfoURI, version);
        log.info("接口:{},参数:{}", url2, mqinfo);
        String result = HttpRequest.post(url2).body(mqinfo).addHeaders(header).timeout(2000).execute().body();
        log.info("接口:{},返回值:{}", url2, result);
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
        log.info("接口:{},参数:{}", url, tokenMapStr);
        String tokenResult = HttpUtil.post(url, tokenMapStr, 2000);
        log.info("接口:{},返回值:{}", url, tokenResult);
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
        sendMsg(nodeCode + "_alarmStatus", alarmStat);
    }


    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        commonDevice.feedback(message);
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        if (ObjectUtils.isNotEmpty(deviceMessage) && StringUtils.isNotBlank(deviceMessage.getOutParamId())
                && deviceMessage.getOutParamId().endsWith("_deployWithdrawAlarmSet")) {
            String outParamId = deviceMessage.getOutParamId();
            if (redisUtil.hasKey(outParamId)) {
                return;
            }
            redisUtil.set(outParamId, "0", 5);
            final String[] strings = deviceMessage.getOutParamId().split("_");
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
                sendMsg(strings[0] + "_openStatus", value);
            }
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    private String baseEncrypt(String publicKey, String password) throws Exception {
        byte[] decoded = Base64.decode(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        //**此处Base64编码，开发者可以使用自己的库**
        return Base64.encode(cipher.doFinal(password.getBytes("UTF-8")));
    }


}
