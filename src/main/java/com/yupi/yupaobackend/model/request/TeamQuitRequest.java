package com.yupi.yupaobackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 退出队伍参数请求体
 * @author: linli
 * @create: 2023-12-30 12:28
 **/
@Data
public class TeamQuitRequest extends CurrentUserRequest implements Serializable {


    private static final long serialVersionUID = 7569130285459673878L;


    /**
     * 队伍id
     */
    private Long teamId;

}
