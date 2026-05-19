package com.mj.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.user.domain.dto.UserLoginDTO;
import com.mj.user.domain.po.User;
import com.mj.user.domain.vo.UserLoginVO;
import com.mj.user.mapper.UserMapper;
import com.mj.user.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements IUserService{

    @Override
    public List<User> queryAllUsers() {
        return this.list();
    }

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .eq(User::getPassword, password);
        User user = getOne(queryWrapper);
        if (user == null)
            throw new RuntimeException("用户名或密码错误");
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setUserId(user.getId());
        userLoginVO.setToken(generateToken(user));
        return userLoginVO;
    }
    private String generateToken(User user) {
        String tokenSource = user.getId() + String.valueOf(System.currentTimeMillis());
        return DigestUtils.md5DigestAsHex(tokenSource.getBytes(StandardCharsets.UTF_8));
    }

}
