package com.wanda.epc.device;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 日期：2023/6/19
 * 作者：flag
 * 功能：配置文件注入
 */

@Data
@Component
@ConfigurationProperties(prefix = "epc") // 配置 文件的前缀
public class DeviceConfig {
    /**
     * 设备列表
     */
    private List<DeviceInfo> deviceList;
}
