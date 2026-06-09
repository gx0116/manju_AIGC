package com.mj.task.controller;

import com.mj.common.context.UserContext;
import com.mj.common.domain.Result;
import com.mj.common.domain.TaskSubmitDTO;
import com.mj.common.enums.TaskStatusEnum;
import com.mj.task.domain.po.AiComicTask;
import com.mj.task.domain.vo.TaskStatusVO;
import com.mj.task.service.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 漫剧任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;

    /**
     * 提交漫剧生成任务
     * 用户提交文本参数 → 入库 → 发MQ
     */
    @PostMapping("/submit")
    public Result<Long> submitTask(@RequestBody TaskSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("[TaskController] 收到任务提交, userId={}, title={}", userId, dto.getTitle());
        Long taskId = taskService.submitTask(dto, userId);
        return Result.success(taskId);
    }

    /**
     * 查询任务状态和进度
     */
    @GetMapping("/{taskId}/status")
    public Result<TaskStatusVO> getTaskStatus(@PathVariable Long taskId) {
        AiComicTask task = taskService.getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        TaskStatusEnum statusEnum = null;
        for (TaskStatusEnum e : TaskStatusEnum.values()) {
            if (e.getValue() == task.getTaskStatus()) {
                statusEnum = e;
                break;
            }
        }

        TaskStatusVO vo = TaskStatusVO.builder()
                .taskId(task.getId())
                .taskStatus(task.getTaskStatus())
                .taskStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知")
                .progress(task.getProgress())
                .videoUrl(task.getVideoUrl())
                .coverUrl(task.getCoverUrl())
                .workId(task.getWorkId())
                .errorMsg(task.getErrorMsg())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();

        return Result.success(vo);
    }
}