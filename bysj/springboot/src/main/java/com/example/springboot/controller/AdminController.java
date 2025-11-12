package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.model.Admin;
import com.example.springboot.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @GetMapping
    //获取所有管理员信息
    public Result<List<Admin>> findAll() {
        String key = "admins:all";
        List<Admin> admins = (List<Admin>) redisTemplate.opsForValue().get(key);
        if (admins == null) {
            admins = adminService.findAll();
            redisTemplate.opsForValue().set(key, admins, 1, TimeUnit.HOURS);
        }
        return Result.success(admins);
    }

    @GetMapping("/{id}")
    public Result<Admin> findById(@PathVariable Integer id) {
        String key = "admins:id:" + id;
        Admin admin = (Admin) redisTemplate.opsForValue().get(key);
        if (admin == null) {
            admin = adminService.findById(id);
            if (admin != null) {
                redisTemplate.opsForValue().set(key, admin, 1, TimeUnit.HOURS);
            }
        }
        return Result.success(admin);
    }

    @PostMapping("/register")
    public Result<Admin> register(@RequestBody Admin admin) {
        return adminService.register(admin);
    }

    @PostMapping
    public Result<Integer> save(@RequestBody Admin admin) {
        int result = adminService.save(admin);
        if (result > 0) {
            // 清除相关缓存
            redisTemplate.delete("admins:all");
            return Result.success(result);
        }
        return Result.error("保存失败");
    }

    @PutMapping
    //更新管理员信息
    public Result<Integer> update(@RequestBody Admin admin) {
        int result = adminService.update(admin);
        if (result > 0) {
            // 清除相关缓存
            redisTemplate.delete("admins:all");
            redisTemplate.delete("admins:id:" + admin.getAdminId());
            if (admin.getUsername() != null) {
                redisTemplate.delete("admins:username:" + admin.getUsername());
            }
            return Result.success(result);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Integer> delete(@PathVariable Integer id) {
        // 先获取要删除的管理员信息，用于清除缓存
        Admin adminToDelete = adminService.findById(id);
        int result = adminService.delete(id);
        if (result > 0) {
            // 清除相关缓存
            redisTemplate.delete("admins:all");
            redisTemplate.delete("admins:id:" + id);
            if (adminToDelete != null && adminToDelete.getUsername() != null) {
                redisTemplate.delete("admins:username:" + adminToDelete.getUsername());
            }
            return Result.success(result);
        }
        return Result.error("删除失败");
    }
    //管理员登录接口
    @PostMapping("/login")
    public Result<String> login(@RequestBody Admin admin) {
        return adminService.login(admin.getUsername(), admin.getPassword(),admin.getStoreId());
    }
    //管理员退出登录接口
    @PostMapping("/logout/{id}")
    public Result<Void> logout(@PathVariable Integer id) {
        adminService.logout(id);
        System.out.println(id);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Admin> getAdminInfo(@RequestParam String username) {
        try {
            String key = "admins:username:" + username;
            Admin admin = (Admin) redisTemplate.opsForValue().get(key);
            if (admin == null) {
                admin = adminService.findByUsername(username);
                if (admin == null) {
                    return Result.error("管理员不存在");
                }
                // 缓存管理员信息（不包含密码）
                Admin cacheAdmin = new Admin();
                cacheAdmin.setAdminId(admin.getAdminId());
                cacheAdmin.setUsername(admin.getUsername());
                cacheAdmin.setRole(admin.getRole() != null ? admin.getRole() : "admin");
                cacheAdmin.setStatus(admin.getStatus());
                cacheAdmin.setLastLogin(admin.getLastLogin());
                redisTemplate.opsForValue().set(key, cacheAdmin, 1, TimeUnit.HOURS);
                admin = cacheAdmin;
            }
            
            System.out.println("返回的管理员信息: " + admin); // 调试日志
            return Result.success(admin);
        } catch (Exception e) {
            System.err.println("获取管理员信息异常: " + e.getMessage());
            return Result.error("获取管理员信息失败");
        }
    }
}
