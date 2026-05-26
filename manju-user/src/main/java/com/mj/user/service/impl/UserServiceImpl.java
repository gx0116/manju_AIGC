package com.mj.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.user.domain.dto.UserLoginDTO;
import com.mj.user.domain.po.User;
import com.mj.user.domain.vo.UserLoginVO;
import com.mj.user.mapper.UserMapper;
import com.mj.user.service.IUserService;
import com.mj.user.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public List<User> queryAllUsers() {
        return this.list();
    }

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        // 1. 根据用户名查询用户
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) {
            log.warn("登录失败，用户名不存在: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 检查用户状态（1=正常, 0=禁用）
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("登录失败，用户已被禁用: {}", username);
            throw new RuntimeException("该账号已被禁用，请联系管理员");
        }

        // 3. 校验密码（BCrypt）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败，密码错误: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        // 5. 构建返回结果
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setUserId(user.getId());
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setToken(token);

        log.info("用户登录成功: {}", username);
        return userLoginVO;
    }
}
