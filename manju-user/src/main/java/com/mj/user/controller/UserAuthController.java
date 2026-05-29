package com.mj.user.controller;

import com.mj.common.domain.Result;
import com.mj.user.domain.dto.UserLoginDTO;
import com.mj.user.domain.dto.UserRegisterDTO;
import com.mj.user.domain.po.User;
import com.mj.user.domain.vo.UserLoginVO;
import com.mj.user.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final IUserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        UserLoginVO loginVO = userService.login(userLoginDTO);
        return Result.success(loginVO);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        userService.register(userRegisterDTO);
        return Result.success();
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        userService.logout();
        return Result.success();
    }

    /**
     * 查询所有用户
     */
    @GetMapping("/profile")
    public Result<User> profile(@RequestHeader("Authorization") String authHeader) {
        // 移除 "Bearer " 前缀获取 token
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = userService.profile(token);
        return Result.success(user);
    }
}
