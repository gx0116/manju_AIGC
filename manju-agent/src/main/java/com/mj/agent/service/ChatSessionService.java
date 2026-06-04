package com.mj.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.agent.domain.po.ChatSession;
import com.mj.agent.domain.vo.ChatSessionVO;
import com.mj.agent.domain.vo.SessionVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Integer num);

    /**
     * 更新会话更新时间
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    void update(String sessionId, String title, Long userId);

    /**
     * 查询历史会话列表
     */
    Map<String, List<ChatSessionVO>> queryHistorySession();

    /**
     * 删除历史会话
     * @param sessionId 会话ID
     */
    void deleteHistorySession(String sessionId);
}
