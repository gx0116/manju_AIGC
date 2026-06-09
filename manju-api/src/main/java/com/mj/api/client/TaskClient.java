package com.mj.api.client;

import com.mj.common.domain.Result;
import com.mj.common.domain.TaskSubmitDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 任务服务 Feign客户端
 */
@FeignClient(name = "task-service")
public interface TaskClient {

    /**
     * 提交漫剧生成任务
     */
    @PostMapping("/task/submit")
    Result<Long> submitTask(@RequestBody TaskSubmitDTO taskSubmitDTO);

    /**
     * 查询任务状态
     */
    @GetMapping("/task/{taskId}/status")
    Result<Integer> getTaskStatus(@PathVariable("taskId") Long taskId);
}