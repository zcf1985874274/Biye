package com.example.springboot.config;

import com.example.springboot.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    // 允许匿名访问的路径列表，与SecurityConfig保持一致
    private final List<String> anonymousPaths = List.of(
            "/api/user/login", 
            "/api/user/register",
            "/api/admins/login",
            "/api/user/check-username",
            "/api/user/verify-phone",
            "/api/user/reset-password",
            "/api/payments/alipay/*/status"
    );
    
    /**
     * 检查请求路径是否允许匿名访问
     * @param requestUri 请求路径
     * @return 是否允许匿名访问
     */
    private boolean isAnonymousPath(String requestUri) {
        for (String path : anonymousPaths) {
            // 处理通配符路径
            if (path.contains("*")) {
                String pattern = path.replace("*", ".*");
                if (requestUri.matches(pattern)) {
                    return true;
                }
            } else {
                if (requestUri.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService userDetailsService;

    @Autowired
    @Qualifier("customAdminDetailsService")
    private UserDetailsService adminDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 获取请求路径
        String requestUri = request.getRequestURI();
        
        // 检查是否是允许匿名访问的路径，如果是，直接跳过JWT验证
        if (isAnonymousPath(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            
            logger.info("Received JWT token: " + jwt);
            
            if (!jwt.matches("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*$")) {
                logger.error("JWT token格式无效: " + jwt);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token格式无效");
                return;
            }
            
            try {
                username = jwtUtil.extractUsername(jwt);
                if (!jwtUtil.validateToken(jwt, username)) {
                    throw new Exception("Token验证失败");
                }
            } catch (Exception e) {
                logger.error("JWT token验证失败: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token验证失败: " + e.getMessage());
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 根据请求路径选择使用哪个DetailsService
           requestUri = request.getRequestURI();
            logger.info("处理JWT认证，用户: " + username + ", 请求路径: " + requestUri);
            UserDetailsService detailsService = null;
            
            // 优先匹配普通用户路径（更具体的路径优先）
            // 普通用户支付宝支付相关路径 - 移到前面优先处理
            if (requestUri.startsWith("/api/alipay/pay/") || requestUri.startsWith("/api/alipay/status/") || requestUri.equals("/api/payments/notify")) {
                detailsService = userDetailsService;
                logger.info("使用userDetailsService处理用户支付宝支付相关请求");
            }
            // 普通用户修改自己的信息
            else if (requestUri.equals("/api/user/self")) {
                detailsService = userDetailsService;
                logger.info("使用userDetailsService处理用户自己信息修改");
            }
            // 普通用户查看自己的订单记录
            else if (requestUri.startsWith("/api/usage-records/self")) {
                detailsService = userDetailsService;
                logger.info("使用userDetailsService处理用户自己订单记录");
            }
            // 普通用户根据用户ID查询记录（需要权限验证）
            else if (requestUri.startsWith("/api/usage-records/user/")) {
                detailsService = userDetailsService;
                logger.info("使用userDetailsService处理用户ID查询记录");
            }
            // 普通用户创建使用记录
            else if (requestUri.equals("/api/usage-records") && "POST".equals(request.getMethod())) {
                detailsService = userDetailsService;
                logger.info("使用userDetailsService处理用户创建使用记录");
            }
            // 普通用户和管理员都可以访问的路径
            else if (requestUri.equals("/api/stores") || 
                     requestUri.equals("/api/rooms") ||
                     requestUri.startsWith("/api/rooms/available") ||
                     requestUri.equals("/api/usage-records")) {
                // 首先尝试使用userDetailsService（普通用户）
                detailsService = userDetailsService;
                logger.info("使用userDetailsService处理普通用户请求");
            }
            // 管理员相关路径使用adminDetailsService
            else if (requestUri.startsWith("/api/admins")) {
                detailsService = adminDetailsService;
                logger.info("使用adminDetailsService处理管理员相关路径");
            } 
            // 管理员操作的用户管理和房间管理路径使用adminDetailsService
            else if (requestUri.equals("/api/user/all") || 
                     requestUri.equals("/api/user") ||
                     (requestUri.startsWith("/api/rooms/") && 
                      !requestUri.startsWith("/api/rooms/available") &&
                      !requestUri.endsWith("/status") &&
                      !requestUri.matches("/api/rooms/\\d+/status"))) {
                detailsService = adminDetailsService;
                logger.info("使用adminDetailsService处理管理员操作路径");
            }
            // 管理员查看所有订单记录（排除用户自己的订单和共享路径）
            else if (requestUri.startsWith("/api/usage-records") && 
                     !requestUri.startsWith("/api/usage-records/self") &&
                     !requestUri.startsWith("/api/usage-records/user/") &&
                     !requestUri.equals("/api/usage-records")) {
                detailsService = adminDetailsService;
                logger.info("使用adminDetailsService处理管理员查看所有订单");
            }
            // 共享路径：房间状态更新，支持普通用户和管理员
            else if (requestUri.matches("/api/rooms/\\d+/status")) {
                // 默认使用userDetailsService，如果失败则尝试adminDetailsService
                detailsService = userDetailsService;
                logger.info("使用userDetailsService处理房间状态更新（共享路径）");
            } else {
                // 默认情况下使用userDetailsService（普通用户路径）
                detailsService = userDetailsService;
                logger.info("使用默认的userDetailsService处理请求");
            }

            UserDetails userDetails;
            try {
                userDetails = detailsService.loadUserByUsername(username);
                logger.info("成功加载用户详情: " + username + ", 使用服务: " + detailsService.getClass().getSimpleName());
            } catch (UsernameNotFoundException e) {
                // 如果是共享路径且用户不存在，尝试使用另一个DetailsService
                if (requestUri.equals("/api/stores") || 
                    requestUri.equals("/api/rooms") ||
                    requestUri.startsWith("/api/rooms/available") ||
                    requestUri.equals("/api/usage-records") ||
                    requestUri.matches("/api/rooms/\\d+/status")) {
                    logger.info("用户不存在，尝试使用另一个DetailsService: " + username);
                    try {
                        // 如果之前用的是userDetailsService，现在尝试adminDetailsService
                        if (detailsService == userDetailsService) {
                            detailsService = adminDetailsService;
                            logger.info("切换到adminDetailsService尝试加载: " + username);
                        } else {
                            detailsService = userDetailsService;
                            logger.info("切换到userDetailsService尝试加载: " + username);
                        }
                        userDetails = detailsService.loadUserByUsername(username);
                        logger.info("使用另一个DetailsService成功加载用户: " + username);
                    } catch (UsernameNotFoundException ex) {
                        logger.error("用户和管理员都不存在: " + username);
                        throw new UsernameNotFoundException("用户不存在: " + username);
                    }
                } else {
                    throw e;
                }
            }

            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
