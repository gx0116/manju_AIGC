package com.mj.work.controller;

import com.mj.common.domain.Result;
import com.mj.work.domain.vo.ComicDramaVO;
import com.mj.work.service.IComicDramaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
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

    /**
     * 创建漫剧作品（入库）
     */
    @PostMapping
    public Result<Long> saveComicDrama(@RequestBody Map<String, Object> params) {
        log.info("[WorkController] 收到作品入库请求, title={}", params.get("title"));
        Long workId = comicDramaService.saveComicDrama(params);
        return Result.success(workId);
    }

    /**
     * 更新漫剧作品
     */
    @PutMapping("/{id}")
    public Result<Void> updateComicDrama(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("[WorkController] 收到作品更新请求, workId={}", id);
        comicDramaService.updateComicDrama(id, params);
        return Result.success();
    }

    /**
     * 更新漫剧作品状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("[WorkController] 收到状态更新请求, workId={}, status={}", id, params.get("generateStatus"));
        comicDramaService.updateStatus(id, params);
        return Result.success();
    }
}
