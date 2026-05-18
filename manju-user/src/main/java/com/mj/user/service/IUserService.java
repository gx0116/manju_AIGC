package com.mj.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.user.domain.po.User;

import java.util.List;

public interface IUserService extends IService<User> {
    List<User> queryAllUsers();
}
