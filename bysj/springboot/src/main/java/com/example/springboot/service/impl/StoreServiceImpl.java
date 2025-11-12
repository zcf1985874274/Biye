package com.example.springboot.service.impl;

import com.example.springboot.common.Result;
import com.example.springboot.mapper.StoreMapper;
import com.example.springboot.model.Store;
import com.example.springboot.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StoreServiceImpl implements StoreService {
    @Autowired
    private StoreMapper storeMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String STORE_CACHE_PREFIX = "store:";
    private static final String STORE_LIST_CACHE_KEY = "store:list";

    @Override
    public List<Store> getAllStores() {
        // 先从Redis缓存中获取
        List<Store> cachedStores = (List<Store>) redisTemplate.opsForValue().get(STORE_LIST_CACHE_KEY);
        if (cachedStores != null) {
            return cachedStores;
        }
        
        // 缓存中没有，从数据库查询
        List<Store> stores = storeMapper.findAll();
        
        // 将结果存入Redis缓存，设置过期时间为30分钟
        redisTemplate.opsForValue().set(STORE_LIST_CACHE_KEY, stores, 30, TimeUnit.MINUTES);
        
        return stores;
    }

    @Override
    public Store getStoreById(Integer storeId) {
        String cacheKey = STORE_CACHE_PREFIX + storeId;
        
        // 先从Redis缓存中获取
        Store cachedStore = (Store) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStore != null) {
            return cachedStore;
        }
        
        // 缓存中没有，从数据库查询
        Store store = storeMapper.findById(storeId);
        
        if (store != null) {
            // 将结果存入Redis缓存，设置过期时间为30分钟
            redisTemplate.opsForValue().set(cacheKey, store, 30, TimeUnit.MINUTES);
        }
        
        return store;
    }

    @Override
    public Result<?> addStore(Store store) {
        storeMapper.insert(store);
        
        // 清除店铺列表缓存
        redisTemplate.delete(STORE_LIST_CACHE_KEY);
        return null;
    }

    @Override
    public Result<?> updateStore(Store store) {
        storeMapper.update(store);
        
        // 清除店铺列表缓存
        redisTemplate.delete(STORE_LIST_CACHE_KEY);
        
        // 清除特定店铺的缓存
        if (store.getStoreId() != null) {
            redisTemplate.delete(STORE_CACHE_PREFIX + store.getStoreId());
        }
        return null;
    }

    @Override
    public Result<?> deleteStore(Integer storeId) {
        storeMapper.delete(storeId);
        
        // 清除店铺列表缓存
        redisTemplate.delete(STORE_LIST_CACHE_KEY);
        
        // 清除特定店铺的缓存
        redisTemplate.delete(STORE_CACHE_PREFIX + storeId);
        return null;
    }
}