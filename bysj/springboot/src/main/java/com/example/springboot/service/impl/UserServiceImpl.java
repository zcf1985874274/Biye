package com.example.springboot.service.impl;

import com.example.springboot.mapper.UserMapper;
import com.example.springboot.model.User;
import com.example.springboot.service.UserService;
import com.example.springboot.common.Result;
import com.example.springboot.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Result<User> register(String username, String password, String email, String phone) {
        // 检查用户名是否已存在
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            return Result.error("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        // 存储明文密码
        user.setPassword(password);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRegistrationDate(new Date());
        user.setLastLogin(new Date());
        user.setStatus("未登录");
        // 初始化余额为0
        user.setBalance(BigDecimal.ZERO);

        // 保存用户
        userMapper.insert(user);

        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<String> login(String username, String password) {
        System.out.println("开始登录验证，用户名：" + username);
        
        // 查找用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            System.out.println("用户不存在：" + username);
            return Result.error("用户不存在");
        }
        
        System.out.println("找到用户：" + username);
        System.out.println("数据库中的密码：" + user.getPassword());
        
        // 直接比较明文密码
        boolean passwordMatch = password.equals(user.getPassword());
        System.out.println("密码验证结果：" + passwordMatch);
        
        if (!passwordMatch) {
            return Result.error("密码错误");
        }

        // 更新用户状态和最后登录时间
        user.setStatus("登录");
        user.setLastLogin(new Date());
        userMapper.updateStatus(user.getUserId(), user.getStatus(), user.getLastLogin());

        // 生成JWT令牌
        String token = jwtUtil.generateToken(username, user.getUserId().longValue());
        System.out.println("登录成功，生成token：" + token);

        return Result.success(token);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    @Override
    public User findById(Integer userId) {
        return userMapper.findById(userId);
    }

    @Override
    public Result<Void> updateStatus(Integer userId, String status) {
        userMapper.updateStatus(userId, status, new Date());
        return Result.success();
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public Result<Void> logout(String username) {
        // 查找用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 更新用户状态为"未登录"
        userMapper.updateStatus(user.getUserId(), "未登录", new Date());

        return Result.success();
    }

    @Override
    public Result<Void> deleteById(Integer userId) {
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 删除用户
        int affectedRows = userMapper.deleteById(userId);
        if (affectedRows == 0) {
            return Result.error("删除用户失败");
        }

        return Result.success();
    }

    @Override
    public Result<Void> updateUser(User user) {
        // 检查用户是否存在
        User existingUser = userMapper.findById(user.getUserId());
        if (existingUser == null) {
            return Result.error("用户不存在");
        }

        // 更新用户信息
        int affectedRows = userMapper.update(user);
        if (affectedRows == 0) {
            return Result.error("更新用户信息失败");
        }

        return Result.success();
    }
    
    @Override
    public Result<Void> resetPassword(String username, String newPassword) {
        // 查找用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 更新密码
        user.setPassword(newPassword);
        int affectedRows = userMapper.updatePassword(user.getUserId(), newPassword);
        
        if (affectedRows == 0) {
            return Result.error("密码重置失败");
        }
        
        return Result.success();
    }
}
