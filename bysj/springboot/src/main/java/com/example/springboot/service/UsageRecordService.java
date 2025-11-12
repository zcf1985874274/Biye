package com.example.springboot.service;

import com.example.springboot.model.UsageRecord;
import java.util.List;
import java.util.Map;

public interface UsageRecordService {
    List<UsageRecord> findAll();
    UsageRecord findById(Integer recordId);
    List<UsageRecord> findByRoomId(Integer roomId);
    List<UsageRecord> findByUserId(Integer userId);
    int save(UsageRecord usageRecord);
    int update(UsageRecord usageRecord);
    int delete(Integer recordId);
    List<UsageRecord> findByUserIdAndDate(Integer userId, Integer year, Integer month);
    List<Map<String, Object>> getDailyProfit(Integer storeId, String startDate, String endDate);
    List<Map<String, Object>> getMonthlyProfit(Integer storeId, Integer year);
    List<Map<String, Object>> getYearlyProfit(Integer storeId);

    List<UsageRecord> findByStoreId(Integer storeId);
}
