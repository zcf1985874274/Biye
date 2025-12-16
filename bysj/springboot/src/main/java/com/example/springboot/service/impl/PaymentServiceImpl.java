package com.example.springboot.service.impl;

import com.example.springboot.mapper.PaymentMapper;
import com.example.springboot.model.Payment;
import com.example.springboot.model.User;
import com.example.springboot.service.PaymentService;
import com.example.springboot.service.UserService;
import com.example.springboot.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public Result<Payment> createPayment(Integer userId, String paymentMethod, BigDecimal amount) {
        // 检查用户是否存在
        User user = userService.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 创建支付记录
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setPaymentStatus("pending"); // 初始状态为待处理
        payment.setPaymentTime(new Timestamp(System.currentTimeMillis()));
        payment.setAlipayTransactionId(null); // 初始为空，支付宝支付后更新

        // 插入支付记录
        int affectedRows = paymentMapper.insert(payment);
        if (affectedRows == 0) {
            return Result.error("创建支付记录失败");
        }

        // 如果是余额支付，直接扣除用户余额并更新支付状态
        if ("balance".equals(paymentMethod)) {
            // 检查余额是否足够
            if (user.getBalance().compareTo(amount) < 0) {
                return Result.error("余额不足");
            }

            // 扣除余额
            user.setBalance(user.getBalance().subtract(amount));
            userService.updateUser(user);

            // 更新支付状态为成功
            paymentMapper.updatePaymentStatus(payment.getPaymentId(), "successful");
            payment.setPaymentStatus("successful");
        }

        return Result.success(payment);
    }

    @Override
    public Result<Void> updatePaymentStatus(Integer paymentId, String paymentStatus) {
        // 检查支付记录是否存在
        Payment payment = paymentMapper.findById(paymentId);
        if (payment == null) {
            return Result.error("支付记录不存在");
        }

        // 更新支付状态
        int affectedRows = paymentMapper.updatePaymentStatus(paymentId, paymentStatus);
        if (affectedRows == 0) {
            return Result.error("更新支付状态失败");
        }

        // 如果支付成功且是余额支付，已在创建时处理，无需重复处理
        // 如果支付失败且是余额支付，需要退还用户余额
        if ("failed".equals(paymentStatus) && "balance".equals(payment.getPaymentMethod())) {
            User user = userService.findById(payment.getUserId());
            if (user != null) {
                // 退还余额
                user.setBalance(user.getBalance().add(payment.getAmount()));
                userService.updateUser(user);
            }
        }

        return Result.success();
    }

    @Override
    public Result<Void> updatePaymentStatusByAlipayId(String alipayTransactionId, String paymentStatus) {
        // 检查支付记录是否存在
        Payment payment = paymentMapper.findByAlipayTransactionId(alipayTransactionId);
        if (payment == null) {
            return Result.error("支付记录不存在");
        }

        // 增加幂等性检查：只有当当前状态是"pending"时才允许更新
        if (!"pending".equals(payment.getPaymentStatus())) {
            // 如果当前状态已经是要更新的状态，则直接返回成功
            if (paymentStatus.equals(payment.getPaymentStatus())) {
                return Result.success();
            }
            // 否则返回错误，防止重复更新
            return Result.error("该支付记录状态已经更新，无法重复操作");
        }

        // 更新支付状态
        int affectedRows = paymentMapper.updatePaymentStatus(payment.getPaymentId(), paymentStatus);
        if (affectedRows == 0) {
            return Result.error("更新支付状态失败");
        }

        // 如果支付宝支付成功，需要增加用户余额（充值）
        if ("successful".equals(paymentStatus)) {
            User user = userService.findById(payment.getUserId());
            if (user != null) {
                // 增加余额
                user.setBalance(user.getBalance().add(payment.getAmount()));
                userService.updateUser(user);
            }
        }

        return Result.success();
    }

    @Override
    public List<Payment> findByUserId(Integer userId) {
        return paymentMapper.findByUserId(userId);
    }

    @Override
    public List<Payment> findByUserIdAndStatus(Integer userId, String paymentStatus) {
        return paymentMapper.findByUserIdAndStatus(userId, paymentStatus);
    }

    @Override
    public Payment findByAlipayTransactionId(String alipayTransactionId) {
        return paymentMapper.findByAlipayTransactionId(alipayTransactionId);
    }

    @Override
    public List<Payment> findAll() {
        return paymentMapper.findAll();
    }

    @Override
    public Payment findById(Integer paymentId) {
        return paymentMapper.findById(paymentId);
    }

    @Override
    public Result<Void> updatePayment(Payment payment) {
        // 检查支付记录是否存在
        Payment existingPayment = paymentMapper.findById(payment.getPaymentId());
        if (existingPayment == null) {
            return Result.error("支付记录不存在");
        }

        // 更新支付记录
        int affectedRows = paymentMapper.update(payment);
        if (affectedRows == 0) {
            return Result.error("更新支付记录失败");
        }

        return Result.success();
    }

    @Override
    public Result<Void> deleteById(Integer paymentId) {
        // 检查支付记录是否存在
        Payment payment = paymentMapper.findById(paymentId);
        if (payment == null) {
            return Result.error("支付记录不存在");
        }

        // 删除支付记录
        int affectedRows = paymentMapper.deleteById(paymentId);
        if (affectedRows == 0) {
            return Result.error("删除支付记录失败");
        }

        return Result.success();
    }
}