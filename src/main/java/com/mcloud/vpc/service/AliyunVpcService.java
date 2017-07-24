package com.mcloud.vpc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mcloud.core.persistence.BaseEntityCrudServiceImpl;
import com.mcloud.vpc.entity.AliyunVpcDTO;
import com.mcloud.vpc.repository.AliyunVpcRepository;

@Service
@Transactional
public class AliyunVpcService extends BaseEntityCrudServiceImpl<AliyunVpcDTO, AliyunVpcRepository> {

  @Autowired
  private AliyunVpcRepository repository;

  @Override
  protected AliyunVpcRepository getRepository() {
    return repository;
  }

}
