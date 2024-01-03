package com.yupi.yupaobackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupaobackend.common.BaseResponse;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.common.ResultUtils;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.model.domain.Team;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.domain.UserTeam;
import com.yupi.yupaobackend.model.request.AddTeamRequest;
import com.yupi.yupaobackend.model.request.TeamQueryRequest;
import com.yupi.yupaobackend.model.request.TeamDisbandRequest;
import com.yupi.yupaobackend.model.request.TeamJoinRequest;
import com.yupi.yupaobackend.model.request.TeamQuitRequest;
import com.yupi.yupaobackend.model.request.TeamUpdateRequest;
import com.yupi.yupaobackend.model.vo.TeamUserVO;
import com.yupi.yupaobackend.service.TeamService;
import com.yupi.yupaobackend.service.UserService;
import com.yupi.yupaobackend.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: linli
 * @create: 2023-12-29 14:34
 **/
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://user.code-li.fun","http://yupao.code-li.fun"})
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;


    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody AddTeamRequest addTeamRequest) {
        if (addTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(addTeamRequest.getUserAccount(), addTeamRequest.getUuid());
        long result = teamService.addTeam(addTeamRequest);
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
    public BaseResponse<List<TeamUserVO>> queryTeamsList(TeamQueryRequest teamQueryRequest) {
        User loginUser = userService.getLoginUser(teamQueryRequest.getUserAccount(), teamQueryRequest.getUuid());
        boolean isAdmin = userService.isAdmin(loginUser);
        List<TeamUserVO> list = teamService.queryTeams(teamQueryRequest,isAdmin);
        //当前用户是否已加入队伍
        return ResultUtils.success(list);
    }



    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> queryTeamsPage(TeamQueryRequest teamQueryRequest) {
        if (teamQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        BeanUtils.copyProperties(teamQueryRequest,team);
        Page<Team> teamPage = teamService.page(new Page<>(teamQueryRequest.getPageNum(), teamQueryRequest.getPageSize()),teamQueryWrapper);
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

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest) {
        if (teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.quitTeam(teamQuitRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/disband")
    public BaseResponse<Boolean>  disbandTeam(@RequestBody TeamDisbandRequest teamDisbandRequest) {
        if (teamDisbandRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.disbandTeam(teamDisbandRequest);
        return ResultUtils.success(result);
    }

    /**
     * 获取我创建的队伍
     * @param teamQueryRequest
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> myCreateTeamsList(TeamQueryRequest teamQueryRequest) {
        User loginUser = userService.getLoginUser(teamQueryRequest.getUserAccount(), teamQueryRequest.getUuid());
        teamQueryRequest.setUserId(loginUser.getId());
        List<TeamUserVO> list = teamService.queryTeams(teamQueryRequest,true);
        return ResultUtils.success(list);
    }

    /**
     * 获取我加入的队伍
     * @param teamQueryRequest
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> myJoinTeamsList(TeamQueryRequest teamQueryRequest) {
        //获取登录用户
        User loginUser = userService.getLoginUser(teamQueryRequest.getUserAccount(), teamQueryRequest.getUuid());
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserTeam::getUserId,userId);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        //去重
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        ArrayList<Long> idList = new ArrayList<>(listMap.keySet());
        if (CollectionUtils.isEmpty(idList)){
            return ResultUtils.success(null);
        }
        teamQueryRequest.setIdList(idList);
        List<TeamUserVO> list = teamService.queryTeams(teamQueryRequest,true);
        return ResultUtils.success(list);
    }
}
