package com.example.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS配置类
 * 注意：此配置与SecurityConfig中的CORS配置保持一致
 * 主要用于非Spring Security过滤链中的请求
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许cookies跨域
        config.setAllowCredentials(true);

        // 允许的源，这里明确指定前端应用的地址
        config.setAllowedOrigins(Arrays.asList("http://localhost:8081"));

        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的头信息
        config.setAllowedHeaders(Arrays.asList("*"));

        // 暴露的头信息
        config.setExposedHeaders(Arrays.asList("Authorization"));

        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
