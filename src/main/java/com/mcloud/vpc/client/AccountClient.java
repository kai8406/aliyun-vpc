package com.mcloud.vpc.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.mcloud.core.result.RestResult;

@FeignClient("account-service")
public interface AccountClient {

  @GetMapping("/account/accesskey/user/{username}/platformId/{platformId}")
  RestResult<AccesskeyDTO> getAccesskey(@PathVariable(value = "username") String username,
      @PathVariable(value = "platformId") String platformId);

}
