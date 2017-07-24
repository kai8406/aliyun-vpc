package com.mcloud.vpc.mq;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import com.mcloud.core.constant.mq.MQConstant;

public interface AliyunVpcMQService {

  @RabbitListener(bindings = @QueueBinding(
      value = @Queue(value = "cmop.agg.aliyun.vpc", durable = "false", autoDelete = "true"),
      key = "vulcanus.vpc.*",
      exchange = @Exchange(value = MQConstant.MQ_EXCHANGE_NETWORK, type = ExchangeTypes.TOPIC)))
  public void aliyunVpcAgg(Message message);

}
