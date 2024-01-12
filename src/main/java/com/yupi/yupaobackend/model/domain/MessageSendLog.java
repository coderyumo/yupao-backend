package com.yupi.yupaobackend.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName message_send_log
 */
@TableName(value ="message_send_log")
@Data
public class MessageSendLog implements Serializable {
    /**
     * 消息id（uuid）
     */
    @TableId
    private String msgId;

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
     * 邀请人id
     */
    private Long inviterId;

    /**
     * 队列名字
     */
    private String routeKey;

    /**
     * 0-发送中 1-发送成功 2-发送失败
     */
    private Integer status;

    /**
     * 交换机名字
     */
    private String exchange;

    /**
     * 重试次数
     */
    private Integer tryCount;

    /**
     * 第一次重试时间
     */
    private Date tryTime;

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