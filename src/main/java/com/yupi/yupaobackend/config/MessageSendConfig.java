package com.yupi.yupaobackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @description: 发送添加好友信息
 *
 * @author: linli
 * @create: 2024-01-03 13:38
 **/
@Configuration
public class MessageSendConfig {

    //添加好友的队列
    public static final String ADD_FRIEND_SEND_QUEUE_NAME = "add_friend_send_queue_name";

    //添加好友的交换机
    public static final String ADD_FRIEND_SEND_EXCHANGE_NAME = "add_friend_send_exchange_name";

    @Bean
    Queue addFriendSendQueue(){
        return new Queue(ADD_FRIEND_SEND_QUEUE_NAME,true,false,false);
    }

    @Bean
    DirectExchange addFriendSendExchange(){
        return new DirectExchange(ADD_FRIEND_SEND_EXCHANGE_NAME,true,false);
    }

    @Bean
    Binding addFriendSendBinding(){
        return BindingBuilder.bind(addFriendSendQueue())
                .to(addFriendSendExchange())
                .with(ADD_FRIEND_SEND_QUEUE_NAME);
    }
}
