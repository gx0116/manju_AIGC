package com.mj.agent.tools;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.mj.agent.config.ToolResultHolder;
import com.mj.agent.constants.Constant;
import com.mj.agent.tools.result.ComicDramaInfo;
import com.mj.api.client.WorkClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ComicDramaTools {

    private final WorkClient workClient;
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量

    @Tool(description = Constant.Tools.QUERY_MANJU_BY_ID)
    public ComicDramaInfo queryComicById(
            @ToolParam(description = Constant.ToolParams.MANJU_ID) Long comicDramaId, ToolContext toolContext
    ) {
        return Optional.ofNullable(comicDramaId)
                .map(id -> ComicDramaInfo.of(workClient.queryComicDramaById(id)))
                .map(comicDramaInfo -> {
                    // 存储数据的字段名
                    String field = StrUtil.format(FIELD_NAME_FORMAT,
                            StrUtil.lowerFirst(ComicDramaInfo.class.getSimpleName()),
                            comicDramaInfo.getId());
                    // 存储的key
                    String requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
                    ToolResultHolder.put(requestId, field, comicDramaInfo);
                    return comicDramaInfo;
                })
                .orElse(null);
    }
}
