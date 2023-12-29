package com.yupi.yupaobackend.service;

import com.yupi.yupaobackend.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 用户服务测试
 * @author linli
 */
@SpringBootTest
class UserServiceTest {
    @Resource
    private UserService userService;

    @Test
    void testAddUser(){
        User user = new User();
        user.setUsername("dogYupi");
        user.setUserAccount("123");
        user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/AUpl4UT9k4TD5GqoCgHG8dEnlHuMbGhB10Uic06euXjbWhlu9kb6PwzsTq1e1aewmFnBKcDGqX1HUltp3YHFPUA/132");
        user.setGender(0);
        user.setUserPassword("xxx");
        user.setPhone("123");
        user.setEmail("123");
        boolean rs = userService.save(user);
        System.out.println("user.getId() = " + user.getEmail());
        Assertions.assertTrue(rs);
    }

    @Test
    void userRegister() {
        String userAccount ="yupi";
        String password ="";
        String checkPassword ="123456";
        String planetCode ="12345";
        long result = userService.userRegister(userAccount,password,checkPassword, planetCode);
        Assert.assertEquals(-1,result);
        userAccount = "yu";
        result = userService.userRegister(userAccount,password,checkPassword, planetCode);
        Assert.assertEquals(-1,result);
        userAccount = "yupi";
        password ="123456";
        result = userService.userRegister(userAccount,password,checkPassword, planetCode);
        Assert.assertEquals(-1,result);
        userAccount = "yu pi";
        password ="12345678";
        result = userService.userRegister(userAccount,password,checkPassword, planetCode);
        Assert.assertEquals(-1,result);
        checkPassword ="123456789";
        result = userService.userRegister(userAccount,password,checkPassword, planetCode);
        Assert.assertEquals(-1,result);
        userAccount = "123";
        checkPassword ="12345678";
        result = userService.userRegister(userAccount,password,checkPassword, planetCode);
        Assert.assertEquals(-1,result);
        userAccount = "yupi11122";
        result = userService.userRegister(userAccount,password,checkPassword, planetCode);
        Assert.assertTrue(result>0);
    }
}