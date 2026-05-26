package com.mj.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.user.domain.dto.UserLoginDTO;
import com.mj.user.domain.dto.UserRegisterDTO;
import com.mj.user.domain.po.User;
import com.mj.user.domain.vo.UserLoginVO;

import java.util.List;

public interface IUserService extends IService<User> {

    UserLoginVO login(UserLoginDTO userLoginDTO);

    void register(UserRegisterDTO userRegisterDTO);

    List<User> queryAllUsers();

}
