package com.example.springboot.config;

import com.example.springboot.util.JwtUtil;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
            String requestUri = request.getRequestURI();
            UserDetailsService detailsService = null;
            
            // 管理员相关路径使用adminDetailsService
            if (requestUri.startsWith("/api/admins")) {
                detailsService = adminDetailsService;
            } 
            // 管理员操作的用户管理和房间管理路径使用adminDetailsService
            else if (requestUri.equals("/api/user/all") || 
                     requestUri.equals("/api/user") ||
                     requestUri.startsWith("/api/rooms/") ||
                     requestUri.equals("/api/rooms")) {
                detailsService = adminDetailsService;
            }
            // 管理员查看所有订单记录（排除用户自己的订单）
            else if (requestUri.startsWith("/api/usage-records") && 
                     !requestUri.startsWith("/api/usage-records/self") &&
                     !requestUri.startsWith("/api/usage-records/user/")) {
                detailsService = adminDetailsService;
            } 
            // 普通用户修改自己的信息
            else if (requestUri.equals("/api/user/self")) {
                detailsService = userDetailsService;
            }
            // 普通用户查看自己的订单记录
            else if (requestUri.startsWith("/api/usage-records/self")) {
                detailsService = userDetailsService;
            }
            // 普通用户根据用户ID查询记录（需要权限验证）
            else if (requestUri.startsWith("/api/usage-records/user/")) {
                detailsService = userDetailsService;
            }
            // 普通用户和管理员都可以访问 /api/stores
            else if (requestUri.equals("/api/stores")) {
                // 首先尝试使用userDetailsService（普通用户）
                try {
                    detailsService = userDetailsService;
                    // 尝试加载用户，如果用户存在则使用userDetailsService
                    userDetailsService.loadUserByUsername(username);
                } catch (UsernameNotFoundException e) {
                    // 如果普通用户不存在，再尝试使用adminDetailsService
                    try {
                        detailsService = adminDetailsService;
                        adminDetailsService.loadUserByUsername(username);
                    } catch (UsernameNotFoundException ex) {
                        // 如果都不存在，则抛出异常
                        throw new UsernameNotFoundException("用户不存在: " + username);
                    }
                }
            } else {
                // 默认情况下使用userDetailsService（普通用户路径）
                detailsService = userDetailsService;
            }

            UserDetails userDetails = detailsService.loadUserByUsername(username);

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
