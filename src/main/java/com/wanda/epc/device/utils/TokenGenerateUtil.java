package com.wanda.epc.device.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * @program: iot_epc
 * @description: 海康威视门禁HTTP请求token工具类
 * @author: LianYanFei
 * @create: 2022-10-11 17:00
 **/
public class TokenGenerateUtil {

    public static final String md5(String s) {
        char[] hexDigits = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            try {
                mdTemp.update(s.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                mdTemp.update(s.getBytes());
            }
            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xF];
                str[k++] = hexDigits[byte0 & 0xF];
            }
            return (new String(str)).toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }

    public static final String buildToken(String url, String paramJson, String secret) {
        String tempUrl = null;
        tempUrl = url.substring("http://".length());
        int index = tempUrl.indexOf("/");
        String URI = tempUrl.substring(index);
        String[] ss = URI.split("\\?");
        if (ss.length > 1)
            return md5(String.valueOf(ss[0]) + ss[1] + secret);
        return md5(String.valueOf(ss[0]) + paramJson + secret);
    }
}
