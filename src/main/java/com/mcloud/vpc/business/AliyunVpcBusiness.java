package com.mcloud.vpc.business;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.CreateVpcRequest;
import com.aliyuncs.ecs.model.v20140526.CreateVpcResponse;
import com.aliyuncs.ecs.model.v20140526.DeleteVpcRequest;
import com.aliyuncs.ecs.model.v20140526.DeleteVpcResponse;
import com.aliyuncs.ecs.model.v20140526.ModifyVpcAttributeRequest;
import com.aliyuncs.ecs.model.v20140526.ModifyVpcAttributeResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.mcloud.core.constant.ActiveEnum;
import com.mcloud.core.constant.mq.MQConstant;
import com.mcloud.core.constant.result.ResultDTO;
import com.mcloud.core.constant.result.ResultEnum;
import com.mcloud.core.constant.task.TaskDTO;
import com.mcloud.core.mapper.BeanMapper;
import com.mcloud.core.mapper.JsonMapper;
import com.mcloud.vpc.client.AccesskeyDTO;
import com.mcloud.vpc.client.AccountClient;
import com.mcloud.vpc.client.VpcServiceDTO;
import com.mcloud.vpc.constant.AliyunVpcStatusEnum;
import com.mcloud.vpc.entity.AliyunVpcDTO;
import com.mcloud.vpc.service.AliyunVpcService;

@Component
public class AliyunVpcBusiness {

  private static JsonMapper binder = JsonMapper.nonEmptyMapper();

  /**
   * 阿里云初始化连接.
   * 
   * @param regionId
   * @param accesskeyDTO
   * @return
   */
  private static IAcsClient getServiceInstance(String regionId, AccesskeyDTO accesskeyDTO) {
    DefaultProfile profile = DefaultProfile.getProfile(regionId, accesskeyDTO.getAccesskeyId(),
        accesskeyDTO.getAccesskeySecret());
    IAcsClient client = new DefaultAcsClient(profile);
    return client;
  }

  @Autowired
  private AccountClient accountClient;

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  private AliyunVpcService service;

  /**
   * 根据阿里云的Id获得AliyunVpcDTO对象.
   * 
   * @param uuid
   * @return
   */
  private AliyunVpcDTO getAliyunVpcDTOByUUID(String uuid) {
    Map<String, Object> map = new HashMap<>();
    map.put("EQ_uuid", uuid);
    return service.find(map);
  }

  public void removeVpc(VpcServiceDTO vpcServiceDTO) {

    // Step.1 获得Task对象.
    TaskDTO taskDTO = new TaskDTO();
    taskDTO.setAction(MQConstant.ROUTINGKEY_WORK_VPC_REMOVE);
    taskDTO.setUsername(vpcServiceDTO.getUsername());
    taskDTO.setRequestData(vpcServiceDTO.toString());
    taskDTO.setTaskCode(vpcServiceDTO.getTaskCode());

    // Step.2 根据username获得阿里云accesskeyId和accesskeySecret
    AccesskeyDTO accesskeyDTO = accountClient
        .getAccesskey(vpcServiceDTO.getUsername(), vpcServiceDTO.getPlatformId()).getData();

    // Step.3 持久化AliyunVpcDTO
    AliyunVpcDTO aliyunVpcDTO = getAliyunVpcDTOByUUID(vpcServiceDTO.getVpcUuid());

    // Step.4 调用阿里云SDK执行操作.
    DeleteVpcRequest deleteVpcRequest = new DeleteVpcRequest();
    deleteVpcRequest.setVpcId(aliyunVpcDTO.getUuid());

    IAcsClient client = getServiceInstance(aliyunVpcDTO.getRegionId(), accesskeyDTO);

    DeleteVpcResponse response = null;

    try {

      response = client.getAcsResponse(deleteVpcRequest);

      taskDTO.setRequestId(response.getRequestId());

    } catch (ClientException e) {

      // 修改DB中VPC的状态.
      aliyunVpcDTO.setStatus(AliyunVpcStatusEnum.Error.name());// Available or Pending or Error
      aliyunVpcDTO = service.saveAndFlush(aliyunVpcDTO);

      /// 修改Task对象执行状态.
      taskDTO.setResponseData("ErrCode:" + e.getErrCode() + " | ErrMsg:" + e.getErrMsg());

      // 将task对象发布到mq.
      rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_LOG, MQConstant.ROUTINGKEY_TASK_SAVE,
          binder.toJson(taskDTO));

      ResultDTO resultDTO = new ResultDTO(new HashMap<>(), taskDTO.getResponseData(),
          vpcServiceDTO.getRegionId(), "", ResultEnum.ERROR.name(), vpcServiceDTO.getTaskCode(),
          vpcServiceDTO.getUsername(), vpcServiceDTO.getVpcId());

      // 将执行的结果进行广播.
      rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NETWORK,
          MQConstant.ROUTINGKEY_RESULT_REMOVE, binder.toJson(resultDTO));
      return;
    }

    // 将task对象发布到mq.
    rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_LOG, MQConstant.ROUTINGKEY_TASK_SAVE,
        binder.toJson(taskDTO));

    // Step.4 更新Task和服务对象.

    aliyunVpcDTO.setActive(ActiveEnum.N.name());
    aliyunVpcDTO.setModifyTime(new Date());
    aliyunVpcDTO = service.saveAndFlush(aliyunVpcDTO);

    ResultDTO resultDTO = new ResultDTO(new HashMap<>(), "", vpcServiceDTO.getRegionId(),
        aliyunVpcDTO.getUuid(), ResultEnum.SUCCESS.name(), vpcServiceDTO.getTaskCode(),
        vpcServiceDTO.getUsername(), vpcServiceDTO.getVpcId());

    // Step.5 将执行的结果进行广播.
    rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NETWORK,
        MQConstant.ROUTINGKEY_RESULT_VPC_REMOVE, binder.toJson(resultDTO));
  }

  public void saveVpc(VpcServiceDTO vpcServiceDTO) {

    // Step.1 获得Task对象.
    TaskDTO taskDTO = new TaskDTO();
    taskDTO.setAction(MQConstant.ROUTINGKEY_WORK_VPC_SAVE);
    taskDTO.setUsername(vpcServiceDTO.getUsername());
    taskDTO.setRequestData(vpcServiceDTO.toString());
    taskDTO.setTaskCode(vpcServiceDTO.getTaskCode());

    // Step.2 根据username获得阿里云accesskeyId和accesskeySecret
    AccesskeyDTO accesskeyDTO = accountClient
        .getAccesskey(vpcServiceDTO.getUsername(), vpcServiceDTO.getPlatformId()).getData();

    // Step.3 持久化AliyunVpcDTO

    AliyunVpcDTO aliyunVpcDTO = BeanMapper.map(vpcServiceDTO, AliyunVpcDTO.class);
    aliyunVpcDTO.setVpcId(vpcServiceDTO.getVpcId());
    aliyunVpcDTO.setCreateTime(new Date());

    aliyunVpcDTO = service.saveAndFlush(aliyunVpcDTO);

    // Step.4 调用阿里云SDK执行操作.
    CreateVpcRequest createVpcRequest = new CreateVpcRequest();
    createVpcRequest.setDescription(aliyunVpcDTO.getDescription());
    createVpcRequest.setCidrBlock(aliyunVpcDTO.getCidrBlock());
    createVpcRequest.setRegionId(aliyunVpcDTO.getRegionId());
    createVpcRequest.setVpcName(aliyunVpcDTO.getVpcName());

    IAcsClient client = getServiceInstance(aliyunVpcDTO.getRegionId(), accesskeyDTO);

    CreateVpcResponse createVpcResponse = null;

    try {

      createVpcResponse = client.getAcsResponse(createVpcRequest);

      taskDTO.setRequestId(createVpcResponse.getRequestId());

    } catch (ClientException e) {

      // 修改DB中VPC的状态.
      aliyunVpcDTO.setStatus(AliyunVpcStatusEnum.Error.name());// Available or Pending or Error
      aliyunVpcDTO = service.saveAndFlush(aliyunVpcDTO);

      /// 修改Task对象执行状态.
      taskDTO.setResponseData("ErrCode:" + e.getErrCode() + " | ErrMsg:" + e.getErrMsg());

      // 将task对象发布到mq.
      rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_LOG, MQConstant.ROUTINGKEY_TASK_SAVE,
          binder.toJson(taskDTO));


      ResultDTO resultDTO = new ResultDTO(new HashMap<>(), taskDTO.getResponseData(),
          vpcServiceDTO.getRegionId(), "", ResultEnum.ERROR.name(), vpcServiceDTO.getTaskCode(),
          vpcServiceDTO.getUsername(), vpcServiceDTO.getVpcId());


      // 将执行的结果进行广播.
      rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NETWORK,
          MQConstant.ROUTINGKEY_RESULT_SAVE, binder.toJson(resultDTO));
      return;
    }

    // 将task对象发布到mq.
    rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_LOG, MQConstant.ROUTINGKEY_TASK_SAVE,
        binder.toJson(taskDTO));

    // Step.4 更新Task和服务对象.
    aliyunVpcDTO.setStatus(AliyunVpcStatusEnum.Available.name());// Available or Pending or Error
    aliyunVpcDTO.setUuid(createVpcResponse.getVpcId());
    aliyunVpcDTO = service.saveAndFlush(aliyunVpcDTO);

    Map<String, Object> map = new HashMap<>();
    map.put("ip", "172.10.10.11");

    ResultDTO resultDTO = new ResultDTO(map, "", vpcServiceDTO.getRegionId(),
        aliyunVpcDTO.getUuid(), ResultEnum.SUCCESS.name(), vpcServiceDTO.getTaskCode(),
        vpcServiceDTO.getUsername(), vpcServiceDTO.getVpcId());

    // Step.5 将执行的结果进行广播.
    rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NETWORK,
        MQConstant.ROUTINGKEY_RESULT_VPC_SAVE, binder.toJson(resultDTO));
  }

  public void updateVpc(VpcServiceDTO vpcServiceDTO) {

    // Step.1 获得Task对象.
    TaskDTO taskDTO = new TaskDTO();
    taskDTO.setAction(MQConstant.ROUTINGKEY_WORK_VPC_UPDATE);
    taskDTO.setUsername(vpcServiceDTO.getUsername());
    taskDTO.setRequestData(vpcServiceDTO.toString());
    taskDTO.setTaskCode(vpcServiceDTO.getTaskCode());

    // Step.2 根据username获得阿里云accesskeyId和accesskeySecret
    AccesskeyDTO accesskeyDTO = accountClient
        .getAccesskey(vpcServiceDTO.getUsername(), vpcServiceDTO.getPlatformId()).getData();

    // Step.3 查询AliyunVpcDTO.
    AliyunVpcDTO aliyunVpcDTO = getAliyunVpcDTOByUUID(vpcServiceDTO.getVpcUuid());

    // Step.4 调用阿里云SDK执行操作.
    ModifyVpcAttributeRequest modifyVpcAttributeRequest = new ModifyVpcAttributeRequest();
    modifyVpcAttributeRequest.setDescription(vpcServiceDTO.getDescription());
    modifyVpcAttributeRequest.setVpcName(vpcServiceDTO.getVpcName());
    modifyVpcAttributeRequest.setVpcId(vpcServiceDTO.getVpcUuid());

    IAcsClient client = getServiceInstance(aliyunVpcDTO.getRegionId(), accesskeyDTO);

    ModifyVpcAttributeResponse response = null;

    try {

      response = client.getAcsResponse(modifyVpcAttributeRequest);

      taskDTO.setRequestId(response.getRequestId());

    } catch (ClientException e) {

      // 修改DB中VPC的状态.
      aliyunVpcDTO.setStatus(AliyunVpcStatusEnum.Error.name());// Available or Pending or Error
      aliyunVpcDTO = service.saveAndFlush(aliyunVpcDTO);

      /// 修改Task对象执行状态.
      taskDTO.setResponseData("ErrCode:" + e.getErrCode() + " | ErrMsg:" + e.getErrMsg());

      // 将task对象发布到mq.
      rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_LOG, MQConstant.ROUTINGKEY_TASK_SAVE,
          binder.toJson(taskDTO));



      ResultDTO resultDTO = new ResultDTO(new HashMap<>(), taskDTO.getResponseData(),
          vpcServiceDTO.getRegionId(), "", ResultEnum.ERROR.name(), vpcServiceDTO.getTaskCode(),
          vpcServiceDTO.getUsername(), vpcServiceDTO.getVpcId());


      // 将执行的结果进行广播.
      rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NETWORK,
          MQConstant.ROUTINGKEY_RESULT_UPDATE, binder.toJson(resultDTO));
      return;
    }

    // // Step.5 更新Task和服务对象.
    aliyunVpcDTO.setDescription(vpcServiceDTO.getDescription());
    aliyunVpcDTO.setVpcName(vpcServiceDTO.getVpcName());
    aliyunVpcDTO.setModifyTime(new Date());
    aliyunVpcDTO = service.saveAndFlush(aliyunVpcDTO);

    // 将task对象发布到mq.
    rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NETWORK, MQConstant.ROUTINGKEY_TASK_SAVE,
        binder.toJson(taskDTO));


    ResultDTO resultDTO = new ResultDTO(new HashMap<>(), "", vpcServiceDTO.getRegionId(),
        aliyunVpcDTO.getUuid(), ResultEnum.SUCCESS.name(), vpcServiceDTO.getTaskCode(),
        vpcServiceDTO.getUsername(), vpcServiceDTO.getVpcId());


    // Step.6 将执行的结果进行广播.
    rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NETWORK,
        MQConstant.ROUTINGKEY_RESULT_VPC_MODIFY, binder.toJson(resultDTO));
  }

}
