package com.mj.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.common.domain.TaskSubmitDTO;
import com.mj.common.enums.TaskStatusEnum;
import com.mj.task.config.RabbitMQConfig;
import com.mj.task.domain.po.AiComicTask;
import com.mj.task.mapper.AiComicTaskMapper;
import com.mj.task.service.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 漫剧任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<AiComicTaskMapper, AiComicTask> implements ITaskService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Long submitTask(TaskSubmitDTO dto, Long userId) {
        // 1. 构建任务实体入库
        AiComicTask task = AiComicTask.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .userId(userId)
                .style(dto.getStyle())
                .contentType(dto.getContentType())
                .mainCharacters(dto.getMainCharacters())
                .episodeCount(dto.getEpisodeCount())
                .categoryId(dto.getCategoryId())
                .isPublic(dto.getIsPublic())
                .taskStatus(TaskStatusEnum.PENDING.getValue())
                .progress(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        this.save(task);
        log.info("[TaskService] 任务入库成功, taskId={}, title={}", task.getId(), task.getTitle());

        // 2. 发送MQ消息给Director协调器
        Map<String, Object> mqMessage = new HashMap<>();
        mqMessage.put("taskId", task.getId());
        mqMessage.put("title", task.getTitle());
        mqMessage.put("description", task.getDescription());
        mqMessage.put("style", task.getStyle());
        mqMessage.put("contentType", task.getContentType());
        mqMessage.put("mainCharacters", task.getMainCharacters());
        mqMessage.put("userId", userId != null ? userId : 0L);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_EXCHANGE,
                RabbitMQConfig.DIRECTOR_ROUTING_KEY,
                mqMessage
        );
        log.info("[TaskService] MQ消息已发送, taskId={}", task.getId());

        return task.getId();
    }

    @Override
    public void updateTaskStatus(Long taskId, Integer status, String errorMsg) {
        AiComicTask task = new AiComicTask();
        task.setId(taskId);
        task.setTaskStatus(status);
        if (errorMsg != null) {
            task.setErrorMsg(errorMsg);
        }
        task.setUpdateTime(LocalDateTime.now());
        this.updateById(task);
        log.info("[TaskService] 任务状态更新, taskId={}, status={}", taskId, status);
    }

    @Override
    public void updateProgress(Long taskId, Integer progress) {
        AiComicTask task = new AiComicTask();
        task.setId(taskId);
        task.setProgress(progress);
        task.setUpdateTime(LocalDateTime.now());
        this.updateById(task);
    }
}