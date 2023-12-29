package com.yupi.yupaobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建RedisTemplate对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置Redis连接工厂
        template.setConnectionFactory(redisConnectionFactory);
        // 创建JSON序列化器（两种设置Value和HashValue的JSON的序列化器）
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer
                = new GenericJackson2JsonRedisSerializer();
        /*Jackson2JsonRedisSerializer jackson2JsonRedisSerializer
                                                          = new Jackson2JsonRedisSerializer<>(Object.class);*/

        // 设置Key和HashKey采用String序列化
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置Value和HashValue采用JSON的序列化
        template.setValueSerializer(genericJackson2JsonRedisSerializer);
        template.setHashValueSerializer(genericJackson2JsonRedisSerializer);
        //template.setValueSerializer(jackson2JsonRedisSerializer);
        //template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }

}
