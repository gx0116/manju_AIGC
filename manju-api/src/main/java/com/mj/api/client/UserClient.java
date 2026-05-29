package com.mj.api.client;

import com.mj.api.domain.po.User;
import com.mj.common.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/profile")
    Result<User> profile(@RequestHeader("Authorization") String authHeader);
}
