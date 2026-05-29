package com.mj.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mj.task.domain.po.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

}