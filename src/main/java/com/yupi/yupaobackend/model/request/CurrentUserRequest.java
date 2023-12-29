package com.yupi.yupaobackend.model.request;

import lombok.Data;

@Data
public class CurrentUserRequest {
    private String userAccount;
    private String uuid;
}
