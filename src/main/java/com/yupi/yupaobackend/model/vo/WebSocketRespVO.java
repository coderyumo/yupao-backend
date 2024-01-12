package com.yupi.yupaobackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @description:
 * @author: yumo
 * @create: 2024-01-12 16:50
 **/
@Data
public class WebSocketRespVO implements Serializable {


    private static final long serialVersionUID = 563709915200844227L;

    private String message;

    private Long senderId;

    private Boolean isAgree;
}
