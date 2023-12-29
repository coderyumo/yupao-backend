package com.yupi.yupaobackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupaobackend.common.ErrorCode;
import com.yupi.yupaobackend.exception.BusinessException;
import com.yupi.yupaobackend.mapper.TeamMapper;
import com.yupi.yupaobackend.model.domain.Team;
import com.yupi.yupaobackend.model.domain.User;
import com.yupi.yupaobackend.model.domain.UserTeam;
import com.yupi.yupaobackend.model.dto.AddTeamParam;
import com.yupi.yupaobackend.model.enums.TeamStatusEnum;
import com.yupi.yupaobackend.service.TeamService;
import com.yupi.yupaobackend.service.UserService;
import com.yupi.yupaobackend.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
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
}




