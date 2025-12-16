package com.example.springboot.controller;

import com.example.springboot.dto.LoginDTO;
import com.example.springboot.dto.RegisterDTO;
import com.example.springboot.model.User;
import com.example.springboot.service.UserService;
import com.example.springboot.common.Result;
import com.example.springboot.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等相关接口")
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
    @Operation(summary = "用户注册", description = "用户使用用户名、密码、邮箱和手机号进行注册")
    public Result<User> register(@RequestBody RegisterDTO registerDTO) {
        Result<User> result = userService.register(
                registerDTO.getUsername(),
                registerDTO.getPassword(),
                registerDTO.getEmail(),
                registerDTO.getPhone()
        );
        if (result.getCode() == 200) {
            // 清除所有用户列表缓存
            redisTemplate.delete("users:all");
        }
        return result;
    }

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果，包含JWT token
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户使用用户名和密码进行登录，成功后返回JWT token")
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
    @Operation(summary = "获取用户信息", description = "根据用户名获取用户详细信息")
    public Result<User> getUserInfo(@Parameter(description = "用户名") @RequestParam String username) {
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
    @Operation(summary = "获取所有用户信息", description = "获取系统中所有用户的信息列表（管理员权限）")
    public Result<List<User>> getAllUsers() {
        String key = "users:all";
        // 强制清除缓存，确保获取最新数据
        redisTemplate.delete(key);
        List<User> users = userService.findAll();
        // 不返回密码
        users.forEach(user -> user.setPassword(null));
        redisTemplate.opsForValue().set(key, users, 1, TimeUnit.HOURS);
        return Result.success(users);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户退出登录", description = "用户退出登录，清除登录状态")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader, 
                             @Parameter(description = "用户名") @RequestParam String username) {
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
    @Operation(summary = "删除用户", description = "根据用户ID删除用户（管理员权限）")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Integer userId) {
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
    @Operation(summary = "更新用户信息", description = "管理员更新用户信息")
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
    @Operation(summary = "用户修改个人信息", description = "普通用户修改自己的账号信息")
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

    /**
     * 检查用户名是否存在并返回脱敏手机号
     * @param username 用户名
     * @return 用户信息（包含脱敏手机号）
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名", description = "检查用户名是否存在并返回脱敏手机号，用于找回密码")
    public Result<User> checkUsername(@Parameter(description = "用户名") @RequestParam String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.error("用户名不存在");
        }
        
        // 创建返回对象，只包含用户名和脱敏后的手机号
        User result = new User();
        result.setUsername(user.getUsername());
        
        // 对手机号进行脱敏处理：显示前3位和后2位，中间用*代替
        String phone = user.getPhone();
        if (phone != null && phone.length() >= 5) {
            String maskedPhone = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
            result.setPhone(maskedPhone);
        } else {
            result.setPhone(phone);
        }
        
        // 保存原始手机号到session或临时存储，用于后续验证
        // 这里使用Redis存储，设置5分钟过期时间
        String key = "reset:phone:" + username;
        redisTemplate.opsForValue().set(key, phone, 5, TimeUnit.MINUTES);
        
        return Result.success(result);
    }

    /**
     * 验证手机号
     * @param data 包含用户名和手机号的请求体
     * @return 验证结果
     */
    @PostMapping("/verify-phone")
    @Operation(summary = "验证手机号", description = "验证用户输入的手机号是否正确，用于找回密码")
    public Result<Void> verifyPhone(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String inputPhone = data.get("phone");
        
        if (username == null || inputPhone == null) {
            return Result.error("参数不完整");
        }
        
        // 从Redis中获取原始手机号
        String key = "reset:phone:" + username;
        String originalPhone = (String) redisTemplate.opsForValue().get(key);
        
        if (originalPhone == null) {
            return Result.error("验证超时，请重新开始");
        }
        
        if (!inputPhone.equals(originalPhone)) {
            return Result.error("手机号不正确");
        }
        
        // 验证成功，生成验证令牌，设置5分钟过期时间
        String verifyToken = "verify:" + username + ":" + System.currentTimeMillis();
        redisTemplate.opsForValue().set("reset:token:" + username, verifyToken, 5, TimeUnit.MINUTES);
        
        return Result.success();
    }

    /**
     * 重置密码
     * @param data 包含用户名和新密码的请求体
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "验证通过后重置用户密码")
    public Result<Void> resetPassword(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String newPassword = data.get("newPassword");
        
        if (username == null || newPassword == null) {
            return Result.error("参数不完整");
        }
        
        // 验证令牌是否存在
        String tokenKey = "reset:token:" + username;
        String token = (String) redisTemplate.opsForValue().get(tokenKey);
        
        if (token == null) {
            return Result.error("验证超时，请重新开始");
        }
        
        // 重置密码
        Result<Void> result = userService.resetPassword(username, newPassword);
        
        if (result.getCode() == 200) {
            // 清除所有相关的临时数据
            redisTemplate.delete("reset:phone:" + username);
            redisTemplate.delete("reset:token:" + username);
            redisTemplate.delete("users:info:" + username);
            redisTemplate.delete("users:all");
        }
        
        return result;
    }

    /**
     * 更新用户余额
     * @param data 包含用户ID和新余额的请求体
     * @return 更新结果
     */
    @PostMapping("/update-balance")
    @Operation(summary = "更新用户余额", description = "管理员更新用户账户余额")
    public Result<Void> updateUserBalance(@RequestBody Map<String, Object> data) {
        Integer userId = (Integer) data.get("userId");
        Double balance = (Double) data.get("balance");
        
        if (userId == null || balance == null) {
            return Result.error("参数不完整");
        }
        
        // 查找用户
        User user = userService.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 更新余额
        user.setBalance(java.math.BigDecimal.valueOf(balance));
        Result<Void> result = userService.updateUser(user);
        
        if (result.getCode() == 200) {
            // 清除相关缓存
            redisTemplate.delete("users:all");
            redisTemplate.delete("users:id:" + userId);
            if (user.getUsername() != null) {
                redisTemplate.delete("users:info:" + user.getUsername());
            }
        }
        
        return result;
    }
}
