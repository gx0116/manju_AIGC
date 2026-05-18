package com.mj.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.user.domain.po.User;
import com.mj.user.mapper.SysUserMapper;
import com.mj.user.service.IUserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper,User> implements IUserService{

    @Override
    public List<User> queryAllUsers() {
        return this.list();
    }
}
