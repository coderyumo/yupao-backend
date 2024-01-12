package com.yupi.yupaobackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupaobackend.model.domain.MessageSendLog;
import com.yupi.yupaobackend.service.MessageSendLogService;
import com.yupi.yupaobackend.mapper.MassageSendLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author linli
* @description 针对表【massage_send_log】的数据库操作Service实现
* @createDate 2024-01-03 13:04:37
*/
@Service
public class MessageSendLogServiceImpl extends ServiceImpl<MassageSendLogMapper, MessageSendLog>
    implements MessageSendLogService {

    @Resource
    MassageSendLogMapper massageSendLogMapper;

    @Override
    public MessageSendLog getBySenderIdAndRecipientId(Long senderId, Long recipientId) {

        return massageSendLogMapper.getBySenderIdAndRecipientId(senderId,recipientId);
    }

}




