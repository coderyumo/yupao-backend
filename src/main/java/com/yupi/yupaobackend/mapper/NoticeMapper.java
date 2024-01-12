package com.yupi.yupaobackend.mapper;

import com.yupi.yupaobackend.model.domain.Notice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author linli
* @description 针对表【notice(通知表)】的数据库操作Mapper
* @createDate 2024-01-03 16:08:33
* @Entity com.yupi.yupaobackend.model.domain.Notice
*/
public interface NoticeMapper extends BaseMapper<Notice> {

    Notice getBySenderIdAndRecipientId(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);
}




