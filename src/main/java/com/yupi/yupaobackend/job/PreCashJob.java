package com.yupi.yupaobackend.job;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupaobackend.mapper.UserMapper;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.yupi.yupaobackend.constant.RedisConstant.USER_SEARCH_KEY;

/**
 * 缓存预热
 */
@Component
@Slf4j
public class PreCashJob {

    @Resource
    private   RedisTemplate redisTemplate;

    @Resource
    private   UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private  RedissonClient redissonClient;

//    List<Long> mainUserList = Collections.singletonList(3L);

    // 每天晚上 23:30 执行一次任务
    @Scheduled(cron = "0 30 23 * * ?")
    public void doCashRecommendTask() {
        RLock lock = redissonClient.getLock("yupao:prechchsjob:docache:lock");

        try {
            //只有一个线程会获取锁
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                List<Long> mainUserList = new ArrayList<>();
                //为所有用户进行用户推荐
                List<User> list = userService.list();
                for (User user : list) {
                    mainUserList.add(user.getId());
                }
                for (Long userId : mainUserList) {
                    //没有直接查询数据库
                    List<User> userList = userMapper.searchAddCount();
                    // 执行你的任务逻辑
                    String key = USER_SEARCH_KEY + userId;
                    //写缓存
                    try {
                        redisTemplate.opsForValue().set(key, userList, 24, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.error("redis set key error");
                    }
                }
            }

        } catch (InterruptedException e) {
            log.error("redis set key error");
        } finally {
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }
}
