package com.yupi.yupaobackend.model.dto;

import com.yupi.yupaobackend.common.PageRequest;
import lombok.Data;

/**
 * @description: 队伍查询参数
 * @author: linli
 * @create: 2023-12-29 14:52
 **/
@Data
public class TeamQuery extends PageRequest {

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
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

}
