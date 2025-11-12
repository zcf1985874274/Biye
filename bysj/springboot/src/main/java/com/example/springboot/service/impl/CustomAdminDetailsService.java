package com.example.springboot.service.impl;

import com.example.springboot.model.Admin;
import com.example.springboot.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("customAdminDetailsService")
public class CustomAdminDetailsService implements UserDetailsService {

    @Autowired
    private AdminService adminService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminService.findByUsername(username);
        if (admin == null) {
            throw new UsernameNotFoundException("管理员不存在: " + username);
        }

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .roles(admin.getRole() != null ? admin.getRole().toUpperCase() : "ADMIN")
                .build();
    }
}
