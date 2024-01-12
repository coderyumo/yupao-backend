package com.yupi.yupaobackend.service;

import com.yupi.yupaobackend.model.domain.Notice;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupaobackend.model.domain.User;

import java.util.List;

/**
* @author linli
* @description 针对表【notice(通知表)】的数据库操作Service
* @createDate 2024-01-03 16:08:33
*/
public interface NoticeService extends IService<Notice> {

    /**
     * 获取所有好友申请信息
     * @param id
     * @return
     */
    List<User> getNoticeData(Long id);

    Notice getBySenderIdAndRecipientId(Long senderId, Long recipientId);

}
