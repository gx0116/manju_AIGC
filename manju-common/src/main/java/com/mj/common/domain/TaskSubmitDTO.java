package com.mj.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 漫剧任务提交DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmitDTO {

    /** 漫剧标题 */
    private String title;

    /** 漫剧简介/剧情描述 */
    private String description;

    /** 画风：1-二次元 2-写实 3-国风 4-卡通 */
    private Integer style;

    /** 内容类型：1-图片漫剧 2-视频漫剧 */
    private Integer contentType;

    /** 主要角色（逗号分隔） */
    private String mainCharacters;

    /** 总集数 */
    private Integer episodeCount;

    /** 分类ID */
    private Long categoryId;

    /** 是否公开 0-私有 1-公开 */
    private Integer isPublic;
}