package com.yupi.yupaobackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
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
import com.yupi.yupaobackend.model.domain.Notice;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.dto.UserDTO;
import com.yupi.yupaobackend.model.enums.AddFriendStatusEnum;
import com.yupi.yupaobackend.model.request.*;
import com.yupi.yupaobackend.service.NoticeService;
import com.yupi.yupaobackend.service.UserService;
import com.yupi.yupaobackend.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.yupaobackend.constant.RedisConstant.*;
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
    private RedisTemplate redisTemplate;

    @Resource
    private UserMapper userMapper;

    @Resource
    @Lazy
    private NoticeService noticeService;

    @Resource
    private RedissonClient redissonClient;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 用户注册
     *
     * @param registerRequest 用户注册
     * @return 新用户 id
     */
    @Override
    public long userRegister(UserRegisterRequest registerRequest) {
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();
        String planetCode = registerRequest.getPlanetCode();
        String activeIds = registerRequest.getTags();
        String avatarUrl = registerRequest.getAvatarUrl();
        String username = registerRequest.getUsername();
        String phone = registerRequest.getPhone();
        String email = registerRequest.getEmail();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode, activeIds, avatarUrl, username,
                phone, email
        )) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode, activeIds, avatarUrl, username)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号至少四位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码至少八位");
        }

        if (planetCode.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能包含特殊字符");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码不相同");
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
        BeanUtils.copyProperties(registerRequest, user);
        user.setUserPassword(encryptPassword);
        System.out.println("user = " + user);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
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
            redisTemplate.expire(TOKEN_KEY + uuid, 10, TimeUnit.MINUTES);
            return currentToken;
        }
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "user login failed, userAccount cannot match " +
                    "userPassword");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        String newUuid = UUID.randomUUID().toString().replace("-", "");
        log.info("uuid ==================> {}",  uuid);
        String token = userAccount + "-" + newUuid; // 1. 校验
        // 4. 存储用户信息到Redis中,设置key过期时间和token过期时间
        redisTemplate.opsForHash().put(TOKEN_KEY + newUuid, safetyUser.getUserAccount(), safetyUser);
        redisTemplate.expire(TOKEN_KEY + newUuid, 10, TimeUnit.MINUTES);
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
        safetyUser.setFriendId(originUser.getFriendId());
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
        for (String tagName : byTagsRequest.getTagNameList()) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        Page<User> userPage = this.page(new Page<>(byTagsRequest.getPageNum(), byTagsRequest.getPageSize()), queryWrapper);
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
        Page<User> users = this.page(new Page<>(byTagsRequest.getPageNum(), byTagsRequest.getPageSize()), queryWrapper);

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
        if (StringUtils.isNotBlank(userDTO.getUsername())) {
            oldUser.setUsername(userDTO.getUsername());
        }
        if (StringUtils.isNotBlank(userDTO.getEmail())) {
            oldUser.setEmail(userDTO.getEmail());
        }
        if (userDTO.getGender() != null) {
            oldUser.setGender(userDTO.getGender());
        }
        if (StringUtils.isNotBlank(userDTO.getPhone())) {
            oldUser.setPhone(userDTO.getPhone());
        }
        if (StringUtils.isNotBlank(userDTO.getAvatarUrl())) {
            oldUser.setAvatarUrl(userDTO.getAvatarUrl());
        }
        if (StringUtils.isNotBlank(userDTO.getTags())) {
            oldUser.setTags(userDTO.getTags());
        }
        if (StringUtils.isNotBlank(userDTO.getAvatarUrl())) {
            oldUser.setAvatarUrl(userDTO.getAvatarUrl());
        }
        if (StringUtils.isNotBlank(userDTO.getProfile())) {
            oldUser.setProfile(userDTO.getProfile());
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

    @Override
    public List<User> matchUser(int num, User loginUser) {
        //有缓存直接读缓存
        String key = USER_MATCH_KEY + loginUser.getId();
        List<User> cacheUserList = (List<User>) redisTemplate.opsForValue().get(key);
        if (CollectionUtils.isNotEmpty(cacheUserList)) {
            return cacheUserList;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 无标签或者为当前用户自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 原本顺序的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }

        //查出来最匹配的用户，进行存储，并设置过期时间
        //写缓存
        try {
            redisTemplate.opsForValue().set(key, finalUserList, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error");
        }
        return finalUserList;

    }

    @Override
    public Boolean addFriend(AddFriendRequest addFriendRequest) {
        if (addFriendRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        RLock lock = redissonClient.getLock(ADD_FRIEND_KEY);
        try {

            //只有一个线程会获取锁
            //判断发送人id和接收人id是否存在
            User sender = this.getById(addFriendRequest.getSenderId());
            User recipient = this.getById(addFriendRequest.getRecipientId());
            if (sender != null && recipient != null) {
                Long recipientId = addFriendRequest.getRecipientId();
                Long senderId = addFriendRequest.getSenderId();
                //校验是否已经是好友
                Boolean alreadyFriends = checkIfAlreadyFriends(sender, recipientId);
                Boolean alreadyFriends1 = checkIfAlreadyFriends(recipient, senderId);
                if (alreadyFriends && alreadyFriends1) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方已经是您的好友，请勿重新发送");
                }

                //判断是否已经发送好友申请，如果状态为添加失败则可以继续添加
                QueryWrapper<Notice> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().eq(Notice::getSenderId, senderId).eq(Notice::getRecipientId,
                        recipientId);
                List<Notice> list = noticeService.list(queryWrapper);
                for (Notice notice : list) {
                    //正在发送好友申请
                    if (notice.getAddFriendStatus().equals(AddFriendStatusEnum.ADDING.getValue())) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "正在发送好友申请，请勿重新发送!");
                    }
                    //添加成功，对方是你的好友
                    if (notice.getAddFriendStatus().equals(AddFriendStatusEnum.ADD_SUCCESS.getValue())) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方已经是您的好友，请勿重新添加!");
                    }
                }
                // 保存到消息通知表中
                Notice notice = new Notice();
                notice.setSenderId(senderId);
                notice.setRecipientId(recipientId);
                notice.setAddFriendStatus(AddFriendStatusEnum.ADDING.getValue());
                boolean save = noticeService.save(notice);
                if (!save) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "保存消息通知表失败!");
                }
                return true;
            }
        } finally {
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return true;
    }

    @Override
    public Boolean increaseFriend(AddFriendRequest addFriendRequest) {
        if (addFriendRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long recipientId = addFriendRequest.getRecipientId();
        Long senderId = addFriendRequest.getSenderId();
        Notice notice = new Notice();
        notice.setSenderId(senderId);
        notice.setRecipientId(recipientId);
        boolean save = noticeService.save(notice);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "好友申请失败");
        }
        return true;
    }

    @Override
    public List<User> getAddFriendNotice(Long id) {
        QueryWrapper<Notice> qw = new QueryWrapper<>();
        qw.lambda().eq(Notice::getRecipientId, id);
        List<Notice> list = noticeService.list(qw);
        ArrayList<Long> applyUserId = new ArrayList<>();
        for (Notice notice : list) {
            applyUserId.add(notice.getSenderId());
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(User::getId, applyUserId);
        List<User> applyUserList = this.list(queryWrapper);
        return applyUserList;
    }

    /**
     * 校验是否是好友
     *
     * @param user
     * @param friendId
     */
    private Boolean checkIfAlreadyFriends(User user, Long friendId) {
        String userFriendId = user.getFriendId();
        if (StrUtil.isNotBlank(userFriendId)) {
            JSONArray jsonArray = JSONUtil.parseArray(userFriendId);
            // 将friendId转为Integer类型进行比较
            return jsonArray.contains(Math.toIntExact(friendId));
        }
        return false;
    }

    /**
     * 添加好友到好友列表
     *
     * @param user
     * @param friendId
     */
    private void addFriendList(User user, Long friendId) {
        JSONArray friendIdJsonArray = StrUtil.isBlank(user.getFriendId()) ? new JSONArray() : JSONUtil.parseArray(user.getFriendId());
        friendIdJsonArray.add(friendId);
        user.setFriendId(JSONUtil.toJsonStr(friendIdJsonArray));
        boolean updateResult = this.updateById(user);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新好友列表失败");
        }
    }

    /**
     * 删除好友
     *
     * @param user
     * @param friendId
     */
    private void removeFriendFromList(User user, Long friendId) {
        if (StrUtil.isNotBlank(user.getFriendId())) {
            JSONArray friendIdJsonArray = JSONUtil.parseArray(user.getFriendId());
            JSONArray newFriendIdJsonArray = new JSONArray();
            for (Object id : friendIdJsonArray) {
                if (!id.equals(Math.toIntExact(friendId))) {
                    newFriendIdJsonArray.add(id);
                }
            }
            user.setFriendId(JSONUtil.toJsonStr(newFriendIdJsonArray));
            boolean updateResult = this.updateById(user);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除好友ID失败");
            }
        }
    }


    @Override
    @Transactional
    public Boolean agreeFriend(AddFriendRequest addFriendRequest) {
        Long senderId = addFriendRequest.getSenderId();
        Long recipientId = addFriendRequest.getRecipientId();
        User sender = this.getById(senderId);
        User recipient = this.getById(recipientId);

        // 再次校验是否已经是好友
        Boolean alreadyFriends = checkIfAlreadyFriends(sender, recipientId);
        Boolean alreadyFriends1 = checkIfAlreadyFriends(recipient, senderId);

        if (alreadyFriends && alreadyFriends1) {
            return true;
        }

        // 修改消息通知表
        QueryWrapper<Notice> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Notice::getSenderId, senderId).eq(Notice::getRecipientId, recipientId).eq(Notice::getAddFriendStatus, AddFriendStatusEnum.ADDING.getValue());
        Notice notice1 = noticeService.getOne(queryWrapper);
        notice1.setAddFriendStatus(AddFriendStatusEnum.ADD_SUCCESS.getValue());
        boolean updateNotice1 = noticeService.updateById(notice1);
        if (!updateNotice1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改通知表失败");
        }


        QueryWrapper<Notice> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.lambda().eq(Notice::getRecipientId, senderId).eq(Notice::getSenderId, recipientId).eq(Notice::getAddFriendStatus, AddFriendStatusEnum.ADDING.getValue());
        Notice notice2 = noticeService.getOne(queryWrapper2);
        if (notice2 != null){
            notice2.setAddFriendStatus(AddFriendStatusEnum.ADD_SUCCESS.getValue());
            boolean updateNotice2 = noticeService.updateById(notice2);
            if (!updateNotice2) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改通知表失败");
            }
        }

        // 在发送人好友列表添加上对方的id
        addFriendList(sender, recipientId);
        // 在接收人好友列表添加上对方的id
        addFriendList(recipient, senderId);

        return true;
    }


    @Override
    public Boolean rejectFriend(AddFriendRequest addFriendRequest) {
        Long senderId = addFriendRequest.getSenderId();
        Long recipientId = addFriendRequest.getRecipientId();

        // 修改消息通知表
        QueryWrapper<Notice> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Notice::getSenderId, senderId).eq(Notice::getRecipientId, recipientId).eq(Notice::getAddFriendStatus, AddFriendStatusEnum.ADDING.getValue());
        Notice notice = noticeService.getOne(queryWrapper);
        notice.setAddFriendStatus(AddFriendStatusEnum.ADD_ERROR.getValue());
        boolean updateNotice = noticeService.updateById(notice);
        if (!updateNotice) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改通知表失败");
        }
        return true;
    }

    @Override
    public List<User> listFriend(User loginUser) {
        // 最新数据
        User user = this.getById(loginUser.getId());
        String friendId = user.getFriendId();

        ArrayList<User> userArrayList = new ArrayList<>();
        if (StrUtil.isBlank(friendId)) {
            return userArrayList;
        }

        JSONArray jsonArray = JSONUtil.parseArray(friendId);
        for (Object id : jsonArray) {
            userArrayList.add(this.getById((Serializable) id));
        }

        return userArrayList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFriend(DeleteFriendRequest deleteFriendRequest) {
        Long senderId = deleteFriendRequest.getId();
        Long recipientId = deleteFriendRequest.getDeleteId();
        String userAccount = deleteFriendRequest.getUserAccount();
        String uuid = deleteFriendRequest.getUuid();
        User sender = this.getById(senderId);
        User recipient = this.getById(recipientId);


        //删除好友，修改消息通知表的好友状态为添加失败
        QueryWrapper<Notice> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Notice::getRecipientId, senderId).eq(Notice::getSenderId, recipientId).eq(Notice::getAddFriendStatus, AddFriendStatusEnum.ADD_SUCCESS.getValue());
        Notice notice = noticeService.getOne(queryWrapper);

        if (notice==null){
            QueryWrapper<Notice> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.lambda().eq(Notice::getSenderId, senderId).eq(Notice::getRecipientId, recipientId).eq(Notice::getAddFriendStatus, AddFriendStatusEnum.ADD_SUCCESS.getValue());
            notice = noticeService.getOne(queryWrapper2);
        }

        notice.setAddFriendStatus(AddFriendStatusEnum.ADD_ERROR.getValue());
        noticeService.updateById(notice);

        //双方好友列表id都删除对方
        removeFriendFromList(sender, recipientId);
        removeFriendFromList(recipient, senderId);

        return true;
    }


    @Override
    public Boolean refreshCache(CurrentUserRequest currentUserRequest) {
        String userAccount = currentUserRequest.getUserAccount();
        String uuid = currentUserRequest.getUuid();
        User cashUser = (User) redisTemplate.opsForHash().get(TOKEN_KEY + uuid, userAccount);
        // 先删除缓存
        try {
            redisTemplate.opsForHash().delete(TOKEN_KEY + uuid, userAccount);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除缓存失败");
        }
        User user = getById(cashUser.getId());
        User safetyUser = this.getSafetyUser(user);

        redisTemplate.opsForHash().put(TOKEN_KEY + uuid, userAccount, safetyUser);
        redisTemplate.expire(TOKEN_KEY + uuid, 10, TimeUnit.MINUTES);
        return true;
    }
}
