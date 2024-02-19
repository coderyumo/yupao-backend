package com.yupi.yupaobackend.controller;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupaobackend.common.BaseResponse;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.common.ResultUtils;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.mapper.UserMapper;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.dto.UserDTO;
import com.yupi.yupaobackend.model.request.*;
import com.yupi.yupaobackend.model.vo.WebSocketRespVO;
import com.yupi.yupaobackend.server.WebSocketServer;
import com.yupi.yupaobackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yupi.yupaobackend.constant.RedisConstant.TOKEN_KEY;
import static com.yupi.yupaobackend.constant.RedisConstant.USER_SEARCH_KEY;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://user.code-li.fun", "http://yupao.code-li.fun", "http://4c8b5c0b.r3.cpolar.top/"})
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private WebSocketServer webSocketServer;

    @Resource
    private UserMapper userMapper;

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
        String activeIds = userRegisterRequest.getTags();
        String avatarUrl = userRegisterRequest.getAvatarUrl();
        String username = userRegisterRequest.getUsername();
        String phone = userRegisterRequest.getPhone();
        String email = userRegisterRequest.getEmail();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode, activeIds, avatarUrl, username,
                phone, email
        )) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        long result = userService.userRegister(userRegisterRequest);
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
        } else {
            redisTemplate.expire(TOKEN_KEY + userRequest.getUuid(), 10, TimeUnit.MINUTES);
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
        // 参数验证
        if (pageSize <= 0 || pageNum <= 0 || StringUtils.isEmpty(userAccount) || StringUtils.isEmpty(uuid)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(userAccount, uuid);
        String key = USER_SEARCH_KEY + loginUser.getId();

        // 读取缓存
        List<User> userList = (List<User>) redisTemplate.opsForValue().get(key);
        Page<User> userPage = new Page<>();
        // 如果缓存有数据，直接返回
        if (CollectionUtils.isNotEmpty(userList)) {
            userList = userList.stream()
                    .filter(user -> user.getId() != loginUser.getId())
                    .collect(Collectors.toList());
            userPage.setRecords(userList);
            return ResultUtils.success(userPage);
        }

        // 查询数据库
        userList = userMapper.searchAddCount();

        // 过滤当前登录用户
        userList = userList
                .stream()
                .filter(user -> user.getId() != loginUser.getId())
                .collect(Collectors.toList());

        // 对用户进行处理
        List<User> safetyUsers = userList.stream().map(userService::getSafetyUser).collect(Collectors.toList());
        userPage.setRecords(safetyUsers);

        // 写缓存
        try {
            redisTemplate.opsForValue().set(key, userList, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error setting Redis key", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
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
    public BaseResponse<List<User>> matchUser(CurrentUserRequest currentUserRequest) {
        int num = currentUserRequest.getNum();
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(currentUserRequest.getUserAccount(), currentUserRequest.getUuid());
        List<User> matchUser = userService.matchUser(num, loginUser);

        return ResultUtils.success(matchUser);
    }

    @PostMapping("/friend/add")
    public BaseResponse<Boolean> addFriend(@RequestBody AddFriendRequest addFriendRequest) {
        if (addFriendRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String key = TOKEN_KEY + addFriendRequest.getUuid();
        System.out.println("key = " + key);
        User user = (User) redisTemplate.opsForHash().get(key, addFriendRequest.getUserAccount());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Boolean result = userService.addFriend(addFriendRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/friend/delete")
    public BaseResponse<Boolean> deleteFriend(@RequestBody DeleteFriendRequest deleteFriendRequest) {
        if (deleteFriendRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Boolean result = userService.deleteFriend(deleteFriendRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/friend/agree")
    public BaseResponse<Boolean> agreeFriend(@RequestBody AddFriendRequest addFriendRequest) {
        WebSocketRespVO webSocketRespVO = new WebSocketRespVO();
        boolean agree = userService.agreeFriend(addFriendRequest);
        User user = userService.getById(addFriendRequest.getRecipientId());
        webSocketRespVO.setMessage("添加" + user.getUsername() + "的好友申请已通过");
        webSocketRespVO.setIsAgree(true);
        webSocketRespVO.setSenderId(addFriendRequest.getSenderId());
        webSocketServer.sendToAllClient(JSONUtil.toJsonStr(webSocketRespVO));
        return ResultUtils.success(true);
    }

    @PostMapping("/friend/reject")
    public BaseResponse<Boolean> rejectFriend(@RequestBody AddFriendRequest addFriendRequest) {
        WebSocketRespVO webSocketRespVO = new WebSocketRespVO();
        boolean agree = userService.rejectFriend(addFriendRequest);
        User user = userService.getById(addFriendRequest.getRecipientId());
        webSocketRespVO.setMessage("添加" + user.getUsername() + "的好友申请被拒绝");
        webSocketRespVO.setIsAgree(false);
        webSocketRespVO.setSenderId(addFriendRequest.getSenderId());
        webSocketServer.sendToAllClient(JSONUtil.toJsonStr(webSocketRespVO));
        return ResultUtils.success(true);
    }

    @GetMapping("/friend/list")
    public BaseResponse<List<User>> listFriend(CurrentUserRequest currentUserRequest) {
        if (currentUserRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(currentUserRequest.getUserAccount(), currentUserRequest.getUuid());
        List<User> friendList = userService.listFriend(loginUser);
        return ResultUtils.success(friendList);
    }

    @PostMapping("/refresh/cache")
    public BaseResponse<Boolean> refreshCache(@RequestBody CurrentUserRequest currentUserRequest) {
        boolean refresh = userService.refreshCache(currentUserRequest);
        return ResultUtils.success(refresh);
    }

}
