package com.mj.api.client;

import com.mj.common.domain.MediaCompositeDTO;
import com.mj.common.domain.MediaCompositeResultDTO;
import com.mj.common.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 媒体服务 Feign客户端
 */
@FeignClient(name = "media-service")
public interface MediaClient {

    /**
     * FFmpeg合成漫剧视频
     */
    @PostMapping("/media/composite")
    Result<MediaCompositeResultDTO> compositeVideo(@RequestBody MediaCompositeDTO request);

    /**
     * 获取合成进度
     */
    @PostMapping("/media/composite/progress")
    Result<Integer> getCompositeProgress(@RequestBody Long taskId);
}