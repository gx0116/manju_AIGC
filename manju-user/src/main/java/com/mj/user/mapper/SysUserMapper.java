package com.mj.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mj.user.domain.po.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<User> {
}
