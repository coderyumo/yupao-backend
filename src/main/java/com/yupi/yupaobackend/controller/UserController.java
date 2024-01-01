package com.yupi.yupaobackend.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupaobackend.common.BaseResponse;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.common.ResultUtils;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.dto.UserDTO;
import com.yupi.yupaobackend.model.request.CurrentUserRequest;
import com.yupi.yupaobackend.model.request.SearchUserByTagsRequest;
import com.yupi.yupaobackend.model.request.UserLoginRequest;
import com.yupi.yupaobackend.model.request.UserRegisterRequest;
import com.yupi.yupaobackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.yupi.yupaobackend.constant.RedisConstant.TOKEN_KEY;
import static com.yupi.yupaobackend.constant.RedisConstant.USER_SEARCH_KEY;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8000"})
//@CrossOrigin(origins = {"http://user.code-li.fun", "http://101.35.26.98:8000"})
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    RedisTemplate redisTemplate;

    /**
     * 用户注册
     * 9
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<String> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        String uuid = userLoginRequest.getUuid();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String key = userService.userLogin(userAccount, userPassword, uuid);
        return ResultUtils.success(key);
    }

    /**
     * 用户注销
     *
     * @param userRequest
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(@RequestBody CurrentUserRequest userRequest) {
        String key = TOKEN_KEY + userRequest.getUuid();
        Integer result = Math.toIntExact(redisTemplate.opsForHash().delete(key, userRequest.getUserAccount()));
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param userRequest
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(CurrentUserRequest userRequest) {
        String key = TOKEN_KEY + userRequest.getUuid();
        System.out.println("key = " + key);
        User user = (User) redisTemplate.opsForHash().get(key, userRequest.getUserAccount());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(User::getUserAccount, user.getUserAccount());
        User one = userService.getOne(queryWrapper);
        return ResultUtils.success(one);
    }

    @GetMapping("/search")
    public BaseResponse<Page<User>> searchUsers(long pageSize, long current, String currentUserAccount) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(User::getUserAccount, currentUserAccount);
        User currentUser = userService.getOne(wrapper);
        if (currentUser.getUserRole() == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        if (StringUtils.isNotBlank(username)) {
//            queryWrapper.like("username", username);
//        }
        Page<User> userPage = userService.page(new Page<>(current, pageSize), queryWrapper);
//        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(userPage);
    }

    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, String userAccount, String uuid) {
        User loginUser = userService.getLoginUser(userAccount, uuid);
        String key = USER_SEARCH_KEY + loginUser.getId();
        //有缓存，直接读缓存
        Page<User> userPage = (Page<User>) redisTemplate.opsForValue().get(key);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        //没有直接查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        //写缓存
        try {
            redisTemplate.opsForValue().set(key,userPage,5, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("redis set key error");
        }
        return ResultUtils.success(userPage);
    }

    @GetMapping("/search/tags")
    public BaseResponse<Page<User>> searchByTags(SearchUserByTagsRequest byTagsRequest) {
        System.out.println("tagNameList = " + byTagsRequest);
        if (CollectionUtils.isEmpty(byTagsRequest.getTagNameList())) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Page<User> users = userService.queryUsersByTags(byTagsRequest);
        return ResultUtils.success(users);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody UserDTO userDTO) {
        //校验参数是否为空
        if (userDTO == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        User loginUser = userService.getLoginUser(userDTO.getCurrentUserAccount(), userDTO.getUuid());
        Integer result = userService.updateUser(userDTO, loginUser);

        return ResultUtils.success(result);
    }


    /**
     * 获取最匹配的用户
     *
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUser(CurrentUserRequest currentUserRequest){
        int num = currentUserRequest.getNum();
        if (num <= 0 || num >20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(currentUserRequest.getUserAccount(), currentUserRequest.getUuid());
        List<User> matchUser = userService.matchUser(num, loginUser);

        return ResultUtils.success(matchUser);
    }

}
