package com.mj.user.controller;

import com.mj.common.domain.Result;
import com.mj.user.domain.dto.UserLoginDTO;
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
     * 查询所有用户
     */
    @GetMapping("/profile")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.queryAllUsers();
        return Result.success(users);
    }
}
