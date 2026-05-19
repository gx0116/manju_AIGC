package com.mj.user.controller;

import com.mj.user.domain.dto.UserLoginDTO;
import com.mj.user.domain.po.User;
import com.mj.user.domain.vo.UserLoginVO;
import com.mj.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @PostMapping("/login")
    public UserLoginVO login(@RequestBody UserLoginDTO userLoginDTO){
        return userService.login(userLoginDTO);
    }

    @GetMapping("/all")
    public List<User> getAllUsers(){
        return userService.queryAllUsers();
    }
}
