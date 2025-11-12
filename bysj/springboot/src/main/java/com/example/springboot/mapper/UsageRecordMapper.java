package com.example.springboot.mapper;

import com.example.springboot.model.UsageRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UsageRecordMapper {
    // 查询所有记录
    List<UsageRecord> findAll();

    // 根据ID查询记录
    UsageRecord findById(@Param("recordId") Integer recordId);

    // 新增记录
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    int insert(UsageRecord usageRecord);

    // 更新记录
    int update(UsageRecord usageRecord);

    // 删除记录
    int deleteById(@Param("recordId") Integer recordId);

    // 根据房间ID查询记录
    List<UsageRecord> findByRoomId(@Param("roomId") Integer roomId);

    // 根据用户ID查询记录
    List<UsageRecord> findByUserId(@Param("userId") Integer userId);
    
    // 根据用户ID、年份和月份查询记录
    List<UsageRecord> findByUserIdAndDate(
        @Param("userId") Integer userId,
        @Param("year") Integer year,
        @Param("month") Integer month
    );
    
    // 根据房间ID查询最新的使用记录
    UsageRecord findLatestByRoomId(@Param("roomId") Integer roomId);

    // 查询每日盈利数据，支持按店铺ID和日期范围过滤
    List<Map<String, Object>> getDailyProfit(
        @Param("storeId") Integer storeId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );

    // 查询每月盈利数据，支持按店铺ID和年份过滤
    List<Map<String, Object>> getMonthlyProfit(
        @Param("storeId") Integer storeId,
        @Param("year") Integer year
    );

    // 查询每年盈利数据，支持按店铺ID过滤
    List<Map<String, Object>> getYearlyProfit(
        @Param("storeId") Integer storeId
    );

    // 根据店铺ID查询记录
    List<UsageRecord> findByStoreId(@Param("storeId") Integer storeId);
}
