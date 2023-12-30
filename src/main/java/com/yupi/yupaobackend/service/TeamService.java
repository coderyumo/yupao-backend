package com.yupi.yupaobackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupaobackend.model.domain.Team;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.dto.AddTeamParam;
import com.yupi.yupaobackend.model.dto.TeamQuery;
import com.yupi.yupaobackend.model.request.TeamDisbandRequest;
import com.yupi.yupaobackend.model.request.TeamJoinRequest;
import com.yupi.yupaobackend.model.request.TeamQuitRequest;
import com.yupi.yupaobackend.model.request.TeamUpdateRequest;
import com.yupi.yupaobackend.model.vo.TeamUserVO;

import java.util.List;

/**
* @author linli
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-12-29 14:26:28
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     * @param addTeamParam
     * @return
     */
    long addTeam(AddTeamParam addTeamParam);

    /**
     * 搜搜队伍
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> queryTeams(TeamQuery teamQuery,Boolean isAdmin);

    /**
     * 修改队伍
     * @param teamUpdateRequest
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @return
     */
    Boolean joinTeam(TeamJoinRequest teamJoinRequest);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @return
     */
    Boolean quitTeam(TeamQuitRequest teamQuitRequest);

    /**
     * 队长解散队伍
     * @param teamDisbandRequest
     * @return
     */
    Boolean disbandTeam(TeamDisbandRequest teamDisbandRequest);
}
