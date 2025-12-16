package com.example.springboot.controller;

import com.example.springboot.model.Payment;
import com.example.springboot.service.PaymentService;
import com.example.springboot.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "支付管理", description = "用户支付记录管理相关接口")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建支付记录
     * @param data 包含用户ID、支付方式和金额的请求体
     * @return 支付结果
     */
    @PostMapping
    @Operation(summary = "创建支付记录", description = "用户使用余额或支付宝进行支付")
    public Result<Payment> createPayment(@RequestBody Map<String, Object> data) {
        Integer userId = (Integer) data.get("userId");
        String paymentMethod = (String) data.get("paymentMethod");
        BigDecimal amount = new BigDecimal(data.get("amount").toString());

        if (userId == null || paymentMethod == null || amount == null) {
            return Result.error("参数不完整");
        }

        Result<Payment> result = paymentService.createPayment(userId, paymentMethod, amount);
        if (result.getCode() == 200) {
            // 清除相关缓存
            redisTemplate.delete("payments:user:" + userId);
            redisTemplate.delete("payments:all");
        }
        return result;
    }

    /**
     * 根据ID获取支付记录
     * @param paymentId 支付ID
     * @return 支付记录
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "根据ID获取支付记录", description = "根据支付ID获取支付记录的详细信息")
    public Result<Payment> getPaymentById(@Parameter(description = "支付ID") @PathVariable Integer paymentId) {
        String key = "payments:id:" + paymentId;
        Payment payment = (Payment) redisTemplate.opsForValue().get(key);
        if (payment == null) {
            payment = paymentService.findById(paymentId);
            if (payment != null) {
                redisTemplate.opsForValue().set(key, payment, 1, TimeUnit.HOURS);
            }
        }
        return Result.success(payment);
    }

    /**
     * 根据用户ID获取支付记录
     * @param userId 用户ID
     * @return 支付记录列表
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID获取支付记录", description = "获取指定用户的所有支付记录")
    public Result<List<Payment>> getPaymentsByUserId(@Parameter(description = "用户ID") @PathVariable Integer userId) {
        String key = "payments:user:" + userId;
        List<Payment> payments = (List<Payment>) redisTemplate.opsForValue().get(key);
        if (payments == null) {
            payments = paymentService.findByUserId(userId);
            redisTemplate.opsForValue().set(key, payments, 30, TimeUnit.MINUTES);
        }
        return Result.success(payments);
    }

    /**
     * 根据用户ID和支付状态获取支付记录
     * @param userId 用户ID
     * @param paymentStatus 支付状态
     * @return 支付记录列表
     */
    @GetMapping("/user/{userId}/status/{paymentStatus}")
    @Operation(summary = "根据用户ID和支付状态获取支付记录", description = "获取指定用户和状态的支付记录")
    public Result<List<Payment>> getPaymentsByUserIdAndStatus(
            @Parameter(description = "用户ID") @PathVariable Integer userId,
            @Parameter(description = "支付状态") @PathVariable String paymentStatus) {
        List<Payment> payments = paymentService.findByUserIdAndStatus(userId, paymentStatus);
        return Result.success(payments);
    }

    /**
     * 根据支付宝交易ID获取支付记录
     * @param alipayTransactionId 支付宝交易ID
     * @return 支付记录
     */
    @GetMapping("/alipay/{alipayTransactionId}")
    @Operation(summary = "根据支付宝交易ID获取支付记录", description = "根据支付宝交易ID查询支付记录")
    public Result<Payment> getPaymentByAlipayTransactionId(@Parameter(description = "支付宝交易ID") @PathVariable String alipayTransactionId) {
        Payment payment = paymentService.findByAlipayTransactionId(alipayTransactionId);
        if (payment == null) {
            return Result.error("支付记录不存在");
        }
        return Result.success(payment);
    }

    /**
     * 更新支付状态
     * @param data 包含支付ID和支付状态的请求体
     * @return 更新结果
     */
    @PatchMapping("/{paymentId}/status")
    @Operation(summary = "更新支付状态", description = "更新支付记录的状态")
    public Result<Void> updatePaymentStatus(@Parameter(description = "支付ID") @PathVariable Integer paymentId, @RequestBody Map<String, String> data) {
        String paymentStatus = data.get("paymentStatus");
        if (paymentStatus == null) {
            return Result.error("支付状态不能为空");
        }

        Result<Void> result = paymentService.updatePaymentStatus(paymentId, paymentStatus);
        if (result.getCode() == 200) {
            // 清除相关缓存
            Payment payment = paymentService.findById(paymentId);
            if (payment != null) {
                redisTemplate.delete("payments:user:" + payment.getUserId());
            }
            redisTemplate.delete("payments:id:" + paymentId);
            redisTemplate.delete("payments:all");
        }
        return result;
    }

    /**
     * 根据支付宝交易ID更新支付状态
     * @param data 包含支付宝交易ID和支付状态的请求体
     * @return 更新结果
     */
    @PatchMapping("/alipay/{alipayTransactionId}/status")
    @Operation(summary = "根据支付宝交易ID更新支付状态", description = "支付宝回调接口，更新支付状态")
    public Result<Void> updatePaymentStatusByAlipayId(
            @Parameter(description = "支付宝交易ID") @PathVariable String alipayTransactionId,
            @RequestBody Map<String, String> data) {
        String paymentStatus = data.get("paymentStatus");
        if (paymentStatus == null) {
            return Result.error("支付状态不能为空");
        }

        Result<Void> result = paymentService.updatePaymentStatusByAlipayId(alipayTransactionId, paymentStatus);
        if (result.getCode() == 200) {
            // 清除相关缓存
            Payment payment = paymentService.findByAlipayTransactionId(alipayTransactionId);
            if (payment != null) {
                redisTemplate.delete("payments:user:" + payment.getUserId());
                redisTemplate.delete("payments:id:" + payment.getPaymentId());
            }
            redisTemplate.delete("payments:all");
        }
        return result;
    }

    /**
     * 获取所有支付记录
     * @return 支付记录列表
     */
    @GetMapping
    @Operation(summary = "获取所有支付记录", description = "获取系统中所有支付记录列表（管理员权限）")
    public Result<List<Payment>> getAllPayments() {
        String key = "payments:all";
        List<Payment> payments = (List<Payment>) redisTemplate.opsForValue().get(key);
        if (payments == null) {
            payments = paymentService.findAll();
            redisTemplate.opsForValue().set(key, payments, 30, TimeUnit.MINUTES);
        }
        return Result.success(payments);
    }

    /**
     * 删除支付记录
     * @param paymentId 支付ID
     * @return 删除结果
     */
    @DeleteMapping("/{paymentId}")
    @Operation(summary = "删除支付记录", description = "根据ID删除支付记录（管理员权限）")
    public Result<Void> deletePayment(@Parameter(description = "支付ID") @PathVariable Integer paymentId) {
        // 先获取要删除的支付记录，用于清除缓存
        Payment paymentToDelete = paymentService.findById(paymentId);
        Result<Void> result = paymentService.deleteById(paymentId);
        
        if (result.getCode() == 200) {
            // 清除相关缓存
            redisTemplate.delete("payments:all");
            redisTemplate.delete("payments:id:" + paymentId);
            if (paymentToDelete != null) {
                redisTemplate.delete("payments:user:" + paymentToDelete.getUserId());
            }
        }
        return result;
    }
}