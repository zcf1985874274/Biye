package com.example.springboot.service;

import com.example.springboot.common.Result;
import com.example.springboot.model.Payment;

import java.util.Map;

/**
 * 支付宝支付服务接口
 */
public interface AlipayService {

    /**
     * 创建支付宝支付订单并生成二维码
     * @param payment 支付记录
     * @return 包含支付二维码的结果
     */
    Result<Map<String, Object>> createAlipayOrder(Payment payment);

    /**
     * 处理支付宝支付回调
     * @param params 支付宝回调参数
     * @return 处理结果
     */
    Result<Boolean> handleAlipayCallback(Map<String, String> params);

    /**
     * 查询支付宝支付订单状态
     * @param paymentId 支付记录ID
     * @return 支付订单状态
     */
    Result<Map<String, Object>> queryAlipayOrderStatus(Integer paymentId);
}
