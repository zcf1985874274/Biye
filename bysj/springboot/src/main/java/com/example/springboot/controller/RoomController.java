package com.example.springboot.controller;

import com.example.springboot.model.Room;
import com.example.springboot.service.RoomService;
import com.example.springboot.common.Result;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    //获取所有房间信息
    public Result<?> getAllRooms() {
        String key = "rooms:all";
        Result<?> result = (Result<?>) redisTemplate.opsForValue().get(key);
        if (result == null) {
            result = roomService.getAllRooms();
            redisTemplate.opsForValue().set(key, result, 1, TimeUnit.MINUTES);
        }
        return result;
    }

    @GetMapping("/available")
    public Result<?> getAvailableRooms(@RequestParam(required = false) Integer storeId) {
        return roomService.getAvailableRooms(storeId);
    }

    @GetMapping("/{id}")
    public Result<?> getRoomById(@PathVariable Integer id) {
        String key = "rooms:id:" + id;
        Result<?> result = (Result<?>) redisTemplate.opsForValue().get(key);
        if (result == null) {
            result = roomService.getRoomById(id);
            if (result != null) {
                redisTemplate.opsForValue().set(key, result, 1, TimeUnit.HOURS);
            }
        }
        return result;
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasRole('super_admin') or hasRole('admin')")
    //获取指定店铺的房间信息
    public Result<?> getRoomsByStoreId(@PathVariable Integer storeId) {
        if (storeId == null) {
            return Result.error("storeId不能为空");
        }
        // 如果是super_admin，返回所有房间
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_super_admin"))) {
            return roomService.getAllRooms();
        }
        // 否则返回指定storeId的房间
        return roomService.getRoomsByStoreId(storeId);
    }

    @PostMapping
    //添加房间信息
    public Result<?> addRoom(@RequestBody Room room) {
        Result<?> result = roomService.addRoom(room);
        redisTemplate.delete("rooms:all");
        return result;
    }

    @PutMapping("/{id}")
    //修改房间信息
    public Result<?> updateRoom(@PathVariable Integer id, @RequestBody Room room) {
        room.setRoomId(id);
        Result<?> result = roomService.updateRoom(room);
        redisTemplate.delete("rooms:all");
        redisTemplate.delete("rooms:id:" + id);
        return result;
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteRoom(@PathVariable Integer id) {
        Result<?> result = roomService.deleteRoom(id);
        redisTemplate.delete("rooms:all");
        redisTemplate.delete("rooms:id:" + id);
        return result;
    }

    @Operation(summary = "更新房间状态")
    @PatchMapping("/{id}/status")
    public Result<?> updateRoomStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> requestBody) {
        String status = requestBody.get("status");
        if (status == null) {
            return Result.error("状态参数不能为空");
        }
        Result<?> result = roomService.updateRoomStatus(id, status);
        redisTemplate.delete("rooms:all");
        redisTemplate.delete("rooms:id:" + id);
        return result;
    }
}