package com.example.springboot.mapper;

import java.sql.Timestamp;

import com.example.springboot.model.Admin;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminMapper {
    // 查询所有管理员
    List<Admin> findAll();

    // 根据ID查询管理员
    Admin findById(@Param("adminId") Integer adminId);

    // 新增管理员
    @Options(useGeneratedKeys = true, keyProperty = "adminId")
    int insert(Admin admin);

    // 更新管理员
    int update(Admin admin);

    // 删除管理员
    int deleteById(@Param("adminId") Integer adminId);

    // 根据用户名查询管理员
    Admin findByUsername(@Param("username") String username);

    // 更新登录状态
    int updateLoginStatus(@Param("adminId") Integer adminId,
                         @Param("status") String status,
                         @Param("lastLogin") Timestamp lastLogin);
}
