package com.mj.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.common.domain.TaskSubmitDTO;
import com.mj.task.domain.po.AiComicTask;

/**
 * 漫剧任务服务
 */
public interface ITaskService extends IService<AiComicTask> {

    /**
     * 提交漫剧生成任务（入库+发MQ）
     *
     * @param dto 任务提交参数
     * @param userId 用户ID
     * @return 任务ID
     */
    Long submitTask(TaskSubmitDTO dto, Long userId);

    /**
     * 更新任务状态
     */
    void updateTaskStatus(Long taskId, Integer status, String errorMsg);

    /**
     * 更新任务进度
     */
    void updateProgress(Long taskId, Integer progress);
}