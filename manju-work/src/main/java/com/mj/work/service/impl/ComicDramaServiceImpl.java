package com.mj.work.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.work.domain.po.AiComicDrama;
import com.mj.work.domain.vo.ComicDramaVO;
import com.mj.work.mapper.ComicDramaMapper;
import com.mj.work.service.IComicDramaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class ComicDramaServiceImpl extends ServiceImpl<ComicDramaMapper, AiComicDrama> implements IComicDramaService {

    @Override
    public ComicDramaVO queryComicDramaById(Long id) {
        AiComicDrama aiComicDrama = this.getById(id);
        if (aiComicDrama == null) {
            return null;
        }
        ComicDramaVO comicDramaVO = new ComicDramaVO();
        BeanUtil.copyProperties(aiComicDrama, comicDramaVO);
        return comicDramaVO;
    }

    @Override
    public Long saveComicDrama(Map<String, Object> params) {
        AiComicDrama drama = new AiComicDrama();
        drama.setTitle(Convert.toStr(params.get("title")));
        drama.setDescription(Convert.toStr(params.get("description")));
        drama.setAuthor(Convert.toStr(params.get("author")));
        drama.setUserId(Convert.toLong(params.get("userId")));
        drama.setAiModel(Convert.toStr(params.get("aiModel")));
        drama.setStyle(Convert.toStr(params.get("style")));
        drama.setMainCharacters(Convert.toStr(params.get("mainCharacters")));
        drama.setContentType(Convert.toInt(params.get("contentType")));
        drama.setCoverUrl(Convert.toStr(params.get("coverUrl")));
        drama.setEpisodeCount(Convert.toInt(params.get("episodeCount")));
        drama.setGenerateStatus(Convert.toInt(params.get("generateStatus")));
        drama.setGenerateProgress(100);
        drama.setIsPublic(Convert.toInt(params.get("isPublic")));
        drama.setStatus(Convert.toInt(params.get("status")));
        drama.setCreateTime(LocalDateTime.now());
        drama.setUpdateTime(LocalDateTime.now());

        this.save(drama);
        log.info("[WorkService] 漫剧作品入库成功, workId={}, title={}", drama.getId(), drama.getTitle());
        return drama.getId();
    }

    @Override
    public void updateComicDrama(Long id, Map<String, Object> params) {
        AiComicDrama drama = new AiComicDrama();
        drama.setId(id);

        if (params.containsKey("coverUrl")) {
            drama.setCoverUrl(Convert.toStr(params.get("coverUrl")));
        }
        if (params.containsKey("videoUrl")) {
            // videoUrl 存储方式：可以在AiComicDrama中添加该字段，或存到aiPrompt
            drama.setAiPrompt(Convert.toStr(params.get("videoUrl")));
        }
        if (params.containsKey("generateStatus")) {
            drama.setGenerateStatus(Convert.toInt(params.get("generateStatus")));
        }
        if (params.containsKey("generateProgress")) {
            drama.setGenerateProgress(Convert.toInt(params.get("generateProgress")));
        }
        if (params.containsKey("title")) {
            drama.setTitle(Convert.toStr(params.get("title")));
        }
        if (params.containsKey("description")) {
            drama.setDescription(Convert.toStr(params.get("description")));
        }
        drama.setUpdateTime(LocalDateTime.now());

        this.updateById(drama);
        log.info("[WorkService] 漫剧作品更新成功, workId={}", id);
    }

    @Override
    public void updateStatus(Long id, Map<String, Object> params) {
        AiComicDrama drama = new AiComicDrama();
        drama.setId(id);
        drama.setGenerateStatus(Convert.toInt(params.get("generateStatus")));

        if (params.containsKey("generateProgress")) {
            drama.setGenerateProgress(Convert.toInt(params.get("generateProgress")));
        }
        drama.setUpdateTime(LocalDateTime.now());

        this.updateById(drama);
        log.info("[WorkService] 漫剧状态更新, workId={}, status={}", id, params.get("generateStatus"));
    }
}
