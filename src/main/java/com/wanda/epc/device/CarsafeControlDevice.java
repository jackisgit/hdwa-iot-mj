package com.wanda.epc.device;


import com.alibaba.fastjson.JSONObject;
import com.wanda.epc.device.utils.TokenGenerateUtil;
import com.wanda.epc.param.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author LianYanFei
 * @description 车安门禁子系统对接
 * @date 2023/7/27
 */
@Slf4j
@Service
public class CarsafeControlDevice extends BaseDevice {

    @Value("${access.secret}")
    private String privateKey;

    @Value("${access.doorOpenUrl}")
    private String doorOpenUrl;

    @Value("${access.doorStatusUrl}")
    private String doorStatusUrl;


    @Override
    public void sendMessage(DeviceMessage dm) {

    }

    @Override
    public boolean processData() throws Exception {
        return false;
    }

    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {

    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    public Boolean openDoor(String controllerNo, String doorNo) {

        try {
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("controllerno", controllerNo);
            dataMap.put("doorno", doorNo);

            String dataStr = JSONObject.toJSONString(dataMap);
            Object data = JSONObject.parse(dataStr); //从String转换成Object类型 "data":{}与"data":"{}" 区别


            // 获取当前日期时间
            LocalDateTime currentDateTime = LocalDateTime.now();
            // 定义日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 格式化当前日期时间为指定格式的字符串
            String nowDateTime = currentDateTime.format(formatter);

            Map<String, Object> validateMap = new HashMap<String, Object>();
            validateMap.put("from", "UT");// caller
            validateMap.put("timestamp", nowDateTime);// timestemp
            validateMap.put("nonce", randomMath());// nonce

            String signing = TokenGenerateUtil.signing(validateMap);
            System.out.println("signing:" + signing);
            signing = signing + data;  //此处追加data进行加密
            String sign = TokenGenerateUtil.encode(signing, privateKey);

            validateMap.put("sign", sign);
            validateMap.put("branchno", "1");
            validateMap.put("queryFormat", null);

            validateMap.put("data", data);
            String postString = JSONObject.toJSONString(validateMap);
            log.info("请求参数：{}", postString);


            String result = TokenGenerateUtil.sendPost(doorOpenUrl, postString);
            log.info("开门结果集：{}", result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public int randomMath() {
        // 创建一个Random对象
        Random random = new Random();

        // 生成0到99之间的随机数
        int randomNumber = random.nextInt(100); // 0到99的范围，不包括100

        return randomNumber;
    }
}
