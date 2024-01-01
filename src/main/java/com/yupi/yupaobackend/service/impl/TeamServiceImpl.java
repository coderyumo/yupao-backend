package com.yupi.yupaobackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.mapper.TeamMapper;
import com.yupi.yupaobackend.model.domain.Team;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.domain.UserTeam;
import com.yupi.yupaobackend.model.dto.AddTeamParam;
import com.yupi.yupaobackend.model.dto.TeamQuery;
import com.yupi.yupaobackend.model.enums.TeamStatusEnum;
import com.yupi.yupaobackend.model.request.TeamDisbandRequest;
import com.yupi.yupaobackend.model.request.TeamJoinRequest;
import com.yupi.yupaobackend.model.request.TeamQuitRequest;
import com.yupi.yupaobackend.model.request.TeamUpdateRequest;
import com.yupi.yupaobackend.model.vo.TeamUserVO;
import com.yupi.yupaobackend.service.TeamService;
import com.yupi.yupaobackend.service.UserService;
import com.yupi.yupaobackend.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    RedissonClient redissonClient;

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
        if (expireTime != null && expireTime.before(new Date())) {
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
        Team oldTeam = getTeamById(id);
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


        Long teamId = teamJoinRequest.getTeamId();

        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getTeamById(teamId);

        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有的队伍");
        }

        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须要有密码才能加入");
            }
        }

        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !team.getPassword().equals(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不匹配");
            }
        }
        long userId = loginUser.getId();
        //分布式锁
        RLock lock = redissonClient.getLock("yupao:join_team:lock");
        try {
            //只有一个线程会获取锁
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                    queryWrapper.lambda().eq(UserTeam::getUserId, userId);
                    List<UserTeam> hasJoinTeams = userTeamService.list(queryWrapper);
                    if (hasJoinTeams.size() == 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入五个队伍");
                    }
                    //已加入队伍的成员
                    List<UserTeam> userTeams = this.hasJoinTeamUser(teamId);
                    if (userTeams.size() == team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能加入未满的队伍");
                    }

                    //不能重复加入已加入的队伍
                    //已加入队伍的id
                    ArrayList<Long> hasJoinTeamId = new ArrayList<>();
                    hasJoinTeams.forEach(t -> {
                        hasJoinTeamId.add(t.getTeamId());
                    });
                    if (hasJoinTeamId.contains(teamId)) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入");
                    }

                    //修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("redis set key error");
        } finally {
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest) {

        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //当前用户
        String userAccount = teamQuitRequest.getUserAccount();
        String uuid = teamQuitRequest.getUuid();
        User loginUser = userService.getLoginUser(userAccount, uuid);

        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);

        //查询是否加入队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        Long userId = loginUser.getId();
        queryWrapper.lambda().eq(UserTeam::getTeamId, teamId).eq(UserTeam::getUserId, userId);
        long count = userTeamService.count(queryWrapper);
        //没加入队伍直接抛异常
        if (count == 0) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "未加入队伍");
        }

        List<UserTeam> userTeams = this.hasJoinTeamUser(teamId);
        //队伍只剩下一人，删除队伍关系，删除队伍
        if (userTeams.size() == 1) {
            this.removeById(teamId);
        } else {
            //队伍人数大于一人
            //如果是队长退出则将队伍创建人转移给第二加入的成员
            if (Objects.equals(team.getUserId(), userId)) {
                Team tempTeam = new Team();
                tempTeam.setId(teamId);
                userTeams.sort(Comparator.comparing(UserTeam::getJoinTime));
                tempTeam.setUserId(userTeams.get(1).getUserId());
                //更新队伍队长
                boolean result = this.updateById(tempTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队长失败");
                }
            }
            //如果不是队长直接退出队伍，删除用户-队伍关系
        }
        return userTeamService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disbandTeam(TeamDisbandRequest teamDisbandRequest) {

        if (teamDisbandRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //校验队伍是否存在
        Long teamId = teamDisbandRequest.getTeamId();
        Team team = getTeamById(teamId);

        //当前用户
        String userAccount = teamDisbandRequest.getUserAccount();
        String uuid = teamDisbandRequest.getUuid();
        User loginUser = userService.getLoginUser(userAccount, uuid);

        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "禁止访问");
        }

        //移除所有加入队伍的关系
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserTeam::getTeamId, teamId);
        boolean remove = userTeamService.remove(queryWrapper);
        if (!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }

        return this.removeById(teamId);
    }

    /**
     * 根据id查询队伍
     *
     * @param teamId 队伍id
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍当前人数
     *
     * @param teamId
     * @return
     */
    private List<UserTeam> hasJoinTeamUser(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.lambda().eq(UserTeam::getTeamId, teamId);
        return userTeamService.list(userTeamQueryWrapper);
    }

    @Override
    public List<TeamUserVO> queryTeams(TeamQuery teamQuery, Boolean isAdmin) {
        //1. 从请求参数中取出队伍名称等查询条件，如果存在则作为查询条件
        //当前登录用户
        User loginUser = userService.getLoginUser(teamQuery.getUserAccount(), teamQuery.getUuid());

        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(teamQuery.getId() != null && teamQuery.getId() > 0, Team::getId, teamQuery.getId())
                .in(!CollectionUtils.isEmpty(teamQuery.getIdList()), Team::getId, teamQuery.getIdList())
                .like(StringUtils.isNotBlank(teamQuery.getName()), Team::getName, teamQuery.getName())
                .like(StringUtils.isNotBlank(teamQuery.getDescription()), Team::getDescription, teamQuery.getDescription())
                .apply(teamQuery.getMaxNum() != null && teamQuery.getMaxNum() <= 10, "max_num <= {0}", teamQuery.getMaxNum())
                .eq(teamQuery.getUserId() != null && teamQuery.getUserId() > 0, Team::getUserId, teamQuery.getUserId());

        if (StringUtils.isNotBlank(teamQuery.getSearchText())) {
            queryWrapper.lambda()
                    .like(Team::getName, teamQuery.getSearchText())
                    .or()
                    .like(Team::getDescription, teamQuery.getSearchText());
        }

        queryWrapper.lambda().and(qw -> qw.gt(Team::getExpireTime, new Date()).or().isNull(Team::getExpireTime));

        Integer status = teamQuery.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum != null && (statusEnum.equals(TeamStatusEnum.PUBLIC) || statusEnum.equals(TeamStatusEnum.SECRET))) {
            queryWrapper.lambda().eq(Team::getStatus, status);
        }


        List<Team> teamList = new ArrayList<>();
        //传入的参数为私有
        if (statusEnum != null && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
            //查询出当前登录用户创建的队伍
            QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
            Long loginUserId = loginUser.getId();
            teamQueryWrapper.lambda().eq(Team::getUserId, loginUserId);
            List<Team> createTeams = this.list(teamQueryWrapper);
            List<Integer> createTeamStatus = new ArrayList<>();
            List<Long> createTeamIds = new ArrayList<>();
            createTeams.forEach(team -> {
                createTeamIds.add(team.getId());
                createTeamStatus.add(team.getStatus());
            });
            //不是管理员，且创建队伍不为空，且创建的队伍状态不包含私有，且参数为私有
            if (!isAdmin
                    && CollectionUtils.isNotEmpty(createTeamStatus)
                    && !createTeamStatus.contains(TeamStatusEnum.PRIVATE.getValue())) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限访问");
            }
            //不是管理员，且创建队伍不为空，且创建的队伍状态包含私有,且参数为私有
            if (!isAdmin
                    && CollectionUtils.isNotEmpty(createTeamStatus)
                    && createTeamStatus.contains(TeamStatusEnum.PRIVATE.getValue())) {
                //遍历创建的私有队伍
                for (Long createTeamId : createTeamIds) {
                    Team createTeam = this.getTeamById(createTeamId);
                    if (createTeam.getStatus() == 1) {
                        teamList.add(createTeam);
                    }
                }
            }
            queryWrapper.lambda().eq(Team::getStatus, status);
        }

        //如果是管理员，可以查询私有的队伍
        if (statusEnum != null && isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
            queryWrapper.lambda().eq(Team::getStatus, status);
        }

        List<Team> teamList1 = this.list(queryWrapper);
        //加入私有队伍
        teamList1.addAll(teamList);

        if (CollectionUtils.isEmpty(teamList1)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> respTeamUserVO = new ArrayList<>();
        //关联查询用户信息
        for (Team team : teamList1) {
            if (team.getExpireTime() == null || team.getExpireTime().after(new Date())) {
                TeamUserVO teamUserVO = new TeamUserVO();
                BeanUtils.copyProperties(team, teamUserVO);
                ArrayList<User> userList = new ArrayList<>();
                ArrayList<Long> memberId = new ArrayList<>();
                Long teamId = team.getId();
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.lambda().eq(UserTeam::getTeamId, teamId);
                List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
                for (UserTeam userTeam : list) {
                    User user = userService.getById(userTeam.getUserId());
                    User safetyUser = userService.getSafetyUser(user);
                    userList.add(safetyUser);

                    //所有加入队伍的成员id
                    memberId.add(user.getId());
                    teamUserVO.setUserList(userList);
                }
                User userById = userService.getById(team.getUserId());
                teamUserVO.setCreateUsername(userById.getUsername());
                teamUserVO.setCreateAvatarUrl(userById.getAvatarUrl());
                teamUserVO.setCreateUser(userById);
                teamUserVO.setMemberId(memberId);
                Long userId = loginUser.getId();
                teamUserVO.setIsJoin(memberId.contains(userId));
                respTeamUserVO.add(teamUserVO);
            }

        }
        return respTeamUserVO;
    }
}




