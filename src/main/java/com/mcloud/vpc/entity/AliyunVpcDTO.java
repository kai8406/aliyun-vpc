package com.mcloud.vpc.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mcloud.core.constant.ActiveEnum;
import com.mcloud.core.constant.PlatformEnum;
import com.mcloud.vpc.constant.AliyunVpcStatusEnum;

import lombok.Data;

@Data
@Entity
@Table(name = "aliyun_vpc")
public class AliyunVpcDTO {

  @Id
  @GenericGenerator(name = "system-uuid", strategy = "uuid")
  @GeneratedValue(generator = "system-uuid")
  private String id;

  /**
   * 数据状态, A:活跃 N:不活跃,用于显示数据.
   */
  @JsonIgnore
  @Column(name = "active")
  private String active = ActiveEnum.A.name();

  /**
   * 平台ID. {@link PlatformEnum}
   */
  @Column(name = "platform_id")
  private String platformId;

  /**
   * 区域.
   */
  @Column(name = "region_id")
  private String regionId;

  /**
   * task对象,不持久化.
   */
  @Transient
  private String taskId = "";

  /**
   * 用户名,唯一.
   */
  @Column(name = "user_name")
  private String username;

  /**
   * 平台资源的唯一标识符.
   */
  @Column(name = "uuid")
  private String uuid = "";

  /**
   * CIDR
   */
  @Column(name = "cidr_block")
  private String cidrBlock;

  /**
   * 说明
   */
  @Column(name = "description")
  private String description;

  /**
   * 资源状态.{@link AliyunVpcStatusEnum}
   */
  @Column(name = "status")
  private String status = AliyunVpcStatusEnum.Pending.name();

  /**
   * VPC聚合服务主键.
   */
  @Column(name = "vpc_id")
  private String vpcId;

  /**
   * VPC名称
   */
  @Column(name = "vpc_name")
  private String vpcName;

  /**
   * 创建时间
   */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "create_time")
  private Date createTime;

  /**
   * 修改时间
   */
  @JsonIgnore
  @Column(name = "modify_time")
  private Date modifyTime;

}
