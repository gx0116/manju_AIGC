package com.mj.work.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI漫剧信息表 实体类
 * 表名：ai_comic_drama
 */
@Data
@TableName("ai_comic_drama")
public class AiComicDrama {

    /**
     * 漫剧主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 漫剧标题
     */
    private String title;

    /**
     * 漫剧封面图片URL
     */
    private String coverUrl;

    /**
     * 漫剧简介/剧情描述
     */
    private String description;

    /**
     * 作者/创作者
     */
    private String author;

    /**
     * 创建用户ID（AI生成归属用户）
     */
    private Long userId;

    /**
     * AI生成使用的提示词
     */
    private String aiPrompt;

    /**
     * 使用的AI模型（如：豆包/StableDiffusion等）
     */
    private String aiModel;

    /**
     * 生成状态 0-待生成 1-生成中 2-生成成功 3-生成失败
     */
    private Integer generateStatus;

    /**
     * 生成进度 0-100
     */
    private Integer generateProgress;

    /**
     * 内容类型 1-图片漫剧 2-视频漫剧 3-图文混合
     */
    private Integer contentType;

    /**
     * 总集数/章节数
     */
    private Integer episodeCount;

    /**
     * 主要角色（逗号分隔）
     */
    private String mainCharacters;

    /**
     * 漫画风格（二次元/写实/国风/卡通等）
     */
    private String style;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 是否公开 0-私有 1-公开
     */
    private Integer isPublic;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}