package com.mj.work.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mj.work.domain.po.AiComicDrama;
import com.mj.work.domain.vo.ComicDramaVO;
import com.mj.work.mapper.ComicDramaMapper;
import com.mj.work.service.IComicDramaService;
import org.springframework.stereotype.Service;

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
}
