package com.yupi.yupaobackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @description:
 * @author: linli
 * @create: 2024-01-03 13:28
 **/
@Data
public class AddFriendRequest implements Serializable {

    private static final long serialVersionUID = -3875902490135062967L;

    private Long senderId;

    private Long recipientId;

    private String userAccount;

    private String uuid;

}
