package com.yupi.yupaobackend.once;

import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class InsertUsers {


    @Resource
    private UserService userService;

    private final ExecutorService executorService = new ThreadPoolExecutor(100,1000,1000, TimeUnit.MINUTES,new ArrayBlockingQueue<>(10000));

//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void insertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ArrayList<User> users = new ArrayList<>();
        final int NUM = 10000000;
        for (int i = 70000; i < NUM; i++) {
            User user = new User();
            user.setUsername("假用户" + i);
            user.setUserAccount("abin" + i);
            user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/AUpl4UT9k4TD5GqoCgHG8dEnlHuMbGhB10Uic06euXjbWhlu9kb6PwzsTq1e1aewmFnBKcDGqX1HUltp3YHFPUA/132");
            user.setGender(0);
            user.setUserPassword("123456789");
            user.setPhone("1123");
            user.setEmail("1123@qq.com");
            user.setPlanetCode("6" + i);
            user.setTags("[\"大一\",\"java\",\"c++\",\"python\",\"c\"]");
            user.setProfile("大家好，我是渣渣辉，是兄弟就来砍我");
            users.add(user);
        }
        userService.saveBatch(users, 10000);
        stopWatch.stop();
        System.out.println("stopWatch.getTotalTimeMillis() = " + stopWatch.getTotalTimeMillis());
    }


//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
//    public void doConcurrencyInsertUsers() {
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        final int NUM = 1000000;
//        //分1000组
//        int j = 0;
//        ArrayList<CompletableFuture<Void>> futureArrayList = new ArrayList<>();
//        // 使用自定义线程池
//
//        for (int i = 0; i < 100; i++) {
//            ArrayList<User> userList = new ArrayList<>();
//
//            while (true) {
//                j++;
//                User user = new User();
//                user.setUsername("假用户" + j);
//                user.setUserAccount("abin" + j);
//                user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/AUpl4UT9k4TD5GqoCgHG8dEnlHuMbGhB10Uic06euXjbWhlu9kb6PwzsTq1e1aewmFnBKcDGqX1HUltp3YHFPUA/132");
//                user.setGender(0);
//                user.setUserPassword("123456789");
//                user.setPhone("1123");
//                user.setEmail("1123@qq.com");
//                user.setPlanetCode("6" + j);
//                user.setTags("[\"大一\",\"java\",\"c++\",\"python\",\"c\"]");
//                user.setProfile("大家好，我是渣渣辉，是兄弟就来砍我");
//                userList.add(user);
//                if (j % 1000 == 0) {
//                    break;
//                }
//            }
//            //异步执行
//            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                userService.saveBatch(userList, 1000);
//            }, executorService);
//            futureArrayList.add(future);
//        }
//        stopWatch.stop();
//        System.out.println("stopWatch.getTotalTimeMillis() = " + stopWatch.getTotalTimeMillis());
//    }



//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int TOTAL_USERS = 1000000;
        final int BATCH_SIZE = 1000;
        final int TOTAL_BATCHES = TOTAL_USERS / BATCH_SIZE;

        List<CompletableFuture<Void>> futureArrayList = new ArrayList<>();
        for (int i = 0; i < TOTAL_BATCHES; i++) {
            List<User> userList = new ArrayList<>();
            for (int j = 0; j < BATCH_SIZE; j++) {
                int userIndex = i * BATCH_SIZE + j + 1;
                if (userIndex > TOTAL_USERS) {
                    break;
                }
                User user = new User();
                user.setUsername("假用户" + userIndex);
                user.setUserAccount("abin" + userIndex);
                user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/AUpl4UT9k4TD5GqoCgHG8dEnlHuMbGhB10Uic06euXjbWhlu9kb6PwzsTq1e1aewmFnBKcDGqX1HUltp3YHFPUA/132");
                user.setGender(0);
                user.setUserPassword("123456789");
                user.setPhone("1123");
                user.setEmail("1123@qq.com");
                user.setPlanetCode("6" + userIndex);
                user.setTags("[\"大一\",\"java\",\"c++\",\"python\",\"c\"]");
                user.setProfile("大家好，我是渣渣辉，是兄弟就来砍我");
                userList.add(user);
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    userService.saveBatch(userList, BATCH_SIZE);
                } catch (Exception e) {
                    // 适当处理异常（记录日志或处理错误）
                    e.printStackTrace();
                }
            }, executorService);

            futureArrayList.add(future);
        }

        // 等待所有异步任务完成
        CompletableFuture[] futures = futureArrayList.toArray(new CompletableFuture[0]);
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.join(); // 等待所有任务完成

        stopWatch.stop();
        System.out.println("stopWatch.getTotalTimeMillis() = " + stopWatch.getTotalTimeMillis());
    }

}
