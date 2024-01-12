package com.yupi.yupaobackend.model.request;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @description:
 * @author: linli
 * @create: 2024-01-03 13:28
 **/
@Data
@ToString
public class DeleteFriendRequest extends CurrentUserRequest implements Serializable {

    private static final long serialVersionUID = -3001850738731102945L;

    private Long id;

    private Long deleteId;


}
