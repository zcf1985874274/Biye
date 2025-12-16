package com.example.springboot.controller;

import com.example.springboot.model.UsageRecord;
import com.example.springboot.service.UsageRecordService;
import com.example.springboot.service.RoomService;
import com.example.springboot.util.JwtUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "使用记录管理")
@RestController
@RequestMapping("/api/usage-records")
public class UsageRecordController {

    @Autowired
    private UsageRecordService usageRecordService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RoomService roomService;
    @Operation(summary = "获取所有使用记录")
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll() {
        Map<String, Object> response = new HashMap<>();
        try {
            String key = "usage_records:all";
            List<UsageRecord> records = (List<UsageRecord>) redisTemplate.opsForValue().get(key);
            if (records == null) {
                records = usageRecordService.findAll();
                redisTemplate.opsForValue().set(key, records, 30, TimeUnit.MINUTES);
            }
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "根据ID获取使用记录", description = "根据记录ID获取使用记录的详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@Parameter(description = "记录ID") @PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            String key = "usage_records:id:" + id;
            UsageRecord record = (UsageRecord) redisTemplate.opsForValue().get(key);
            if (record == null) {
                record = usageRecordService.findById(id);
                if (record == null) {
                    response.put("code", 404);
                    response.put("message", "记录不存在");
                    return ResponseEntity.status(404).body(response);
                }
                redisTemplate.opsForValue().set(key, record, 1, TimeUnit.HOURS);
            }
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", record);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "根据房间ID查询使用记录", description = "获取指定棋牌室房间的所有使用记录")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Map<String, Object>> findByRoomId(@Parameter(description = "房间ID") @PathVariable Integer roomId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String key = "usage_records:room:" + roomId;
            List<UsageRecord> records = (List<UsageRecord>) redisTemplate.opsForValue().get(key);
            if (records == null) {
                records = usageRecordService.findByRoomId(roomId);
                redisTemplate.opsForValue().set(key, records, 30, TimeUnit.MINUTES);
            }
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "根据用户ID查询使用记录", description = "获取指定用户的所有使用记录")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> findByUserId(@Parameter(description = "用户ID") @PathVariable Integer userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String key = "usage_records:user:" + userId;
            List<UsageRecord> records = (List<UsageRecord>) redisTemplate.opsForValue().get(key);
            if (records == null) {
                records = usageRecordService.findByUserId(userId);
                redisTemplate.opsForValue().set(key, records, 30, TimeUnit.MINUTES);
            }
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "获取当前用户的使用记录", description = "获取当前登录用户的使用记录，可按年月筛选")
    @GetMapping("/self")
    public ResponseEntity<Map<String, Object>> getSelfUsageRecords(
        @RequestHeader("Authorization") String token,
        @Parameter(description = "年份，可选") @RequestParam(required = false) Integer year,
        @Parameter(description = "月份，可选") @RequestParam(required = false) Integer month
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 从JWT中提取用户ID
            String jwtToken = token.replace("Bearer ", "");
            Integer userId = Math.toIntExact(jwtUtil.extractUserId(jwtToken));
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权访问");
                return ResponseEntity.status(401).body(response);
            }
            // 根据 year 和 month 过滤记录
            List<UsageRecord> records = usageRecordService.findByUserIdAndDate(userId, year, month);
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 记录错误堆栈

            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "创建使用记录", description = "创建新的棋牌室使用记录")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody UsageRecord usageRecord) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 添加调试日志
            System.out.println("接收到的使用记录数据: " + usageRecord);
            System.out.println("roomId: " + usageRecord.getRoomId());
            System.out.println("userId: " + usageRecord.getUserId());
            System.out.println("storeId: " + usageRecord.getStoreId());
            System.out.println("startTime: " + usageRecord.getStartTime());
            System.out.println("endTime: " + usageRecord.getEndTime());
            System.out.println("paymentId: " + usageRecord.getPaymentId());
            
            // 验证必要字段（包括 storeId）
            if (usageRecord.getRoomId() == null || usageRecord.getUserId() == null
                    || usageRecord.getStartTime() == null || usageRecord.getEndTime() == null
                    || usageRecord.getStoreId() == null) { // 新增 storeId 校验
                response.put("code", 400);
                response.put("message", "缺少必要参数: roomId=" + usageRecord.getRoomId() + 
                    ", userId=" + usageRecord.getUserId() + ", storeId=" + usageRecord.getStoreId() + 
                    ", startTime=" + (usageRecord.getStartTime() != null ? usageRecord.getStartTime().toString() : "null") + 
                    ", endTime=" + (usageRecord.getEndTime() != null ? usageRecord.getEndTime().toString() : "null"));
                return ResponseEntity.badRequest().body(response);
            }
            
            // 时间字段已经是Timestamp类型，无需转换
            // 如果需要验证时间格式，可以在这里添加验证逻辑
            
            // 创建使用记录
            int result = usageRecordService.save(usageRecord);
            
            // 更新对应房间状态为"使用中"
            if (usageRecord.getRoomId() != null) {
                roomService.updateRoomStatus(usageRecord.getRoomId(), "使用中");
            }
            
            // 清除相关缓存
            clearUsageRecordCache(usageRecord.getUserId(), usageRecord.getRoomId(), usageRecord.getStoreId());
            
            response.put("code", 201);
            response.put("message", "创建成功");
            response.put("data", usageRecord);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "创建失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "更新使用记录", description = "更新已有的使用记录信息")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "记录ID") @PathVariable Integer id, @RequestBody UsageRecord usageRecord) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 先获取原记录信息，用于清除缓存
            UsageRecord oldRecord = usageRecordService.findById(id);
            usageRecord.setRecordId(id);
            int result = usageRecordService.update(usageRecord);
            if (result == 0) {
                response.put("code", 404);
                response.put("message", "记录不存在");
                return ResponseEntity.status(404).body(response);
            }
            // 清除相关缓存
            if (oldRecord != null) {
                clearUsageRecordCache(oldRecord.getUserId(), oldRecord.getRoomId(), oldRecord.getStoreId());
            }
            clearUsageRecordCache(usageRecord.getUserId(), usageRecord.getRoomId(), usageRecord.getStoreId());
            redisTemplate.delete("usage_records:id:" + id);
            response.put("code", 200);
            response.put("message", "更新成功");
            response.put("data", usageRecord);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "删除使用记录", description = "根据ID删除使用记录")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@Parameter(description = "记录ID") @PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 先获取要删除的记录信息，用于清除缓存
            UsageRecord recordToDelete = usageRecordService.findById(id);
            
            int result = usageRecordService.delete(id);
            if (result == 0) {
                response.put("code", 404);
                response.put("message", "记录不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            // 清除相关缓存
            if (recordToDelete != null) {
                clearUsageRecordCache(recordToDelete.getUserId(), recordToDelete.getRoomId(), recordToDelete.getStoreId());
            }
            redisTemplate.delete("usage_records:id:" + id);
            
            response.put("code", 200);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @Operation(summary = "根据店铺ID查询使用记录")
    @CrossOrigin(origins = "http://localhost:8081")
    @GetMapping("/store/{storeId}")
    public ResponseEntity<Map<String, Object>> findByStoreId(@PathVariable Integer storeId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<UsageRecord> records = usageRecordService.findByStoreId(storeId);
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "获取每日盈利数据")
    @GetMapping("/daily-profit")
    public ResponseEntity<Map<String, Object>> getDailyProfit(
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> data = usageRecordService.getDailyProfit(storeId, startDate, endDate);
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "获取每月盈利数据")
    @GetMapping("/monthly-profit")
    public ResponseEntity<Map<String, Object>> getMonthlyProfit(
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) Integer year) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> data = usageRecordService.getMonthlyProfit(storeId, year);
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "获取每年盈利数据")
    @GetMapping("/yearly-profit")
    public ResponseEntity<Map<String, Object>> getYearlyProfit(
            @RequestParam(required = false) Integer storeId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> data = usageRecordService.getYearlyProfit(storeId);
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 清除使用记录相关缓存
     */
    private void clearUsageRecordCache(Integer userId, Integer roomId, Integer storeId) {
        // 清除所有记录缓存
        redisTemplate.delete("usage_records:all");
        
        // 清除用户相关缓存
        if (userId != null) {
            redisTemplate.delete("usage_records:user:" + userId);
        }
        
        // 清除房间相关缓存
        if (roomId != null) {
            redisTemplate.delete("usage_records:room:" + roomId);
        }
        
        // 清除店铺相关缓存
        if (storeId != null) {
            redisTemplate.delete("usage_records:store:" + storeId);
        }
        
        // 清除统计数据缓存
        redisTemplate.delete("usage_records:daily_profit:*");
        redisTemplate.delete("usage_records:monthly_profit:*");
        redisTemplate.delete("usage_records:yearly_profit:*");
    }
}
