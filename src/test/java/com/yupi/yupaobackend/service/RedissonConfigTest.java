package com.yupi.yupaobackend.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;

@SpringBootTest
public class RedissonConfigTest {

    @Resource
    private RedissonClient redissonClient;


    @Test
    public void test(){
        ArrayList<String> list = new ArrayList<>();
        list.add("11");
        list.get(0);
        list.remove(0);
        RList<Object> clientList = redissonClient.getList("test-List");
        clientList.add("yupi");
        System.out.println("clientList.get(0) = " + clientList.get(0));
    }

}