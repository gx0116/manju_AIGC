package com.mj.agent.tools;

import com.mj.agent.constants.Constant;
import com.mj.agent.tools.result.ComicDramaInfo;
import com.mj.api.client.WorkClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ComicDramaTools {

    private final WorkClient workClient;

    @Tool(description = Constant.Tools.QUERY_MANJU_BY_ID)
    public ComicDramaInfo queryComicById(
            @ToolParam(description = Constant.ToolParams.MANJU_ID) Long comicDramaId
    ) {
        return Optional.ofNullable(comicDramaId)
                .map(id -> ComicDramaInfo.of(workClient.queryComicDramaById(id)))
                .orElse(null);
    }
}
