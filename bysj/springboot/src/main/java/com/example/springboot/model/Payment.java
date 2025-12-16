package com.example.springboot.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Payment {
    private Integer paymentId;
    private Integer userId;
    private String paymentMethod;
    private BigDecimal amount;
    private String paymentStatus;
    private Timestamp paymentTime;
    private String alipayTransactionId;

    // Getters and Setters
    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Timestamp getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(Timestamp paymentTime) {
        this.paymentTime = paymentTime;
    }

    public String getAlipayTransactionId() {
        return alipayTransactionId;
    }

    public void setAlipayTransactionId(String alipayTransactionId) {
        this.alipayTransactionId = alipayTransactionId;
    }
}