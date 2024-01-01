package com.yupi.yupaobackend.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yupi.yupaobackend.model.domain.User;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @description: 队伍和用户信息封装类
 * @author: linli
 * @create: 2023-12-29 18:21
 **/
@Data
public class TeamUserVO implements Serializable {

    private static final long serialVersionUID = 4178638630610008479L;

    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 入队成员id
     */
    private List<Long> memberId;

    /**
     * 当前用户是否已加入队伍
     */
    private Boolean isJoin;

    /**
     * 创建人姓名
     */
    private String createUsername;

    /**
     * 创建人姓名
     */
    private String createAvatarUrl;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 修改时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 入队用户列表
     */
    private List<User> userList;

}
