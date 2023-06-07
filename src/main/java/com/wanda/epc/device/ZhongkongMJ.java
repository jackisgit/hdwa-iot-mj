package com.wanda.epc.device;

import com.alibaba.fastjson.JSON;
import com.wanda.epc.device.service.AdmsSdkServiceImpl;
import com.wanda.epc.device.service.SdkResult;
import com.wanda.epc.param.DeviceMessage;
import com.wanda.epc.param.DispatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 中控门禁设备对接
 */
@Service
@Slf4j
public class ZhongkongMJ extends BaseDevice {

    @Autowired
    CommonDevice commonDevice;

    private AdmsSdkServiceImpl admsSdkService;

    protected List<String> ipList = new ArrayList<>();

    protected Map<String, String> ipConnectType = new HashMap<>();

    private boolean isFirst = true;

    @Override
    public void sendMessage(DeviceMessage dm) {
        if (dm != null){
            log.info("门禁数据发送：{}", JSON.toJSONString(dm));
            commonDevice.sendMessage(dm);
        }
    }


    /**
     * 此块代码是获取门的开关状态代码  由于线程门磁没有接反馈线 因此返回的开关状态都为00 无门磁  后续接完线在做测试
     */
    public boolean getDoorOpenStatus(){
        List<String> ipList = new ArrayList<>();
        Iterator<Map.Entry<String, DeviceMessage>> iterator = deviceParamMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, DeviceMessage> next = iterator.next();
            //查看开关状态
            if(next.getKey().endsWith("openStatus")){
                String ip = next.getKey().split("_")[0];
                String connectType = next.getKey().split("_")[1];
                if("TCP".equals(connectType) && !ipList.contains(ip)){
                    if(admsSdkService == null){
                        admsSdkService = new AdmsSdkServiceImpl();
                    }
                    String connectMessage = "protocol=TCP,ipaddress=" + ip + ",port=4370,timeout=3000,passwd=";
                    SdkResult sdkResult = admsSdkService.connectExt(connectMessage, new int[4]);
                    log.info("设备连接信息[{}]，反馈信息[{}]", connectType, sdkResult);
                    if (sdkResult.getSuccess() && sdkResult.getResult() > 0){
                        log.info("设备连接成功");
                        sdkResult = admsSdkService.getRTLog(sdkResult.getResult(), new byte[1024*1024] );
                        if(sdkResult.getResult() >= 0){
                            log.info("获取实时数据成功:{}",sdkResult);
                            String[] sourceStrArray = sdkResult.getData().split(",");
                            String judgingCondition = sourceStrArray[4];
                            //只有第四位数据位255时才是 开关状态和报警状态数据
                            if(judgingCondition.equals("255")){
                                String data = sourceStrArray[1];
                                //例：如果该值为0x01020001，则表示该设备的1号门为关闭，2号门为没有设置门磁，3号门为门打开，4号门为门关闭
                                byte[] bytes = intToBytes(Integer.parseInt(data));
                                for(int i = 0; i < bytes.length; i++){
                                    //0无门磁，1门关, 2门开
                                    Byte value = bytes[i];
                                    int doorNumber = bytes.length - i;
                                    DeviceMessage deviceMessage = deviceParamMap.get(ip + "_" + connectType + "_" + doorNumber + "_openStatus");
                                    if(deviceMessage != null){
                                        deviceMessage.setValue(value.toString());
                                    }
                                    sendMessage(deviceMessage);
                                }
                            }
                        }else{
                            log.info("获取实时数据失败:{}", sdkResult);
                        }
                    }else{
                        log.info("设备连接失败");
                    }
                }
                ipList.add(ip);
            }
        }
        return true;
    }

    public static byte[] intToBytes(int num) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (num >>> (24 - i * 8));
        }
        return b;
    }

    @Override
    public boolean processData() {
        if(isFirst){
            Iterator<Map.Entry<String, DeviceMessage>> iterator = super.deviceParamMap.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, DeviceMessage> next = iterator.next();
                String outParamId = next.getKey();
                if(outParamId.contains("HTTP") || outParamId.contains("TCP")){
                    String ip = outParamId.split("_")[0];
                    if(!ipList.contains(ip)){
                        ipList.add(ip);
                    }
                    ipConnectType.put(ip, outParamId.split("_")[1]);
                }
            }
            isFirst = false;
        }
        //设备参数
        // IP_TCP_onLineStatus  在线状态:0:在线  1:离线
        //局域网内查询 可以搜索到的设备
        if(admsSdkService == null){
            admsSdkService = new AdmsSdkServiceImpl();
        }
        SdkResult sdkResult = admsSdkService.searchDevice();
        log.info("搜索局域网内的所有设备IP开始");
        if(sdkResult.getSuccess() && sdkResult.getResult() > 0){
            //处理成在线List列表
            List<String> ipOnlineList = Arrays.asList(sdkResult.getData().split(",")).stream().filter((str)->str.startsWith("IP")).collect(Collectors.toList())
                    .stream().map(e -> e.split("=")[1]).collect(Collectors.toList());
            log.info("搜索局域网内的所有设备IP成功[{}]", ipOnlineList);
            for (String ip : ipList){
                String outParamId = ip + "_" + ipConnectType.get(ip) + "_onlineStatus";
                List<DeviceMessage> deviceMessageList = deviceParamListMap.get(outParamId);
                if(!CollectionUtils.isEmpty(deviceMessageList)){
                    for (DeviceMessage deviceMessage : deviceMessageList) {
                        if(ipOnlineList.contains(ip)){
                            //局域网内能搜索到说明是在线
                            deviceMessage.setValue("1");
                        }else{
                            //局域网内不能能搜索到说明是离线
                            deviceMessage.setValue("0");
                        }
                        sendMessage(deviceMessage);
                    }
                }
            }
        }else{
            log.info("搜索局域网内的所有设备IP,查询失败[{}]", sdkResult);
        }
        return true;
    }

    @Override
    public void dispatchCommand(String meter, Integer funcid, String value, String message) throws Exception {
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        if (deviceMessage!=null && value.equals("2.0")) {
            String outParamId = deviceMessage.getOutParamId();
            String[] split = outParamId.split("_");
            String connectType = split[1];
            String IP = split[0];
            int doorNum = Integer.parseInt(split[2]);
            if("TCP".equals(connectType)){
                if(admsSdkService == null){
                    admsSdkService = new AdmsSdkServiceImpl();
                }
                //先建立设备连接
                String connectMessage = "protocol=TCP,ipaddress="+ IP +",port=4370,timeout=3000,passwd=";
                SdkResult sdkResult = admsSdkService.connectExt(connectMessage, new int[4]);
                if (sdkResult.getSuccess() && sdkResult.getResult() > 0){
                    log.info("设备连接成功，连接信息[{}], 返回信息[{}]", connectMessage, sdkResult);
                    int hCommPro = sdkResult.getResult();//获取句柄，后续根据此句柄，进行设备的操作、查询等
                    sdkResult = admsSdkService.controlDevice(hCommPro,1, doorNum,1, 5,0,"");//远程开门
                    if (sdkResult.getResult() >= 0){
                        log.info("IP为[{}]编号为[{}]开门成功, 返回信息[{}]", IP, doorNum, sdkResult);
                        commonDevice.feedback(message);
                    }else{
                        log.info("IP为[{}]编号为[{}]开门失败, 返回信息[{}]", IP, doorNum, sdkResult);
                    }
                    log.info("断开设备连接[{}]", hCommPro);
                }else{
                    log.error("设备连接失败，连接信息[{}], 返回信息[{}]", connectMessage, sdkResult);
                }
            }else if("HTTP".equals(connectType)){



            }
        }
        commonDevice.feedback(message);
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    /*@Override //测试开门
    public void run(ApplicationArguments args) throws Exception {
        AdmsSdkServiceImpl admsSdkService = new AdmsSdkServiceImpl();
        //先建立设备连接
        List<String> list = new ArrayList<>();
        list.add("192.168.9.98");
        list.add("192.168.9.87");
        list.add("192.168.9.88");
        list.add("192.168.9.81");
        *//*log.info("设备连接成功,准备开门");
        sdkResult = admsSdkService.controlDevice(sdkResult.getResult(),1, 5,1, 5, 0,"");//远程开门
        log.info("开门反馈信息[{}]", sdkResult);
        if (sdkResult.getResult() >= 0){
            log.info("开门成功, 返回信息[{}]", sdkResult);
        }*//*
        for (String ip : list){
            String connectMessage = "protocol=HTTP,ipaddress=" + ip + ",port=4370,timeout=3000,passwd=";
            log.info("设备连接..." + connectMessage);
            SdkResult sdkResult = admsSdkService.connectExt(connectMessage, new int[4]);
            log.info("设备连接反馈信息..." + sdkResult.toString());
            if (sdkResult.getSuccess() && sdkResult.getResult() > 0){
                sdkResult = admsSdkService.getRTLog(sdkResult.getResult(), new byte[1024*1024] );
                log.info("获取实时数据:{}", sdkResult.getData());
            }
            Thread.sleep(1000);
        }

    }*/


    public static void main(String[] args) {
        String a = "192.168.9.85, 192.168.9.161, 192.168.1.154, 192.168.1.158, 192.168.9.153, 192.168.9.156, 192.168.9.162, 192.168.9.100, 192.168.9.155, 192.168.9.104, 192.168.9.164, 192.168.1.152, 192.168.9.96, 192.168.9.99, 192.168.9.108, 192.168.9.107, 192.168.9.106, 192.168.9.113, 192.168.9.111, 192.168.9.95, 192.168.9.212, 192.168.9.112, 192.168.9.114, 192.168.9.97, 192.168.9.110, 192.168.9.115, 192.168.9.103, 192.168.9.117, 192.168.9.165, 192.168.9.205, 192.168.9.118, 192.168.9.202, 192.168.9.214, 192.168.9.116, 192.168.9.105, 192.168.9.216, 192.168.9.210, 192.168.9.203, 192.168.9.109, 192.168.9.207, 192.168.9.102, 192.168.9.94, 192.168.9.201, 192.168.9.215, 192.168.9.204, 192.168.9.217, 192.168.9.206, 192.168.9.208, 192.168.9.213, 192.168.9.211, 192.168.9.163, 192.168.9.87, 192.168.9.166, 192.168.9.101, 192.168.9.83, 192.168.9.82, 192.168.9.209, 192.168.9.84, 192.168.9.81, 192.168.9.86, 192.168.9.80, 192.168.9.88, 192.168.9.89, 192.168.9.92, 192.168.9.167, 192.168.9.98";
        System.out.println(a.split(",").length);
    }

}
