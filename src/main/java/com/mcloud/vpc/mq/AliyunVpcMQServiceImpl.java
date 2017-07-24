package com.mcloud.vpc.mq;

import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mcloud.core.constant.PlatformEnum;
import com.mcloud.core.constant.mq.MQConstant;
import com.mcloud.core.mapper.JsonMapper;
import com.mcloud.core.util.EncodeUtils;
import com.mcloud.vpc.business.AliyunVpcBusiness;
import com.mcloud.vpc.client.VpcServiceDTO;

@Component
public class AliyunVpcMQServiceImpl implements AliyunVpcMQService {

  private static JsonMapper binder = JsonMapper.nonEmptyMapper();

  @Autowired
  private AliyunVpcBusiness business;

  @Override
  public void aliyunVpcAgg(Message message) {

    String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();

    String receiveString = EncodeUtils.EncodeMessage(message.getBody());
    
    
    System.err.println(receiveString);

    VpcServiceDTO vpcServiceDTO = binder.fromJson(receiveString, VpcServiceDTO.class);

    if (!PlatformEnum.aliyun.name().equalsIgnoreCase(vpcServiceDTO.getPlatformId())) {
      return;
    }

    if (MQConstant.ROUTINGKEY_WORK_VPC_SAVE.equalsIgnoreCase(receivedRoutingKey)) {

      business.saveVpc(vpcServiceDTO);

    } else if (MQConstant.ROUTINGKEY_WORK_VPC_UPDATE.equalsIgnoreCase(receivedRoutingKey)) {

      business.updateVpc(vpcServiceDTO);

    } else if (MQConstant.ROUTINGKEY_WORK_VPC_REMOVE.equalsIgnoreCase(receivedRoutingKey)) {

      business.removeVpc(vpcServiceDTO);
    }
  }

}
