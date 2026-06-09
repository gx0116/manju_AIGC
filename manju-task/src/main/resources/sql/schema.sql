-- =====================================================
-- AI漫剧任务表 (manju-task 模块数据库 mj_task)
-- =====================================================
CREATE TABLE IF NOT EXISTS `ai_comic_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务主键ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '漫剧标题',
    `description` TEXT COMMENT '漫剧简介/剧情描述',
    `user_id` BIGINT DEFAULT NULL COMMENT '创建用户ID',
    `style` INT DEFAULT 1 COMMENT '画风：1-二次元 2-写实 3-国风 4-卡通',
    `content_type` INT DEFAULT 1 COMMENT '内容类型：1-图片漫剧 2-视频漫剧',
    `main_characters` VARCHAR(500) DEFAULT NULL COMMENT '主要角色（逗号分隔）',
    `episode_count` INT DEFAULT 1 COMMENT '总集数',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `is_public` INT DEFAULT 0 COMMENT '是否公开 0-私有 1-公开',
    `task_status` INT DEFAULT 0 COMMENT '任务状态：0-待处理 10-分镜生成中 15-分镜完成 20-增强生成中 25-增强完成 30-合成中 40-已完成 -1-失败',
    `progress` INT DEFAULT 0 COMMENT '生成进度 0-100',
    `storyboard_json` MEDIUMTEXT COMMENT '分镜JSON（序列化）',
    `comic_images_json` MEDIUMTEXT COMMENT '漫画图片URL列表JSON',
    `tts_audios_json` MEDIUMTEXT COMMENT 'TTS音频URL列表JSON',
    `video_url` VARCHAR(500) DEFAULT NULL COMMENT '最终合成的视频URL',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `work_id` BIGINT DEFAULT NULL COMMENT '关联的漫剧作品ID',
    `error_msg` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_task_status` (`task_status`),
    KEY `idx_work_id` (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI漫剧任务表';
