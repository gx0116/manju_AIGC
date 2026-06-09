package com.mj.api.client;

import com.mj.api.domain.vo.ComicDramaVO;
import com.mj.common.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "work-service")
public interface WorkClient {

    @GetMapping("/comicDrama")
    ComicDramaVO queryComicDramaById(@RequestParam Long id);

    /**
     * 创建漫剧作品（入库）
     */
    @PostMapping("/comicDrama")
    Result<Long> saveComicDrama(@RequestBody Map<String, Object> params);

    /**
     * 更新漫剧作品状态
     */
    @PutMapping("/comicDrama/{id}/status")
    Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> params);

    /**
     * 更新漫剧作品（封面、视频URL等）
     */
    @PutMapping("/comicDrama/{id}")
    Result<Void> updateComicDrama(@PathVariable("id") Long id, @RequestBody Map<String, Object> params);
}
