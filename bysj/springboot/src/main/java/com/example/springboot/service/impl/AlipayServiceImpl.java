package com.example.springboot.service.impl;

import com.alipay.api.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.example.springboot.common.Result;
import com.example.springboot.config.AlipayConfig;
import com.example.springboot.model.Payment;
import com.example.springboot.service.AlipayService;
import com.example.springboot.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付服务实现类（适配最新 alipay-sdk-java 4.40+）
 */
@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private PaymentService paymentService;

    /** Jackson 单例，线程安全 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 获取支付宝客户端实例
     * 每次请求都创建新实例，确保配置能够立即生效
     */
    private AlipayClient getAlipayClient() {
        return new DefaultAlipayClient(
                alipayConfig.getGatewayUrl(),      // 沙箱网关地址：https://openapi-sandbox.dl.alipaydev.com/gateway.do
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                "json",
                "UTF-8",
                alipayConfig.getAlipayPublicKey(),
                "RSA2"
        );
    }

    /**
     * 当面付 - 预下单（生成二维码）
     */
    @Override
    public Result<Map<String, Object>> createAlipayOrder(Payment payment) {
        try {
            // 1. 生成商户订单号（保证全局唯一，确保只包含字母、数字和下划线）
            String outTradeNo = "ALI_" + System.currentTimeMillis() + "_" + payment.getPaymentId();
            // 清理商户订单号，确保不包含任何特殊字符
            String cleanOutTradeNo = outTradeNo.trim().replaceAll("[^a-zA-Z0-9_]", "");

            AlipayClient client = getAlipayClient();
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(alipayConfig.getNotifyUrl());

            // 2. 业务参数（全部用 String）
            Map<String, String> bizContent = new HashMap<>();
            bizContent.put("subject", "游戏房卡充值");
            bizContent.put("out_trade_no", cleanOutTradeNo);
            bizContent.put("total_amount", payment.getAmount().stripTrailingZeros().toPlainString());
            bizContent.put("timeout_express", "15m");                    // 订单关闭时间
            bizContent.put("qr_code_timeout_express", "30m");           // 二维码有效期（必须）

            // 使用ObjectMapper将Map转换为JSON字符串
            try {
                String bizContentJson = MAPPER.writeValueAsString(bizContent);
                request.setBizContent(bizContentJson);
            } catch (Exception e) {
                log.error("构建业务参数JSON失败", e);
                return Result.error("系统异常，请稍后重试");
            }

            // 3. 执行请求
            log.info("开始执行支付宝预下单请求，商户订单号：{}，金额：{}", cleanOutTradeNo, payment.getAmount());
            AlipayTradePrecreateResponse response = client.execute(request);
            log.info("支付宝预下单响应：code={} msg={} subCode={} subMsg={} isSuccess={}",
                    response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg(), response.isSuccess());
            log.info("支付宝返回的二维码原始数据：'{}'", response.getQrCode());

            // 严格检查支付宝响应：通信成功且业务处理成功（code为10000）
            if (response.isSuccess() && "10000".equals(response.getCode())) {
                // 检查二维码是否返回
                if (response.getQrCode() == null || response.getQrCode().isEmpty()) {
                    log.error("支付宝预下单成功但未返回二维码，商户订单号：{}", cleanOutTradeNo);
                    return Result.error("支付宝预下单成功但未返回二维码");
                }
                
                // 清理二维码数据，去除所有可能的多余字符和格式问题
                String qrCodeRaw = response.getQrCode();
                log.info("支付宝返回的二维码原始数据：'{}'", qrCodeRaw);
                
                // 1. 首先去除所有空白字符（包括空格、制表符、换行符等）
                String cleanQrCode = qrCodeRaw.replaceAll("\\s+", "");
                
                // 2. 移除所有类型的引号（单引号、双引号、反引号，包括全角和半角）
                cleanQrCode = cleanQrCode.replaceAll("['`\"\\u2018\\u2019\\u201C\\u201D\\u0060\\u0027\\u0022]", "");
                
                // 3. 确保只保留URL字符
                cleanQrCode = cleanQrCode.replaceAll("[^a-zA-Z0-9:/\\.\\-\\?\\=\\&\\_\\~\\%]", "");
                
                log.info("清理后的二维码数据：'{}'", cleanQrCode);
                
                // 保存商户订单号（用于后续查询）
                payment.setAlipayTransactionId(cleanOutTradeNo);
                paymentService.updatePayment(payment);

                // 短暂延迟，确保支付宝系统有足够时间处理订单
                try {
                    Thread.sleep(500); // 500ms延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延迟线程被中断", e);
                }

                Map<String, Object> data = new HashMap<>();
                data.put("qrCode", cleanQrCode);      // 前端直接生成二维码
                data.put("outTradeNo", cleanOutTradeNo);
                data.put("paymentId", payment.getPaymentId());

                log.info("支付宝当面付预下单成功，商户订单号：{}，订单金额：{}", cleanOutTradeNo, payment.getAmount());
                log.info("二维码数据：'{}'", cleanQrCode);
                return Result.success(data);
            } else {
                log.error("支付宝预下单失败：");
                log.error("响应码：{}", response.getCode());
                log.error("响应消息：{}", response.getMsg());
                log.error("子响应码：{}", response.getSubCode());
                log.error("子响应消息：{}", response.getSubMsg());
                log.error("业务参数：{}", request.getBizContent());
                log.error("完整响应：{}", response);
                return Result.error("支付宝预下单失败：" + response.getSubMsg());
            }
        } catch (Exception e) {
            log.error("创建支付宝订单异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 支付宝异步通知回调
     */
    @Override
    public Result<Boolean> handleAlipayCallback(Map<String, String> params) {
        try {
            // 1. 验签（最重要！）
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );

            if (!signVerified) {
                log.warn("支付宝回调验签失败，params={}", params);
                return Result.error("fail");
            }

            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");          // 支付宝交易号
            String outTradeNo = params.get("out_trade_no");   // 商户订单号
            
            // 对支付宝返回的商户订单号进行与创建时相同的清理
            String cleanOutTradeNo = outTradeNo.trim().replaceAll("[^a-zA-Z0-9_]", "");

            log.info("支付宝异步通知：tradeStatus={} tradeNo={} outTradeNo={} cleanOutTradeNo={}", 
                    tradeStatus, tradeNo, outTradeNo, cleanOutTradeNo);

            // 2. 防止重复通知（幂等性）
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                paymentService.updatePaymentStatusByAlipayId(cleanOutTradeNo, "successful");
            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                paymentService.updatePaymentStatusByAlipayId(cleanOutTradeNo, "failed");
            }

            // 必须返回 success 给支付宝，否则会持续重试通知
            return Result.success(true);
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return Result.error("fail");
        }
    }



    /**
     * 查询订单支付状态（轮询或手动查询）
     */
    @Override
    public Result<Map<String, Object>> queryAlipayOrderStatus(Integer paymentId) {
        try {
            Payment payment = paymentService.findById(paymentId);
            if (payment == null || payment.getAlipayTransactionId() == null) {
                return Result.error("支付记录不存在或未生成支付宝订单");
            }

            // 清理商户订单号，确保查询时使用的与创建时一致（使用相同的清理规则）
            String outTradeNo = payment.getAlipayTransactionId().trim().replaceAll("[^a-zA-Z0-9_]", "");

            AlipayClient client = getAlipayClient();
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

            Map<String, String> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            
            // 使用ObjectMapper将Map转换为JSON字符串
            try {
                String bizContentJson = MAPPER.writeValueAsString(bizContent);
                request.setBizContent(bizContentJson);
            } catch (Exception e) {
                log.error("构建业务参数JSON失败", e);
                return Result.error("系统异常，请稍后重试");
            }

            // 执行查询请求，最多重试5次，间隔1秒
            AlipayTradeQueryResponse response = null;
            int retryCount = 0;
            int maxRetries = 5;
            long retryDelay = 1000; // 1秒
            boolean found = false;
            
            while (retryCount < maxRetries && !found) {
                try {
                    response = client.execute(request);
                    
                    // 检查是否查询成功
                    if (response != null) {
                        // 如果交易存在（无论状态如何），都标记为找到
                        if (response.isSuccess() || "40004".equals(response.getCode())) {
                            found = true;
                        } 
                        // 通信失败，重试
                        else {
                            retryCount++;
                            log.warn("查询支付宝订单状态通信失败，正在重试 ({}/{})", retryCount, maxRetries);
                            if (retryCount < maxRetries) {
                                Thread.sleep(retryDelay);
                            }
                        }
                    } 
                    // 通信失败，重试
                    else {
                        retryCount++;
                        log.warn("查询支付宝订单状态通信失败，正在重试 ({}/{})", retryCount, maxRetries);
                        if (retryCount < maxRetries) {
                            Thread.sleep(retryDelay);
                        }
                    }
                } catch (Exception e) {
                    retryCount++;
                    log.warn("查询支付宝订单状态异常，正在重试 ({}/{})", retryCount, maxRetries, e);
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        throw e;
                    }
                }
            }

            // 映射支付宝交易状态到系统支付状态
            String tradeStatus = null;
            String paymentStatus = payment.getPaymentStatus(); // 默认使用当前状态
            String totalAmount = null;
            String tradeNo = null;
            String alipayOutTradeNo = null;
            
            // 检查查询结果
            if (response != null) {
                tradeStatus = response.getTradeStatus();
                totalAmount = response.getTotalAmount();
                tradeNo = response.getTradeNo();
                alipayOutTradeNo = response.getOutTradeNo();
                
                if (response.isSuccess() && "10000".equals(response.getCode())) {
                    switch (tradeStatus) {
                        case "TRADE_SUCCESS":
                        case "TRADE_FINISHED":
                            paymentStatus = "successful";
                            break;
                        case "WAIT_BUYER_PAY":
                            paymentStatus = "pending";
                            break;
                        case "TRADE_CLOSED":
                            paymentStatus = "failed";
                            break;
                        default:
                            paymentStatus = "pending";
                            break;
                    }
                    
                    // 无论原状态是什么，只要支付宝返回交易成功或失败，就更新支付状态
                    if ("successful".equals(paymentStatus) || "failed".equals(paymentStatus)) {
                        payment.setPaymentStatus(paymentStatus);
                        // 保留商户订单号，同时可以考虑新增字段存储支付宝真实交易号
                        // 这里不覆盖alipayTransactionId，保持使用商户订单号进行后续查询
                        paymentService.updatePayment(payment);
                    }
                } else if ("40004".equals(response.getCode()) && "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                    // 交易不存在，保持原状态为pending
                    paymentStatus = "pending";
                    tradeStatus = "WAIT_BUYER_PAY"; // 模拟待支付状态
                    alipayOutTradeNo = payment.getAlipayTransactionId();
                    totalAmount = payment.getAmount().toString();
                    log.info("查询的交易不存在：{}", alipayOutTradeNo);
                } else {
                    // 其他错误，记录日志
                    log.error("查询支付宝订单状态失败：{}", response.getMsg());
                    return Result.error("查询支付状态失败：" + response.getSubMsg());
                }
            } else {
                // 如果响应为null，使用默认值
                paymentStatus = "pending";
                tradeStatus = "WAIT_BUYER_PAY";
                alipayOutTradeNo = payment.getAlipayTransactionId();
                totalAmount = payment.getAmount().toString();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("paymentStatus", paymentStatus);
            result.put("tradeStatus", tradeStatus);
            result.put("totalAmount", totalAmount);
            result.put("tradeNo", tradeNo);
            result.put("outTradeNo", alipayOutTradeNo);
                
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询支付宝订单状态异常", e);
            return Result.error("系统异常");
        }
    }
}