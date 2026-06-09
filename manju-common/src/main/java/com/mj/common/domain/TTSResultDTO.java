package com.mj.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS语音合成结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTSResultDTO {

    /** 任务ID */
    private Long taskId;

    /** 镜头序号 */
    private Integer sceneIndex;

    /** 合成的音频文件URL */
    private String audioUrl;

    /** 音频时长（秒） */
    private Double duration;

    /** 生成耗时（毫秒） */
    private Long costTime;
}