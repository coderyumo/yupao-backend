package com.yupi.yupaobackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.vo.TeamUserVO;

import java.util.List;

/**
* @author linli
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2023-12-15 22:57:04
* @Entity com.yupi.usercenterbackend.model.User
*/
public interface UserMapper extends BaseMapper<User> {

    List<User> searchAddCount();
}




