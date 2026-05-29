package com.mj.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.task.config.SessionProperties;
import com.mj.task.domain.po.ChatSession;
import com.mj.task.domain.vo.SessionVO;
import com.mj.task.mapper.ChatSessionMapper;
import com.mj.task.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private final SessionProperties sessionProperties;

    @Override
    public SessionVO createSession(Integer num) {
        SessionVO sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), num));

        // 随机生成sessionId
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());

        // 构建持久化对象，并持久化
        ChatSession chatSession = ChatSession.builder()
                .sessionId(sessionVO.getSessionId())
                //todo .userId(UserContext.getUser())
                .userId(11L)
                .creater(11L)
                .updater(11L)
                .build();
        super.save(chatSession);

        return sessionVO;
    }

}
