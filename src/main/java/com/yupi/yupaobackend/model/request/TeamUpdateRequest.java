package com.yupi.yupaobackend.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @description:
 * @author: linli
 * @create: 2023-12-29 22:31
 **/
@Data
public class TeamUpdateRequest extends CurrentUserRequest implements Serializable {


    private static final long serialVersionUID = -5918582574241321447L;

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
     * 密码
     */
    private String password;

    /**
     * 过期时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;


}
