package com.yupi.yupaobackend.controller;

import com.yupi.yupaobackend.common.BaseResponse;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.common.ResultUtils;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 消息通知
 * @author: linli
 * @create: 2024-01-03 16:57
 **/
@RestController
@RequestMapping("/notice")
@CrossOrigin(origins = {"http://user.code-li.fun","http://yupao.code-li.fun","http://4c8b5c0b.r3.cpolar.top/"})
@Slf4j
public class NoticeController {

    @Resource
    private  NoticeService noticeService;

    /**
     * 获取好友申请信息
     * @param id
     * @return
     */
    @GetMapping("/friend/add")
    public BaseResponse<List<User>> getNotice(Long id){
        //校验
        if (id == null || id <0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = noticeService.getNoticeData(id);
        return ResultUtils.success(userList);
    }



}
