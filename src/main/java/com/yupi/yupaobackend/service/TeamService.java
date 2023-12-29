package com.yupi.yupaobackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupaobackend.model.domain.Team;
import com.yupi.yupaobackend.model.dto.AddTeamParam;

/**
* @author linli
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-12-29 14:26:28
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team
     * @return
     */
    long addTeam(AddTeamParam addTeamParam);
}
