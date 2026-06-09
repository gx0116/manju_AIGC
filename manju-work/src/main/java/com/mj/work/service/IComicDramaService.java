package com.mj.work.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.work.domain.po.AiComicDrama;
import com.mj.work.domain.vo.ComicDramaVO;

import java.util.Map;

public interface IComicDramaService extends IService<AiComicDrama> {

    ComicDramaVO queryComicDramaById(Long id);

    /**
     * 创建漫剧作品（入库）
     * @return 作品ID
     */
    Long saveComicDrama(Map<String, Object> params);

    /**
     * 更新漫剧作品
     */
    void updateComicDrama(Long id, Map<String, Object> params);

    /**
     * 更新漫剧作品状态
     */
    void updateStatus(Long id, Map<String, Object> params);
}
