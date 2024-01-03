package com.yupi.yupaobackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupaobackend.model.domain.Notice;
import com.yupi.yupaobackend.service.NoticeService;
import com.yupi.yupaobackend.mapper.NoticeMapper;
import org.springframework.stereotype.Service;

/**
* @author linli
* @description 针对表【notice(通知表)】的数据库操作Service实现
* @createDate 2024-01-03 16:08:33
*/
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
    implements NoticeService{

}




