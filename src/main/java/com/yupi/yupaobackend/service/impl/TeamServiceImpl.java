package com.yupi.yupaobackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.mapper.TeamMapper;
import com.yupi.yupaobackend.mapper.UserMapper;
import com.yupi.yupaobackend.model.domain.Team;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.domain.UserTeam;
import com.yupi.yupaobackend.model.dto.AddTeamParam;
import com.yupi.yupaobackend.model.dto.TeamQuery;
import com.yupi.yupaobackend.model.enums.TeamStatusEnum;
import com.yupi.yupaobackend.model.request.TeamJoinRequest;
import com.yupi.yupaobackend.model.request.TeamUpdateRequest;
import com.yupi.yupaobackend.model.vo.TeamUserVO;
import com.yupi.yupaobackend.service.TeamService;
import com.yupi.yupaobackend.service.UserService;
import com.yupi.yupaobackend.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author linli
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-12-29 14:26:28
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    UserService userService;

    @Resource
    UserTeamService userTeamService;

    @Resource
    UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(AddTeamParam addTeamParam) {
        //1.请求参数是否为空
        if (addTeamParam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //2.是否登录
        User loginUser = userService.getLoginUser(addTeamParam.getUserAccount(), addTeamParam.getUuid());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        //3.校验信息
        int MaxNum = Optional.ofNullable(addTeamParam.getMaxNum()).orElse(0);
        if (MaxNum < 1 || MaxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }

        String name = addTeamParam.getName();
        if (name.length() > 20 || StringUtils.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题过长或者标题为空");
        }

        String description = addTeamParam.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过程过长");
        }

        int status = Optional.ofNullable(addTeamParam.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }

        String password = addTeamParam.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if ((StringUtils.isBlank(password) || password.length() > 32)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式错误");
            }
        }

        Date expireTime = addTeamParam.getExpireTime();
        if (expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间不能小于当前时间");
        }

        Team team = new Team();

        BeanUtils.copyProperties(addTeamParam, team);
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Team::getUserId, userId);
        long count = this.count(queryWrapper);
        if (count == 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多只能创建五个队伍");
        }

        //存入数据库
        team.setId(null);
        team.setUserId(loginUser.getId());
        boolean result = this.save(team);
        if (!result || team.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

        //存入用户-队伍关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(team.getId());
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return team.getId();
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍数据不存在");
        }
        //不是管理员也不是创建者
        boolean isAdmin = userService.isAdmin(loginUser);
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        TeamStatusEnum oldStatusEum = TeamStatusEnum.getEnumByValue(oldTeam.getStatus());
        //查询查询旧队伍状态
        //旧状态不为加密才走判断

        if (!oldStatusEum.equals(TeamStatusEnum.SECRET)) {
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
            if (statusEnum.equals(TeamStatusEnum.SECRET)) {
                if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须要有密码");
                }
            }
        }

        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    public Boolean joinTeam(TeamJoinRequest teamJoinRequest) {
        User loginUser = userService.getLoginUser(teamJoinRequest.getUserAccount(), teamJoinRequest.getUuid());
        Long userId = loginUser.getId();

        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有的队伍");
        }

        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            if (StringUtils.isBlank(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须要有密码才能加入");
            }
        }

        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            if (StringUtils.isBlank(password) || !team.getPassword().equals(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不匹配");
            }
        }

        //该用户已加入队伍的数量
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserTeam::getUserId, userId);
        List<UserTeam> hasJoinTeams = userTeamService.list(queryWrapper);
        if (hasJoinTeams.size() == 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入五个队伍");
        }

        //已加入队伍的成员
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.lambda().eq(UserTeam::getTeamId, teamId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        if (userTeamList.size() == team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能加入未满的队伍");
        }

        //不能重复加入已加入的队伍
        //已加入队伍的id
        ArrayList<Long> hasJoinTeamId = new ArrayList<>();
        hasJoinTeams.forEach(t->{
            hasJoinTeamId.add(t.getTeamId());
        });
        if (hasJoinTeamId.contains(teamId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入");
        }

        //修改队伍信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());

        return userTeamService.save(userTeam);
    }

    @Override
    public List<TeamUserVO> queryTeams(TeamQuery teamQuery) {
        //1. 从请求参数中取出队伍名称等查询条件，如果存在则作为查询条件
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //当前登录用户
        User loginUser = userService.getLoginUser(teamQuery.getUserAccount(), teamQuery.getUuid());
        boolean isAdmin = userService.isAdmin(loginUser);

        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(teamQuery.getId() != null && teamQuery.getId() > 0, Team::getId, teamQuery.getId())
                .like(StringUtils.isNotBlank(teamQuery.getName()), Team::getName, teamQuery.getName())
                .like(StringUtils.isNotBlank(teamQuery.getDescription()), Team::getDescription, teamQuery.getDescription())
                .eq(teamQuery.getMaxNum() != null && teamQuery.getMaxNum() <= 10, Team::getId, teamQuery.getMaxNum())
                .eq(teamQuery.getUserId() != null && teamQuery.getMaxNum() > 0, Team::getUserId, teamQuery.getUserId());

        if (StringUtils.isNotBlank(teamQuery.getSearchTest())) {
            queryWrapper.lambda().and(qw -> qw.like(Team::getName, teamQuery.getName()).or().like(Team::getDescription
                    , teamQuery.getDescription()));
        }

        queryWrapper.lambda().and(qw -> qw.gt(Team::getExpireTime, new Date()).or().isNull(Team::getExpireTime));

        Integer status = teamQuery.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        if (!isAdmin && statusEnum.equals(TeamStatusEnum.PUBLIC)) {
            queryWrapper.lambda().eq(Team::getStatus, teamQuery.getStatus());
        }

        List<Team> teamList = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> respTeamUserVO = new ArrayList<>();
        //关联查询用户信息
        for (Team team : teamList) {
            if (team.getExpireTime().after(new Date()) || team.getExpireTime() == null) {
                TeamUserVO teamUserVO = new TeamUserVO();
                BeanUtils.copyProperties(team, teamUserVO);
                ArrayList<User> userList = new ArrayList<>();
                Long teamId = team.getId();
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.lambda().eq(UserTeam::getTeamId, teamId);
                List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
                for (UserTeam userTeam : list) {
                    User user = userService.getById(userTeam.getUserId());
                    User safetyUser = userService.getSafetyUser(user);
                    userList.add(safetyUser);

                    teamUserVO.setUserList(userList);
                }
                User userById = userService.getById(team.getUserId());
                teamUserVO.setCreateUsername(userById.getUsername());
                respTeamUserVO.add(teamUserVO);
            }

        }
        return respTeamUserVO;
    }
}




