package com.yupi.yupaobackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupaobackend.model.domain.MessageSendLog;
import com.yupi.yupaobackend.model.enums.AddFriendStatusEnum;
import com.yupi.yupaobackend.model.request.AddFriendRequest;
import com.yupi.yupaobackend.service.MessageSendLogService;
import com.yupi.yupaobackend.service.impl.UserServiceImpl;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @description:
 * @author: linli
 * @create: 2024-01-03 14:19
 **/
@Component
public class MessageSendJob {


    @Resource
    private MessageSendLogService sendLogService;

    @Resource
    private  RabbitTemplate rabbitTemplate;

    @Resource
    private  UserServiceImpl userService;

    /**
     * 每隔十秒执行一次
     */
//    @Scheduled(cron = "0/10 * * * * ?")
    public void messageSend() {
        QueryWrapper<MessageSendLog> qw = new QueryWrapper<>();
        qw.lambda()
                .eq(MessageSendLog::getStatus, 0)
                .le(MessageSendLog::getTryTime, new Date());
        List<MessageSendLog> list = sendLogService.list(qw);
        for (MessageSendLog sendLog : list) {
            sendLog.setUpdateTime(new Date());
            if (sendLog.getTryCount() > 2) {
                //说明已经重试了三次了，此时直接设置消息发送失败
                sendLog.setStatus(2);
                sendLog.setAddFriendStatus(AddFriendStatusEnum.ADD_ERROR.getValue());
                sendLogService.updateById(sendLog);
            }else {
                //还未达到上限，重试
                AddFriendRequest addFriendRequest = new AddFriendRequest();
                addFriendRequest.setSenderId(sendLog.getSenderId());
                addFriendRequest.setRecipientId(sendLog.getRecipientId());
                //更新重试次数
                sendLog.setTryCount(sendLog.getTryCount() + 1);
                sendLogService.updateById(sendLog);
                rabbitTemplate.convertAndSend(sendLog.getExchange(),sendLog.getRouteKey(),addFriendRequest,
                        new CorrelationData(sendLog.getMsgId()));
            }
        }

    }


}
