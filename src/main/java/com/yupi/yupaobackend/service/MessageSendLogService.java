package com.yupi.yupaobackend.service;

import com.yupi.yupaobackend.model.domain.MessageSendLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author linli
* @description 针对表【massage_send_log】的数据库操作Service
* @createDate 2024-01-03 13:04:37
*/
public interface MessageSendLogService extends IService<MessageSendLog> {

    MessageSendLog getBySenderIdAndRecipientId(Long senderId, Long recipientId);
}
