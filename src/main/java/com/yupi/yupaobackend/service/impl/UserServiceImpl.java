package com.yupi.yupaobackend.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.constant.UserConstant;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.mapper.UserMapper;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.dto.UserDTO;
import com.yupi.yupaobackend.model.request.SearchUserByTagsRequest;
import com.yupi.yupaobackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.yupaobackend.constant.RedisConstant.TOKEN_KEY;
import static com.yupi.yupaobackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author linli
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-12-15 22:57:04
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    RedisTemplate redisTemplate;

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(
            String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    @Override
    public String userLogin(String userAccount, String userPassword, String uuid) {

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        String currentToken = userAccount + "-" + uuid;
        // 从Redis中查询用户是否存在
        User cashUser = (User) redisTemplate.opsForHash().get(TOKEN_KEY + uuid, userAccount);
        if (cashUser != null) {
            redisTemplate.expire(TOKEN_KEY + uuid, 30, TimeUnit.MINUTES);
            return currentToken;
        }
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        String newUuid = UUID.randomUUID().toString().replace("-", "");
        log.info("uuid ==================> {}", uuid);
        String token = userAccount + "-" + newUuid; // 1. 校验
        // 4. 存储用户信息到Redis中,设置key过期时间和token过期时间
        redisTemplate.opsForHash().put(TOKEN_KEY + newUuid, safetyUser.getUserAccount(), safetyUser);
        redisTemplate.expire(TOKEN_KEY + newUuid, 24, TimeUnit.HOURS);
        //    request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return token;
    }

    /**
     * 获取当前用户
     *
     * @param userAccount
     * @param uuid
     * @return
     */
    @Override
    public User getLoginUser(String userAccount, String uuid) {
        // 从Redis中查询用户是否存在
        User cashUser = (User) redisTemplate.opsForHash().get(TOKEN_KEY + uuid, userAccount);
        if (cashUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return cashUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户 内存中筛选
     *
     * @param byTagsRequest 用户要拥有的标签
     * @return
     */
    @Override
    public Page<User> queryUsersByTags(SearchUserByTagsRequest byTagsRequest) {
        if (CollectionUtils.isEmpty(byTagsRequest.getTagNameList())) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Gson gson = new Gson();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = this.page(new Page<>(byTagsRequest.getPageNum(),byTagsRequest.getPageSize()),queryWrapper);
        List<User> newUserList = userPage.getRecords().stream()
                .filter(
                        user -> {
                            String tagsStr = user.getTags();
                            Set<String> tagList =
                                    gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
                                    }.getType());
                            tagList = Optional.ofNullable(tagList).orElse(new HashSet<>());
                            for (String tagName : byTagsRequest.getTagNameList()) {
                                if (!tagList.contains(tagName)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                .map(this::getSafetyUser)
                .collect(Collectors.toList());
        userPage.setRecords(newUserList);
        return userPage;
    }

    /**
     * 根据标签搜索用户 SQL查询
     *
     * @param byTagsRequest
     * @return
     */
    @Override
    public Page<User> queryUsersByTagsByMysql(SearchUserByTagsRequest byTagsRequest) {
        if (CollectionUtils.isEmpty(byTagsRequest.getTagNameList())) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        // 拼接and查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : byTagsRequest.getTagNameList()) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        Page<User> users = this.page(new Page<>(byTagsRequest.getPageNum(),byTagsRequest.getPageSize()),queryWrapper);

        return users;
    }

    @Override
    public int updateUser(UserDTO userDTO, User loginUser) {
        long userId = userDTO.getId();
        //如果是管理员，可以更新所有用户
        //如果不是管理员，只允许更新当前用户
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = this.getById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        if (StringUtils.isNotBlank(userDTO.getUsername())){
            oldUser.setUsername(userDTO.getUsername());
        }
        if (StringUtils.isNotBlank(userDTO.getEmail())){
            oldUser.setEmail(userDTO.getEmail());
        }
        if (StringUtils.isNotBlank(userDTO.getPhone())){
            oldUser.setPhone(userDTO.getPhone());
        }
        if (StringUtils.isNotBlank(userDTO.getAvatarUrl())){
            oldUser.setAvatarUrl(userDTO.getAvatarUrl());
        }
        return userMapper.updateById(oldUser);
    }

    @Override
    public boolean isAdmin(String userAccount, String uuid) {
        User cashUser = (User) redisTemplate.opsForHash().get(TOKEN_KEY + uuid, userAccount);
        return cashUser != null && cashUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }
}
