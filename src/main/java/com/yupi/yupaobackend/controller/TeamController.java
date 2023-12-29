package com.yupi.yupaobackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupaobackend.common.BaseResponse;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.common.ResultUtils;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.model.domain.Team;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.dto.AddTeamParam;
import com.yupi.yupaobackend.model.dto.TeamQuery;
import com.yupi.yupaobackend.model.request.TeamJoinRequest;
import com.yupi.yupaobackend.model.request.TeamUpdateRequest;
import com.yupi.yupaobackend.model.vo.TeamUserVO;
import com.yupi.yupaobackend.service.TeamService;
import com.yupi.yupaobackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description:
 * @author: linli
 * @create: 2023-12-29 14:34
 **/
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    UserService userService;

    @Resource
    TeamService teamService;


    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody AddTeamParam addTeamParam) {
        if (addTeamParam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(addTeamParam.getUserAccount(), addTeamParam.getUuid());
        long result = teamService.addTeam(addTeamParam);
        return ResultUtils.success(result);
    }


    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }


    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(teamUpdateRequest.getUserAccount(), teamUpdateRequest.getUuid());
        if (loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.updateTeam(teamUpdateRequest,loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改失败");
        }
        return ResultUtils.success(true);
    }


    @GetMapping("/getTeamById")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team result = teamService.getById(id);
        if (result == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(result);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> queryTeams(TeamQuery teamQuery) {
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<TeamUserVO> list = teamService.queryTeams(teamQuery);
        return ResultUtils.success(list);
    }



    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> queryTeamsPage(TeamQuery teamQuery) {
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        BeanUtils.copyProperties(teamQuery,team);
        Page<Team> teamPage = teamService.page(new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize()),teamQueryWrapper);
        return ResultUtils.success(teamPage);
    }


    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest) {
        if (teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.joinTeam(teamJoinRequest);
        return ResultUtils.success(result);
    }
}
