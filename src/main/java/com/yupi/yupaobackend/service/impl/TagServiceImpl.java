package com.yupi.yupaobackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupaobackend.model.domain.Tag;
import com.yupi.yupaobackend.service.TagService;
import com.yupi.yupaobackend.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author linli
* @description 针对表【tag(标签表)】的数据库操作Service实现
* @createDate 2023-12-25 13:55:10
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




