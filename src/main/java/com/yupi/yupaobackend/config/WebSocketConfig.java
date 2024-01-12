package com.yupi.yupaobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @description:
 * @author: yumo
 * @create: 2024-01-12 00:57
 **/
@Configuration
public class WebSocketConfig {


    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
