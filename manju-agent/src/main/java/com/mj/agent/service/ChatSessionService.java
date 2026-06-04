package com.mj.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.agent.domain.po.ChatSession;
import com.mj.agent.domain.vo.SessionVO;

public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Integer num);

}
