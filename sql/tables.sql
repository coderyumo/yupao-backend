-- auto-generated definition
create table avatar
(
    id         bigint auto_increment comment 'id'
        primary key,
    md5        varchar(255)                       null comment 'md5标识',
    name       varchar(255)                       null comment '文件名称',
    type       varchar(255)                       null comment '文件类型',
    size       bigint                             null comment '文件大小（kb）',
    url        varchar(255)                       null comment '文件路径',
    userId     bigint                             not null comment '用户id',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    isDelete   tinyint  default 0                 null comment '是否删除',
    constraint unique_name
        unique (name)
)
    comment '头像表';

create index unique_md5
    on avatar (md5);

-- auto-generated definition
create table message_send_log
(
    msgId           varchar(512)                       not null comment '消息id（uuid）',
    senderId        bigint                             null comment '发送人id',
    recipientId     bigint                             null comment '收到人id',
    addFriendStatus tinyint                            null comment '添加好友状态',
    inviterId       bigint                             null comment '邀请人id',
    routeKey        varchar(255)                       null comment '队列名字',
    status          tinyint  default 0                 null comment '0-发送中 1-发送成功 2-发送失败',
    exchange        varchar(255)                       null comment '交换机名字',
    tryCount        tinyint                            null comment '重试次数',
    tryTime         datetime                           null comment '第一次重试时间',
    createTime      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    isDelete        tinyint  default 0                 null comment '是否删除',
    constraint unique_msgId
        unique (msgId)
)
    comment '消息发送日志表';

-- auto-generated definition
create table notice
(
    id              bigint auto_increment comment '主键'
        primary key,
    senderId        bigint                             null comment '添加好友发送人id',
    recipientId     bigint                             null comment '添加好友接收人id',
    InviterId       bigint                             null comment '邀请人Id',
    inviteeId       bigint                             null comment '被邀请人id',
    addFriendStatus tinyint                            not null comment '添加好友状态',
    createTime      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    isDelete        tinyint  default 0                 null comment '是否删除'
)
    comment '通知表';

-- auto-generated definition
create table tag
(
    id         bigint auto_increment comment '主键'
        primary key,
    tagName    varchar(256)                       null comment '标签名称',
    parentId   bigint                             null comment '父标签id ',
    isParent   tinyint                            null comment '是否为父标签 0-不是，1-父标签',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint  default 0                 null comment '是否删除',
    constraint unique_tagName
        unique (tagName)
)
    comment '标签表';

-- auto-generated definition
create table team
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    maxNum      int      default 1                 not null comment '最大人数',
    expireTime  datetime                           null comment '过期时间',
    userId      bigint                             null comment '用户id',
    status      int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    password    varchar(512)                       null comment '密码',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment '队伍';

-- auto-generated definition
create table user
(
    id           bigint auto_increment comment '主键'
        primary key,
    friendId     varchar(1024)                      null comment '好友id',
    addCount     int(10)                            null comment '被添加好友的次数',
    username     varchar(256)                       null comment '昵称',
    userAccount  varchar(256)                       null comment '登录账号',
    avatarUrl    varchar(1024)                      null comment '头像',
    gender       tinyint                            null comment '性别',
    userPassword varchar(512)                       not null comment '密码',
    phone        varchar(128)                       null comment '电话号码',
    email        varchar(512)                       null comment '邮箱',
    userStatus   int      default 0                 null comment '用户状态0-正常',
    tags         varchar(1024)                      null comment '标签列表JSON',
    profile      varchar(1024)                      null comment '自我介绍',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    isDelete     tinyint  default 0                 null comment '是否删除',
    userRole     int      default 0                 null comment '用户角色 0-普通用户，1-管理员',
    planetCode   varchar(512)                       null comment '星球编号',
    constraint unique_key_planetcode
        unique (planetCode),
    constraint unique_key_useraccount
        unique (userAccount)
)
    comment '用户表';

-- auto-generated definition
create table user_team
(
    id         bigint auto_increment comment 'id'
        primary key,
    userId     bigint                             null comment '用户id',
    teamId     bigint                             null comment '队伍id',
    joinTime   datetime                           null comment '加入时间',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint  default 0                 not null comment '是否删除'
)
    comment '用户队伍关系';

