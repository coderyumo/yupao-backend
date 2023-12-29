package com.yupi.yupaobackend.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: Redisson 配置
 * @author: linli
 * @create: 2023-12-28 21:15
 * @Version 1.0
 **/
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String host;

    private String port;

    private String password;

    @Bean
    RedissonClient redissonClient() {
        Config config = new Config();
        String url = String.format("redis://%s:%s", host, port);
        config.useSingleServer().setAddress(url).setPassword(password).setDatabase(3);
        return Redisson.create(config);
    }
}
