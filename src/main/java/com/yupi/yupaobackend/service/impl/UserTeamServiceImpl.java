package com.yupi.yupaobackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupaobackend.mapper.UserTeamMapper;
import com.yupi.yupaobackend.model.domain.UserTeam;
import com.yupi.yupaobackend.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author linli
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2023-12-29 14:27:52
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




