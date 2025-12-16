package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.model.Payment;
import com.example.springboot.service.AlipayService;
import com.example.springboot.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付宝支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/alipay")
@Tag(name = "支付宝支付", description = "支付宝支付相关接口")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    /**
     * 创建支付宝支付订单并生成二维码
     * @param paymentId 支付记录ID
     * @return 包含支付二维码的结果
     */
    @PostMapping("/pay/{paymentId}")
    @Operation(summary = "创建支付宝支付订单", description = "创建支付宝支付订单并生成支付二维码")
    public Result<Map<String, Object>> createAlipayOrder(
            @Parameter(description = "支付记录ID") @PathVariable Integer paymentId) {
        Payment payment = paymentService.findById(paymentId);
        if (payment == null) {
            return Result.error("支付记录不存在");
        }

        if (!"pending".equals(payment.getPaymentStatus())) {
            return Result.error("该支付记录状态异常，无法发起支付");
        }

        if (!"alipay".equals(payment.getPaymentMethod())) {
            return Result.error("该支付记录不是支付宝支付方式");
        }

        return alipayService.createAlipayOrder(payment);
    }

    /**
     * 查询支付宝支付订单状态
     * @param paymentId 支付记录ID
     * @return 支付订单状态
     */
    @GetMapping("/status/{paymentId}")
    @Operation(summary = "查询支付宝支付订单状态", description = "查询支付宝支付订单的状态")
    public Result<Map<String, Object>> queryAlipayOrderStatus(
            @Parameter(description = "支付记录ID") @PathVariable Integer paymentId) {
        return alipayService.queryAlipayOrderStatus(paymentId);
    }

    /**
     * 处理支付宝支付回调
     * @param params 支付宝回调参数
     * @return 处理结果
     */
    @PostMapping("/payments/notify")
    @Operation(summary = "处理支付宝支付回调", description = "接收并处理支付宝支付回调通知")
    public String handleAlipayCallback(@RequestParam Map<String, String> params) {
        log.info("收到支付宝回调，参数: {}", params);

        Result<Boolean> result = alipayService.handleAlipayCallback(params);
        if (result.getCode() == 200 && result.getData()) {
            return "success";
        } else {
            log.error("支付宝回调处理失败: {}", result.getMessage());
            return "fail";
        }
    }

}
