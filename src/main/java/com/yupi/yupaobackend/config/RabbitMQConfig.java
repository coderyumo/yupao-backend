package com.yupi.yupaobackend.config;

import com.yupi.yupaobackend.model.domain.MassageSendLog;
import com.yupi.yupaobackend.service.MassageSendLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;

/**
 * @description:
 * @author: linli
 * @create: 2024-01-03 11:09
 **/
@Configuration
@Slf4j
public class RabbitMQConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {


    @Resource
    RabbitTemplate rabbitTemplate;


    @Resource
    MassageSendLogService sendLogService;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String s) {
        String msgId = correlationData.getId();
        if (ack) {
            //说明消息到达交换机
            MassageSendLog sendLog = new MassageSendLog();
            sendLog.setMsgId(msgId);
            sendLog.setStatus(1);
            sendLog.setUpdateTime(new Date());
            //更新数据库
            sendLogService.updateById(sendLog);
            log.info("消息成功到达交换机：{}", msgId);
        } else {
            log.info("消息未到达交换机:{}", msgId);
        }
    }

    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.error("消息未到达队列：{}",returnedMessage.toString());
    }
}