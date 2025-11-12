package com.example.springboot.mapper;

import com.example.springboot.model.Store;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface StoreMapper {
    List<Store> findAll();
    Store findById(Integer storeId);
    void insert(Store store);
    void update(Store store);
    void delete(Integer storeId);
}