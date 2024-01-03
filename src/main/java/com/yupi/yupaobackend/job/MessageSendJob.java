package com.yupi.yupaobackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupaobackend.model.domain.MassageSendLog;
import com.yupi.yupaobackend.model.request.AddFriendRequest;
import com.yupi.yupaobackend.service.MassageSendLogService;
import com.yupi.yupaobackend.service.impl.UserServiceImpl;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
    private MassageSendLogService sendLogService;

    @Resource
    RabbitTemplate rabbitTemplate;

    @Resource
    UserServiceImpl userService;

    /**
     * 每隔十秒执行一次
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void messageSend() {
        QueryWrapper<MassageSendLog> qw = new QueryWrapper<>();
        qw.lambda()
                .eq(MassageSendLog::getStatus, 0)
                .le(MassageSendLog::getTryTime, new Date());
        List<MassageSendLog> list = sendLogService.list(qw);
        for (MassageSendLog sendLog : list) {
            sendLog.setUpdateTime(new Date());
            if (sendLog.getTryCount() > 2) {
                //说明已经重试了三次了，此时直接设置消息发送失败
                sendLog.setStatus(2);
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
