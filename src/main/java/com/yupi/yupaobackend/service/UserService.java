package com.yupi.yupaobackend.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.dto.UserDTO;
import com.yupi.yupaobackend.model.request.AddFriendRequest;
import com.yupi.yupaobackend.model.request.SearchUserByTagsRequest;
import com.yupi.yupaobackend.model.request.UserRegisterRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author linli
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2023-12-15 22:57:04
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userRegisterRequest   用户注册请求体
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    String userLogin(String userAccount, String userPassword, String uuid);

    /**
     * 获取当前登录用户信息
     *
     * @param userAccount
     * @param uuid
     * @return
     */
    User getLoginUser(String userAccount, String uuid);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagList
     * @return
     */
    Page<User> queryUsersByTags(SearchUserByTagsRequest byTagsRequest);

    Page<User> queryUsersByTagsByMysql(SearchUserByTagsRequest byTagsRequest);

    /**
   * 修改用户信息
   *
   * @param userDTO
   * @param LoginUser
   * @return
   */
  int updateUser(UserDTO userDTO, User LoginUser);

  /**
   * 是否为管理员
   *
   * @param userAccount
   * @param uuid
   * @return
   */
  boolean isAdmin(String userAccount, String uuid);

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    List<User> matchUser(int num, User loginUser);

    /**
     * 添加好友
     * @param addFriendRequest
     * @return
     */
    Boolean addFriend(AddFriendRequest addFriendRequest);

    /**
     * 添加好友
     * @param addFriendRequest
     * @return
     */
    Boolean increaseFriend(AddFriendRequest addFriendRequest);

    /**
     * 通知
     * @param id
     * @return
     */
    List<User> getAddFriendNotice(Long id);

    Boolean agreeFriend(AddFriendRequest addFriendRequest);

}
