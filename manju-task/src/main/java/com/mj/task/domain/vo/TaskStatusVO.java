package com.mj.task.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务状态VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusVO {

    private Long taskId;
    private Integer taskStatus;
    private String taskStatusDesc;
    private Integer progress;
    private String videoUrl;
    private String coverUrl;
    private Long workId;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}