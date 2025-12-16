package com.example.springboot.service.impl;

import com.example.springboot.mapper.RoomMapper;
import com.example.springboot.mapper.UsageRecordMapper;
import com.example.springboot.model.Room;
import com.example.springboot.model.UsageRecord;
import java.util.Date;
import com.example.springboot.service.RoomService;
import com.example.springboot.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private UsageRecordMapper usageRecordMapper;

    @Override
    public Result<List<Room>> getAllRooms() {
        try {
            List<Room> rooms = roomMapper.findAll();
            System.out.println(rooms);
            return Result.success(rooms);
        } catch (Exception e) {
            return Result.error("获取房间列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Room> getRoomById(Integer roomId) {
        try {
            Room room = roomMapper.findById(roomId);
            if (room == null) {
                return Result.error("房间不存在");
            }
            return Result.success(room);
        } catch (Exception e) {
            return Result.error("获取房间详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Room> addRoom(Room room) {
        try {
            // 验证房间名称是否已存在
            List<Room> existingRooms = roomMapper.findAll();
            boolean nameExists = existingRooms.stream()
                    .anyMatch(r -> r.getRoomName().equals(room.getRoomName()));
            if (nameExists) {
                return Result.error("房间名称已存在");
            }

            // 设置默认状态
            room.setStatus("空闲");

            roomMapper.insert(room);
            return Result.success(room);
        } catch (Exception e) {
            return Result.error("添加房间失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Room> updateRoom(Room room) {
        try {
            Room existingRoom = roomMapper.findById(room.getRoomId());
            if (existingRoom == null) {
                return Result.error("房间不存在");
            }

            roomMapper.update(room);
            return Result.success(room);
        } catch (Exception e) {
            return Result.error("更新房间失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteRoom(Integer roomId) {
        try {
            Room existingRoom = roomMapper.findById(roomId);
            if (existingRoom == null) {
                return Result.error("房间不存在");
            }

            // 检查房间是否正在使用中
            if ("使用中".equals(existingRoom.getStatus())) {
                return Result.error("房间正在使用中，不能删除");
            }

            roomMapper.deleteById(roomId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除房间失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Room>> getAvailableRooms(Integer storeId) {
        try {
            List<Room> rooms = roomMapper.findAvailableRooms(storeId);
            return Result.success(rooms);
        } catch (Exception e) {
            return Result.error("获取可用房间失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> updateRoomStatus(Integer roomId, String status) {
        try {
            // 验证房间是否存在
            Room existingRoom = roomMapper.findById(roomId);
            if (existingRoom == null) {
                return Result.error("房间不存在");
            }

            // 验证状态值是否有效
            if (status == null || (!status.equals("空闲") && !status.equals("使用中"))) {
                return Result.error("无效的房间状态");
            }

            // 更新状态
            int result = roomMapper.updateStatus(roomId, status);
            if (result == 0) {
                return Result.error("更新房间状态失败");
            }
            
            // 清除Redis缓存
            redisTemplate.delete("rooms:all");
            redisTemplate.delete("rooms:id:" + roomId);
            
            return Result.success();
        } catch (Exception e) {
            return Result.error("更新房间状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result<?> getRoomsByStoreId(Integer storeId) {
        return Result.success(roomMapper.findByStoreId(storeId));
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void checkAndUpdateExpiredRooms() {
        try {
            // 1. 查询所有状态为"使用中"的房间
            List<Room> occupiedRooms = roomMapper.findByStatus("使用中");

            // 2. 检查每个房间的使用记录
            for (Room room : occupiedRooms) {
                // 获取该房间最新的使用记录
                UsageRecord latestRecord = usageRecordMapper.findLatestByRoomId(room.getRoomId());

                if (latestRecord != null && latestRecord.getEndTime().before(new Date())) {
                    // 3. 使用时间已结束，更新房间状态和密码
                    room.setStatus("空闲");
                    // 生成6位随机数密码
                    int randomPassword = (int) (Math.random() * 900000) + 100000;
                    room.setPassword(randomPassword);
                    roomMapper.update(room);

                    // 4. 更新 Redis 缓存中的房间信息
                    String redisKey = "room:" + room.getRoomId();
                    redisTemplate.opsForValue().set(redisKey, room);

                    log.info("房间ID: {} 使用时间已结束，状态已更新为空闲，密码已重置为: {}", room.getRoomId(), randomPassword);
                }
            }
        } catch (Exception e) {
            log.error("检查过期房间状态失败: {}", e.getMessage());
        }
    }
}
