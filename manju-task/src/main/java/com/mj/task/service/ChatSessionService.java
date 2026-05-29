package com.mj.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.task.domain.po.ChatSession;
import com.mj.task.domain.vo.SessionVO;

public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Integer num);

}
