package com.example.springboot.controller;

import com.example.springboot.dto.LoginDTO;
import com.example.springboot.dto.RegisterDTO;
import com.example.springboot.model.User;
import com.example.springboot.service.UserService;
import com.example.springboot.common.Result;
import com.example.springboot.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    //添加用户接口
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterDTO registerDTO) {
        return userService.register(
                registerDTO.getUsername(),
                registerDTO.getPassword(),
                registerDTO.getEmail(),
                registerDTO.getPhone()
        );
    }

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果，包含JWT token
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO loginDTO) {
        System.out.println("用户登录请求: " + loginDTO.getUsername());
        Result<String> result = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        if (result.getCode() == 200) {
            //测试代码
            System.out.println("生成的JWT Token: " + result.getData());
            // 验证生成的token是否符合JWT格式
            if (!result.getData().matches("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*$")) {
                System.err.println("生成的Token不符合JWT格式!");
            }
        }
        return result;
    }

    /**
     * 获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    //获取用户信息接口
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestParam String username) {
        String key = "users:info:" + username;
        User user = (User) redisTemplate.opsForValue().get(key);
        if (user == null) {
            user = userService.findByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            // 不返回密码
            user.setPassword(null);
            redisTemplate.opsForValue().set(key, user, 1, TimeUnit.HOURS);
        }
        return Result.success(user);
    }

    /**
     * 用户退出登录
     * @param username 用户名
     * @return 退出结果
     */
    /**
     * 获取所有用户信息
     * @return 用户列表
     */
    //获取所有用户信息接口
    @GetMapping("/all")
    public Result<List<User>> getAllUsers() {
        String key = "users:all";
        List<User> users = (List<User>) redisTemplate.opsForValue().get(key);
        if (users == null) {
            users = userService.findAll();
            // 不返回密码
            users.forEach(user -> user.setPassword(null));
            redisTemplate.opsForValue().set(key, users, 1, TimeUnit.HOURS);
        }
        return Result.success(users);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader, 
                             @RequestParam String username) {
        // 允许没有Authorization头的请求通过
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return userService.logout(username);
        }
        
        // 如果有有效令牌，也允许登出
        return userService.logout(username);
    }

    /**
     * 根据ID删除用户
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Integer userId) {
        // 先获取用户信息用于清除缓存
        User userToDelete = userService.findById(userId);
        Result<Void> result = userService.deleteById(userId);
        
        if (result.getCode() == 200) {
            // 清除相关缓存
            redisTemplate.delete("users:all");
            redisTemplate.delete("users:id:" + userId);
            if (userToDelete != null && userToDelete.getUsername() != null) {
                redisTemplate.delete("users:info:" + userToDelete.getUsername());
            }
        }
        return result;
    }

    /**
     * 管理员更新用户信息
     * @param user 用户对象
     * @return 更新结果
     */
    @PutMapping
    //修改用户信息接口
    public Result<Void> updateUser(@RequestBody User user) {
        Result<Void> result = userService.updateUser(user);
        
        if (result.getCode() == 200) {
            // 清除相关缓存
            redisTemplate.delete("users:all");
            redisTemplate.delete("users:id:" + user.getUserId());
            if (user.getUsername() != null) {
                redisTemplate.delete("users:info:" + user.getUsername());
            }
        }
        return result;
    }

    /**
     * 普通用户修改自己的账号信息
     * @param user 用户信息
     * @return 操作结果
     */


    @PutMapping("/self")
    public Result<Void> updateSelfInfo(@RequestBody User user, @RequestHeader("Authorization") String token) {
        // 从token中提取用户名和用户ID
        String jwtToken = token.replace("Bearer ", "");

        String username = jwtUtil.extractUsername(jwtToken);
        Long userId = jwtUtil.extractUserId(jwtToken);

        if (username == null || userId == null) {
            return Result.error("未授权访问");
        }
        // 设置user_id
        user.setUserId(Math.toIntExact(userId));
        return userService.updateUser(user);
    }
}
