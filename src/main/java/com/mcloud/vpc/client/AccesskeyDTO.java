package com.mcloud.vpc.client;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mcloud.core.constant.PlatformEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccesskeyDTO {

  /**
   * UUID主键.
   */
  public String id;

  /**
   * 平台ID. {@link PlatformEnum}
   */
  public String platformId;

  /**
   * 用户名,唯一.
   */
  public String username;

  /**
   * 云平台accesskeyId.
   */
  public String accesskeyId;

  /**
   * 云平台accesskeySecret.
   */
  public String accesskeySecret;

  /**
   * 创建时间.
   */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /**
   * 修改时间.
   */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date modifyTime;

}
