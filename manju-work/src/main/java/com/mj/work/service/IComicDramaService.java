package com.mj.work.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mj.work.domain.po.AiComicDrama;
import com.mj.work.domain.vo.ComicDramaVO;

public interface IComicDramaService extends IService<AiComicDrama> {

    ComicDramaVO queryComicDramaById(Long id);

}
