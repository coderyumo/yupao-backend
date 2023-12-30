package com.yupi.yupaobackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @description:
 * @author: linli
 * @create: 2023-12-30 14:30
 **/
@Data
public class TeamDisbandRequest extends CurrentUserRequest implements Serializable {


    private static final long serialVersionUID = -3198527091185225741L;
    /**
     * 队伍id
     */
    private Long teamId;
}
