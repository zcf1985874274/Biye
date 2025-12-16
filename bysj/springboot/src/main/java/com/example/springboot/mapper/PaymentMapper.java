package com.example.springboot.mapper;

import com.example.springboot.model.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMapper {

    /**
     * 插入支付记录
     * @param payment 支付对象
     * @return 影响的行数
     */
    int insert(Payment payment);

    /**
     * 根据支付ID查询支付记录
     * @param paymentId 支付ID
     * @return 支付对象
     */
    Payment findById(Integer paymentId);

    /**
     * 根据用户ID查询支付记录
     * @param userId 用户ID
     * @return 支付记录列表
     */
    List<Payment> findByUserId(Integer userId);

    /**
     * 查询所有支付记录
     * @return 支付记录列表
     */
    List<Payment> findAll();

    /**
     * 更新支付状态
     * @param paymentId 支付ID
     * @param paymentStatus 支付状态
     * @return 影响的行数
     */
    int updatePaymentStatus(@Param("paymentId") Integer paymentId, @Param("paymentStatus") String paymentStatus);

    /**
     * 根据用户ID和支付状态查询支付记录
     * @param userId 用户ID
     * @param paymentStatus 支付状态
     * @return 支付记录列表
     */
    List<Payment> findByUserIdAndStatus(@Param("userId") Integer userId, @Param("paymentStatus") String paymentStatus);

    /**
     * 根据支付宝交易ID查询支付记录
     * @param alipayTransactionId 支付宝交易ID
     * @return 支付对象
     */
    Payment findByAlipayTransactionId(String alipayTransactionId);

    /**
     * 更新支付记录
     * @param payment 支付对象
     * @return 影响的行数
     */
    int update(Payment payment);

    /**
     * 根据ID删除支付记录
     * @param paymentId 支付ID
     * @return 影响的行数
     */
    int deleteById(Integer paymentId);
}