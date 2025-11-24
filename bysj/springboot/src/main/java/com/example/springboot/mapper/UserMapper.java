package com.example.springboot.mapper;

import com.example.springboot.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 插入新用户
     * @param user 用户对象
     * @return 影响的行数
     */
    int insert(User user);

    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     * @return 用户对象
     */
    User findById(Integer userId);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    User findByUsername(String username);

    /**
     * 更新用户状态和最后登录时间
     * @param userId 用户ID
     * @param status 状态
     * @param lastLogin 最后登录时间
     * @return 影响的行数
     */
    int updateStatus(@Param("userId") Integer userId, @Param("status") String status, @Param("lastLogin") Date lastLogin);

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 影响的行数
     */
    int update(User user);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 根据ID删除用户
     * @param userId 用户ID
     * @return 影响的行数
     */
    int deleteById(Integer userId);
    
    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param password 新密码
     * @return 影响的行数
     */
    int updatePassword(@Param("userId") Integer userId, @Param("password") String password);




}