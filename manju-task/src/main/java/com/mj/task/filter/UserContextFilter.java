package com.mj.task.filter;

import com.mj.common.context.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(-100)
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        String usernameHeader = httpRequest.getHeader("X-User-Name");

        if (userIdHeader != null && usernameHeader != null) {
            UserContext.setUser(Long.valueOf(userIdHeader), usernameHeader);
            log.debug("UserContext 已设置, userId: {}, username: {}", userIdHeader, usernameHeader);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
