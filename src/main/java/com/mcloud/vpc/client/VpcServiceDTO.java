package com.mcloud.vpc.client;

import javax.persistence.Column;

import com.mcloud.core.constant.PlatformEnum;

import lombok.Data;

/**
 * VPC聚合服务持久化对象.
 * 
 * @author liukai
 *
 */
@Data
public class VpcServiceDTO {

  /**
   * UUID主键.
   */
  private String vpcId;

  /**
   * 平台ID. {@link PlatformEnum}
   */
  private String platformId;

  /**
   * 区域.
   */
  private String regionId;

  /**
   * task code,不持久化.
   */
  private String taskCode = "";

  /**
   * 用户名,唯一.
   */
  private String username;

  /**
   * 平台资源的唯一标识符.
   */
  private String vpcUuid = "";

  /**
   * CIDR.
   */
  private String cidrBlock;

  /**
   * 说明.
   */
  private String description = "";

  /**
   * 服务/资源状态.{@link VpcStatusEnum}
   */
  @Column(name = "status")
  private String status;

  /**
   * VPC名称.
   */
  private String vpcName = "";

}
