package com.example.springboot.service.impl;

import com.example.springboot.mapper.UsageRecordMapper;
import com.example.springboot.model.UsageRecord;
import com.example.springboot.service.UsageRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UsageRecordServiceImpl implements UsageRecordService {

    @Autowired
    private UsageRecordMapper usageRecordMapper;

    @Override
    public List<Map<String, Object>> getDailyProfit(Integer storeId, String startDate, String endDate) {
        return usageRecordMapper.getDailyProfit(storeId, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getMonthlyProfit(Integer storeId, Integer year) {
        return usageRecordMapper.getMonthlyProfit(storeId, year);
    }

    @Override
    public List<Map<String, Object>> getYearlyProfit(Integer storeId) {
        return usageRecordMapper.getYearlyProfit(storeId);
    }



    @Override
    public List<UsageRecord> findAll() {
        return usageRecordMapper.findAll();
    }

    @Override
    public UsageRecord findById(Integer recordId) {
        return usageRecordMapper.findById(recordId);
    }

    @Override
    public List<UsageRecord> findByRoomId(Integer roomId) {
        return usageRecordMapper.findByRoomId(roomId);
    }

    @Override
    public List<UsageRecord> findByUserId(Integer userId) {
        return usageRecordMapper.findByUserId(userId);
    }

    @Override
    public int save(UsageRecord usageRecord) {
        return usageRecordMapper.insert(usageRecord);
    }

    @Override
    public int update(UsageRecord usageRecord) {
        return usageRecordMapper.update(usageRecord);
    }

    @Override
    public int delete(Integer recordId) {
        return usageRecordMapper.deleteById(recordId);
    }

    @Override
    public List<UsageRecord> findByUserIdAndDate(Integer userId, Integer year, Integer month) {
        return usageRecordMapper.findByUserIdAndDate(userId, year, month);
    }

    @Override
    public List<UsageRecord> findByStoreId(Integer storeId) {
        return usageRecordMapper.findByStoreId(storeId);
    }
}