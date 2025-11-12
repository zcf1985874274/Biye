package com.example.springboot.service.impl;

import com.example.springboot.common.Result;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.model.Admin;
import com.example.springboot.service.AdminService;
import com.example.springboot.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public List<Admin> findAll() {
        return adminMapper.findAll();
    }

    @Override
    public Admin findById(Integer adminId) {
        return adminMapper.findById(adminId);
    }

    @Override
    public Admin findByUsername(String username) {
        return adminMapper.findByUsername(username);
    }

    @Override
    public int save(Admin admin) {
        return adminMapper.insert(admin);
    }

    @Override
    public int update(Admin admin) {
        return adminMapper.update(admin);
    }

    @Override
    public int delete(Integer adminId) {
        return adminMapper.deleteById(adminId);
    }

    @Override
    // 添加管理员账号
    public Result<Admin> register(Admin admin) {
        // 检查用户名是否已存在
        Admin existingAdmin = adminMapper.findByUsername(admin.getUsername());
        if (existingAdmin != null) {
            return Result.error("管理员用户名已存在");
        }
        // 设置默认状态
        admin.setStatus("未登录");
        admin.setLastLogin(new Timestamp(System.currentTimeMillis()));
        // 保存管理员
        adminMapper.insert(admin);
        // 不返回密码
        admin.setPassword(null);
        return Result.success(admin);
    }

    @Override
        //管理员登录验证
    public Result<String> login(String username, String password, Integer storeId) {
        System.out.println("开始管理员登录验证，用户名：" + username);
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            System.out.println("管理员不存在：" + username);
            return Result.error("管理员不存在");
        }
        System.out.println("找到管理员：" + username);
        System.out.println("数据库中的密码：" + admin.getPassword());
        boolean passwordMatch = password.equals(admin.getPassword());
        System.out.println("密码验证结果：" + passwordMatch);
        if (!passwordMatch) {
            return Result.error("密码错误");
        }
        // 更新管理员状态和最后登录时间
        admin.setStatus("登录");
        admin.setLastLogin(new Timestamp(System.currentTimeMillis()));
        adminMapper.updateLoginStatus(admin.getAdminId(), admin.getStatus(), admin.getLastLogin());
        // 生成JWT令牌，包含storeId
        Map<String, Object> claims = new HashMap<>();
        claims.put("storeId", storeId);
        String token = jwtUtil.generateToken(username, admin.getAdminId().longValue(), claims);
        System.out.println("管理员登录成功，生成token：" + token);
        return Result.success(token);
    }

    @Override
    public void logout(Integer adminId) {
        Admin admin = adminMapper.findById(adminId);
        if (admin != null) {
            admin.setStatus("未登录");
            adminMapper.updateLoginStatus(adminId, admin.getStatus(), admin.getLastLogin());
        }
    }
}
