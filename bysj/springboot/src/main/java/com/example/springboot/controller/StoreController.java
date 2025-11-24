package com.example.springboot.controller;

import com.example.springboot.model.Store;
import com.example.springboot.service.StoreService;
import com.example.springboot.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/stores")
@Tag(name = "门店管理", description = "棋牌室门店信息管理相关接口")
public class StoreController {
    @Autowired
    private StoreService storeService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    @Operation(summary = "获取所有门店信息", description = "获取系统中所有棋牌室门店的信息列表")
    //获取所有店铺信息
    public Result<?> getAllStores() {
        String key = "stores:all";
        Result<?> result = (Result<?>) redisTemplate.opsForValue().get(key);
        if (result == null) {
            List<Store> stores = storeService.getAllStores();
            result = Result.success(stores);
            redisTemplate.opsForValue().set(key, result, 1, TimeUnit.MINUTES);
        }
        return result;
    }

    @GetMapping("/{storeId}")
    @Operation(summary = "根据ID获取门店信息", description = "根据门店ID获取详细信息")
    public Result<?> getStoreById(@Parameter(description = "门店ID") @PathVariable Integer storeId) {
        String key = "stores:id:" + storeId;
        Result<?> result = (Result<?>) redisTemplate.opsForValue().get(key);
        if (result == null) {
            Store store = storeService.getStoreById(storeId);
            if (store != null) {
                result = Result.success(store);
                redisTemplate.opsForValue().set(key, result, 1, TimeUnit.HOURS);
            } else {
                result = Result.error("店铺不存在");
            }
        }
        return result;
    }

    @PostMapping
    @Operation(summary = "添加门店", description = "添加新的棋牌室门店信息")
    //添加店铺信息
    public Result<?> addStore(@RequestBody Store store) {
        Result<?> result = storeService.addStore(store);
        redisTemplate.delete("stores:all");
        return result;
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改门店信息", description = "更新已有棋牌室门店的信息")
    //修改店铺信息
    public Result<?> updateStore(@Parameter(description = "门店ID") @PathVariable Integer id, @RequestBody Store store) {
        store.setStoreId(id);
        Result<?> result = storeService.updateStore(store);
        redisTemplate.delete("stores:all");
        redisTemplate.delete("stores:id:" + id);
        return result;
    }

    @DeleteMapping("/{storeId}")
    @Operation(summary = "删除门店", description = "根据ID删除棋牌室门店")
    public Result<?> deleteStore(@Parameter(description = "门店ID") @PathVariable Integer storeId) {
        Result<?> result = storeService.deleteStore(storeId);
        redisTemplate.delete("stores:all");
        redisTemplate.delete("stores:id:" + storeId);
        return result;
    }
    

}