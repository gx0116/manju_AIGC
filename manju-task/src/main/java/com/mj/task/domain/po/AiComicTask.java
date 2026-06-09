package com.mj.task.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI漫剧任务表 实体类
 * 表名：ai_comic_task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_comic_task")
public class AiComicTask {

    /** 任务主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 漫剧标题 */
    private String title;

    /** 漫剧简介/剧情描述 */
    private String description;

    /** 创建用户ID */
    private Long userId;

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

    /** 任务状态：参照TaskStatusEnum */
    private Integer taskStatus;

    /** 生成进度 0-100 */
    private Integer progress;

    /** 分镜JSON（序列化的StoryboardDTO） */
    private String storyboardJson;

    /** 漫画图片URL列表JSON */
    private String comicImagesJson;

    /** TTS音频URL列表JSON */
    private String ttsAudiosJson;

    /** 最终合成的视频URL */
    private String videoUrl;

    /** 封面图片URL */
    private String coverUrl;

    /** 关联的漫剧作品ID（manju-work中的ai_comic_drama.id） */
    private Long workId;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}