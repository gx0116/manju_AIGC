package com.mj.agent.tools.result;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mj.api.domain.po.AiComicDrama;// 你的漫剧实体类
import com.mj.api.domain.vo.ComicDramaVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicDramaInfo {

    @JsonPropertyDescription("漫剧ID")
    private Long id;

    @JsonPropertyDescription("漫剧标题")
    private String title;

    @JsonPropertyDescription("漫剧封面图片地址")
    private String coverUrl;

    @JsonPropertyDescription("漫剧简介/剧情描述")
    private String description;

    @JsonPropertyDescription("作者/创作者名称")
    private String author;

    @JsonPropertyDescription("AI生成使用的提示词")
    private String aiPrompt;

    @JsonPropertyDescription("使用的AI模型名称")
    private String aiModel;

    @JsonPropertyDescription("漫剧生成状态：0-待生成 1-生成中 2-生成成功 3-生成失败")
    private Integer generateStatus;

    @JsonPropertyDescription("漫剧内容类型：1-图片漫剧 2-视频漫剧 3-图文混合")
    private Integer contentType;

    @JsonPropertyDescription("漫剧总集数/章节数")
    private Integer episodeCount;

    @JsonPropertyDescription("主要角色名称，多个用逗号分隔")
    private String mainCharacters;

    @JsonPropertyDescription("漫剧风格，例如：二次元、国风、写实、卡通")
    private String style;

    @JsonPropertyDescription("是否公开：0-私有 1-公开")
    private Integer isPublic;

    /**
     * 将数据库漫剧实体对象转换为AI工具返回对象
     *
     * @param comicDramaVO 数据库漫剧实体
     * @return 转换后的漫剧信息（AI可理解格式）
     */
    public static ComicDramaInfo of(ComicDramaVO comicDramaVO) {
        if (null == comicDramaVO) {
            return null;
        }
        // 属性自动拷贝
        return BeanUtil.toBean(comicDramaVO, ComicDramaInfo.class);
    }
}