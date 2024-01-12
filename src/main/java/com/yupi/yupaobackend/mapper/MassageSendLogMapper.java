package com.yupi.yupaobackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.yupaobackend.model.domain.MessageSendLog;
import org.apache.ibatis.annotations.Param;

/**
* @author linli
* @description 针对表【massage_send_log】的数据库操作Mapper
* @createDate 2024-01-03 13:04:37
* @Entity com.yupi.yupaobackend.model.domain.MassageSendLog
*/
public interface MassageSendLogMapper extends BaseMapper<MessageSendLog> {

    MessageSendLog getBySenderIdAndRecipientId(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);
}




