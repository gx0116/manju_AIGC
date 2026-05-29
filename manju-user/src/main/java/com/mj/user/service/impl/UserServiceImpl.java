package com.mj.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.common.context.UserContext;
import com.mj.user.domain.dto.UserLoginDTO;
import com.mj.user.domain.dto.UserRegisterDTO;
import com.mj.user.domain.po.User;
import com.mj.user.domain.vo.UserLoginVO;
import com.mj.user.mapper.UserMapper;
import com.mj.user.service.IUserService;
import com.mj.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserContext userContext;

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

        long remainingTTL = jwtUtils.getRemainingTTL(token);
        Long userId = jwtUtils.getUserId(token);
        log.info("用户登出成功，userId: {}, token 将在 {}ms 后失效", userId, remainingTTL);
        System.out.println("token");
        System.out.println();
        System.out.println(token);

        return userLoginVO;
    }

    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();
        String phone = userRegisterDTO.getPhone();
        String email = userRegisterDTO.getEmail();

        // 1. 检查用户名是否已存在
        Long usernameCount = lambdaQuery().eq(User::getUsername, username).count();
        if (usernameCount > 0) {
            log.warn("注册失败，用户名已存在: {}", username);
            throw new RuntimeException("用户名已被注册");
        }

        // 2. 检查手机号是否已被占用
        if (phone != null && !phone.isBlank()) {
            Long phoneCount = lambdaQuery().eq(User::getPhone, phone).count();
            if (phoneCount > 0) {
                log.warn("注册失败，手机号已被占用: {}", phone);
                throw new RuntimeException("该手机号已被注册");
            }
        }

        // 3. 检查邮箱是否已被占用
        if (email != null && !email.isBlank()) {
            Long emailCount = lambdaQuery().eq(User::getEmail, email).count();
            if (emailCount > 0) {
                log.warn("注册失败，邮箱已被占用: {}", email);
                throw new RuntimeException("该邮箱已被注册");
            }
        }

        // 4. BCrypt 加密密码
        String encodedPassword = passwordEncoder.encode(password);

        // 5. 构建用户对象并保存
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(1); // 默认正常状态
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        save(user);
        log.info("用户注册成功: {}", username);
    }

    @Override
    public User profile(String token) {
        User user = lambdaQuery().eq(User::getId, jwtUtils.getUserId(token)).one();
        return user;
    }

    @Override
    public void logout() {
        Long userId = userContext.getUserId();
        log.info("用户登出成功，userId: {}", userId);
    }

}
