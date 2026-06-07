package com.mj.api.client;

import com.mj.api.domain.vo.ComicDramaVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "work-service")
public interface WorkClient {

    @GetMapping("/comicDrama")
    ComicDramaVO queryComicDramaById(@RequestParam Long id);

}
