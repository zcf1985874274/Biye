package com.example.springboot.controller;

import com.example.springboot.model.Store;
import com.example.springboot.service.StoreService;
import com.example.springboot.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/stores")
public class StoreController {
    @Autowired
    private StoreService storeService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
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
    public Result<?> getStoreById(@PathVariable Integer storeId) {
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
    //添加店铺信息
    public Result<?> addStore(@RequestBody Store store) {
        Result<?> result = storeService.addStore(store);
        redisTemplate.delete("stores:all");
        return result;
    }

    @PutMapping("/{id}")
    //修改店铺信息
    public Result<?> updateStore(@PathVariable Integer id, @RequestBody Store store) {
        store.setStoreId(id);
        Result<?> result = storeService.updateStore(store);
        redisTemplate.delete("stores:all");
        redisTemplate.delete("stores:id:" + id);
        return result;
    }

    @DeleteMapping("/{storeId}")
    public Result<?> deleteStore(@PathVariable Integer storeId) {
        Result<?> result = storeService.deleteStore(storeId);
        redisTemplate.delete("stores:all");
        redisTemplate.delete("stores:id:" + storeId);
        return result;
    }
    

}