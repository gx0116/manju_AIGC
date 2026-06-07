package com.mj.work.controller;

import com.mj.common.domain.Result;
import com.mj.work.domain.vo.ComicDramaVO;
import com.mj.work.service.IComicDramaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comicDrama")
@RequiredArgsConstructor
public class ComicDramaController {

    private final IComicDramaService comicDramaService;

    @GetMapping
    public ComicDramaVO queryComicDramaById(@RequestParam Long id) {
        ComicDramaVO comicDramaVO = comicDramaService.queryComicDramaById(id);
        return comicDramaVO;
    }

}
