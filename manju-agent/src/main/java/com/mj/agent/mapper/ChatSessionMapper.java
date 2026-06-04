package com.mj.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mj.agent.domain.po.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

}