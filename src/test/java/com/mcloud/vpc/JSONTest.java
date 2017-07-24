package com.mcloud.vpc;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcloud.core.mapper.JsonMapper;
import com.mcloud.vpc.client.AccesskeyDTO;

public class JSONTest {

  private static JsonMapper binder = JsonMapper.nonEmptyMapper();

  @Test
  public void jsonTest() throws JsonProcessingException, IOException {

    String json =
        "{\"resultCode\":0,\"resultMessage\":\"执行成功.\",\"taskId\":\"\",\"data\":{\"id\":\"8a80cb815c7bb86f015c7c6bf94f0002\",\"userId\":\"8a80cb815c7bb86f015c7bb96cda0001\",\"platformId\":\"aliyun\",\"accesskeyId\":\"LTAI81FKWaEzzZbA\",\"accesskeySecret\":\"CRtmJaImTDTl6A9s9IRH1aiS3r4LOQ\",\"createTime\":\"2017-06-06 08:01:10\"}}";

    ObjectMapper map = new ObjectMapper();
    JsonNode node = map.readTree(json);

    String data = node.get("data").toString();

    System.out.println(data);

    AccesskeyDTO accesskeyDTO = binder.fromJson(data, AccesskeyDTO.class);

    System.err.println(accesskeyDTO);

  }

}
