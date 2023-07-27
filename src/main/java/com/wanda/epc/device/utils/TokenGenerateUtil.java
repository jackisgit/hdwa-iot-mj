package com.wanda.epc.device.utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static java.util.Arrays.sort;

/**
 *@description 车安门禁token工具类
 *@author LianYanFei
 *@date 2023/7/27
 */
public class TokenGenerateUtil {

    public static String encode(String content, String seed) throws Exception {
        String macKey = seed;
        String macData = content;
        Mac mac = Mac.getInstance("HMACSHA1");
        byte[] secretByte = macKey.getBytes("UTF-8");
        byte[] dataBytes = macData.getBytes("UTF-8");
        SecretKey secret = new SecretKeySpec(secretByte, "HMACSHA1");
        mac.init(secret);
        byte[] doFinal = mac.doFinal(dataBytes);
        String checksum = Base64.encode(doFinal);
        System.err.println("ENCODE:" + checksum);
        return checksum;
    }

    public static String sendPost(String urlstring, String paramstring) {
        String result = "";
        HttpURLConnection connection = null;
        try {

            URL url = new URL(urlstring);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            // connection.setConnectTimeout(1000);
            connection.setRequestProperty("Content-type", "application/json");
            connection.getOutputStream().write(paramstring.getBytes("UTF-8"));
            connection.getOutputStream().flush();
            connection.getOutputStream().close();
            int code = connection.getResponseCode();
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
            String tempLine = reader.readLine();
            StringBuilder buider = new StringBuilder();
            while (tempLine != null) {
                buider.append(tempLine);
                tempLine = reader.readLine();
            }
            result = buider.toString();
            reader.close();
            in.close();
        } catch (MalformedURLException e) {
            System.out.println(e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return result;
    }

    public static String signing(Map<String, Object> map) {
        String[] array = map.values().toArray(new String[0]);
        sort(array);
        StringBuilder builder = new StringBuilder();
        for (String string : array) {
            builder.append(string);
        }
        return builder.toString();
    }
}
