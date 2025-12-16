package com.example.springboot.controller;

import com.example.springboot.model.Room;
import com.example.springboot.service.RoomService;
import com.example.springboot.common.Result;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "房间管理", description = "棋牌室房间信息管理相关接口")
public class RoomController {

    @Autowired
    private RoomService roomService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    @Operation(summary = "获取所有房间信息", description = "获取系统中所有棋牌室房间的信息列表")
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
    @Operation(summary = "获取可用房间", description = "获取当前可用的棋牌室房间列表")
    public Result<?> getAvailableRooms(@Parameter(description = "门店ID，可选") @RequestParam(required = false) Integer storeId) {
        return roomService.getAvailableRooms(storeId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取房间信息", description = "根据房间ID获取详细信息")
    public Result<?> getRoomById(@Parameter(description = "房间ID") @PathVariable Integer id) {
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
    @Operation(summary = "获取指定店铺的房间信息", description = "根据门店ID获取该门店下的所有房间信息")
    //获取指定店铺的房间信息
    public Result<?> getRoomsByStoreId(@Parameter(description = "门店ID") @PathVariable Integer storeId) {
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
    @Operation(summary = "添加房间", description = "添加新的棋牌室房间信息")
    //添加房间信息
    public Result<?> addRoom(@RequestBody Room room) {
        Result<?> result = roomService.addRoom(room);
        redisTemplate.delete("rooms:all");
        return result;
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改房间信息", description = "更新已有棋牌室房间的信息")
    //修改房间信息
    public Result<?> updateRoom(@Parameter(description = "房间ID") @PathVariable Integer id, @RequestBody Room room) {
        room.setRoomId(id);
        Result<?> result = roomService.updateRoom(room);
        redisTemplate.delete("rooms:all");
        redisTemplate.delete("rooms:id:" + id);
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除房间", description = "根据ID删除棋牌室房间")
    public Result<?> deleteRoom(@Parameter(description = "房间ID") @PathVariable Integer id) {
        Result<?> result = roomService.deleteRoom(id);
        redisTemplate.delete("rooms:all");
        redisTemplate.delete("rooms:id:" + id);
        return result;
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新房间状态", description = "更新棋牌室房间的状态（如空闲/使用中）")
    public Result<?> updateRoomStatus(
            @Parameter(description = "房间ID") @PathVariable Integer id,
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