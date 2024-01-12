package com.yupi.yupaobackend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知表
 * @TableName notice
 */
@TableName(value ="notice")
@Data
public class Notice implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送人id
     */
    private Long senderId;

    /**
     * 接收人id
     */
    private Long recipientId;

    /**
     * 添加好友状态
     */
    private Integer addFriendStatus;

    /**
     * 邀请人Id
     */
    private Long inviterId;

    /**
     * 被邀请人Id
     */
    private Long inviteeId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}