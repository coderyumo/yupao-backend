package com.yupi.yupaobackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @description:
 * @author: linli
 * @create: 2023-12-29 23:10
 **/
@Data
public class TeamJoinRequest extends CurrentUserRequest implements Serializable {

    private static final long serialVersionUID = 2478772835320724204L;

    private Long teamId;

    private String password;

}
