package com.example.springboot.service;

import com.example.springboot.model.Payment;
import com.example.springboot.common.Result;

import java.util.List;

public interface PaymentService {

    /**
     * 创建支付记录
     * @param userId 用户ID
     * @param paymentMethod 支付方式
     * @param amount 支付金额
     * @return 支付结果
     */
    Result<Payment> createPayment(Integer userId, String paymentMethod, java.math.BigDecimal amount);

    /**
     * 更新支付状态
     * @param paymentId 支付ID
     * @param paymentStatus 支付状态
     * @return 更新结果
     */
    Result<Void> updatePaymentStatus(Integer paymentId, String paymentStatus);

    /**
     * 根据支付宝交易ID更新支付状态
     * @param alipayTransactionId 支付宝交易ID
     * @param paymentStatus 支付状态
     * @return 更新结果
     */
    Result<Void> updatePaymentStatusByAlipayId(String alipayTransactionId, String paymentStatus);

    /**
     * 根据用户ID查询支付记录
     * @param userId 用户ID
     * @return 支付记录列表
     */
    List<Payment> findByUserId(Integer userId);

    /**
     * 根据用户ID和支付状态查询支付记录
     * @param userId 用户ID
     * @param paymentStatus 支付状态
     * @return 支付记录列表
     */
    List<Payment> findByUserIdAndStatus(Integer userId, String paymentStatus);

    /**
     * 根据支付宝交易ID查询支付记录
     * @param alipayTransactionId 支付宝交易ID
     * @return 支付记录
     */
    Payment findByAlipayTransactionId(String alipayTransactionId);

    /**
     * 查询所有支付记录
     * @return 支付记录列表
     */
    List<Payment> findAll();

    /**
     * 获取支付记录详情
     * @param paymentId 支付ID
     * @return 支付记录
     */
    Payment findById(Integer paymentId);

    /**
     * 更新支付记录
     * @param payment 支付对象
     * @return 更新结果
     */
    Result<Void> updatePayment(Payment payment);

    /**
     * 删除支付记录
     * @param paymentId 支付ID
     * @return 删除结果
     */
    Result<Void> deleteById(Integer paymentId);
}