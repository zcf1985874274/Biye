package com.example.springboot.service;

import com.example.springboot.model.User;
import com.example.springboot.common.Result;

import java.util.List;

public interface UserService {

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param phone 电话
     * @return 注册结果
     */
    Result<User> register(String username, String password, String email, String phone);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果，包含JWT token
     */
    Result<String> login(String username, String password);

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    User findByUsername(String username);
    
    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户对象
     */
    User findById(Integer userId);

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态
     * @return 更新结果
     */
    Result<Void> updateStatus(Integer userId, String status);

    /**
     * 用户退出登录
     * @param username 用户名
     * @return 退出结果
     */
    Result<Void> logout(String username);
    
    /**
     * 重置用户密码
     * @param username 用户名
     * @param newPassword 新密码
     * @return 重置结果
     */
    Result<Void> resetPassword(String username, String newPassword);

    /**
     * 获取所有用户
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 根据ID删除用户
     * @param userId 用户ID
     * @return 删除结果
     */
    Result<Void> deleteById(Integer userId);

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 更新结果
     */
    Result<Void> updateUser(User user);
}
