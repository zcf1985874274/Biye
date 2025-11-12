package com.example.springboot.service;

import com.example.springboot.common.Result;
import com.example.springboot.model.Store;
import java.util.List;

public interface StoreService {
    List<Store> getAllStores();
    Store getStoreById(Integer storeId);
    Result<?> addStore(Store store);
    Result<?> updateStore(Store store);
    Result<?> deleteStore(Integer storeId);
}