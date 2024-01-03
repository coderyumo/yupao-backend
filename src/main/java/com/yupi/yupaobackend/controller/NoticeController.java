package com.yupi.yupaobackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupaobackend.common.BaseResponse;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.common.ResultUtils;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.model.domain.Notice;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.service.NoticeService;
import com.yupi.yupaobackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 消息通知
 * @author: linli
 * @create: 2024-01-03 16:57
 **/
@RestController
@RequestMapping("/notice")
@CrossOrigin(origins = {"http://user.code-li.fun","http://yupao.code-li.fun"})
@Slf4j
public class NoticeController {


    @Resource
    UserService userService;

    @Resource
    NoticeService noticeService;

    @GetMapping("/list")
    public BaseResponse<List<User>> getNotice(Long id){
        //校验
        if (id == null || id <0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> applyUserList = userService.getAddFriendNotice(id);



        return ResultUtils.success(applyUserList);
    }



}
