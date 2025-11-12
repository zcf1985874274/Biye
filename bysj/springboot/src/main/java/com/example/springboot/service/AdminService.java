package com.example.springboot.service;

import com.example.springboot.common.Result;
import com.example.springboot.model.Admin;
import java.util.List;

public interface AdminService {
    List<Admin> findAll();
    Admin findById(Integer adminId);
    Admin findByUsername(String username);
    int save(Admin admin);
    int update(Admin admin);
    int delete(Integer adminId);
    Result<Admin> register(Admin admin);
    Result<String> login(String username, String password, Integer storeId);
    void logout(Integer adminId);
}
