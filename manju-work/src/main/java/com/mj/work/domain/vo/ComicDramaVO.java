package com.mj.work.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI漫剧 VO（前端展示对象）
 * 用于接口返回给前端
 */
@Data
public class ComicDramaVO {

    private Long id;

    private String title;

    private String coverUrl;

    private String description;

    private String author;

    private String aiModel;

    private String style;

    private String mainCharacters;

    private Integer contentType;

    private Integer episodeCount;

    private Integer generateStatus;

    private Integer isPublic;

    private LocalDateTime createTime;
}