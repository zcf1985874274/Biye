package com.example.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alipay")
@Data
public class AlipayConfig {
    private String gatewayUrl;        // 沙箱：https://openapi.alipaydev.com/gateway.do
    private String appId;
    private String privateKey;        // 多行用 | 换行
    private String alipayPublicKey;   // 多行用 | 换行
    private String notifyUrl;
    private String returnUrl;

    // 处理私钥格式，去除多余空格和换行符
    public String getPrivateKey() {
        if (privateKey == null) {
            return null;
        }
        // 保留BEGIN和END标记，中间内容去除所有非Base64字符
        return privateKey.replaceAll("(?m)^[\\s]*$|\\r", "");
    }

    // 处理公钥格式，去除多余空格和换行符
    public String getAlipayPublicKey() {
        if (alipayPublicKey == null) {
            return null;
        }
        // 保留BEGIN和END标记，中间内容去除所有非Base64字符
        return alipayPublicKey.replaceAll("(?m)^[\\s]*$|\\r", "");
    }
}