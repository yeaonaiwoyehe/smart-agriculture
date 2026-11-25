-- ============================================================================
-- 智能体会话留存表（A-10）
-- 把一次连续的多轮对话归为一个"会话"，会话下挂多条"消息"（用户问 + 助手答），
-- 支撑：会话与回答留存不少于 180 天、每条可点开核对、可跳转关联业务单。
-- 数据源：PostgreSQL / PostGIS（@DS("pg")）
-- 幂等：使用 CREATE TABLE IF NOT EXISTS，重复执行不会清空已有会话数据。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. 会话表：一行代表一次连续对话
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.agent_conversation (
    id                 bigint       NOT NULL,
    user_id            varchar(64),                               -- 会话归属用户 ID
    user_name          varchar(64),                               -- 登录账号（手机号）
    nick_name          varchar(64),                               -- 用户昵称
    side               varchar(16),                               -- 所属端：ag 农机端 / ma 管理端 / common
    title              varchar(128),                              -- 会话标题（取首条问题前若干字）
    message_count      integer      DEFAULT 0,                    -- 消息总条数（问+答）
    last_message_time  timestamp,                                 -- 最近一条消息时间，列表按此排序
    create_time        timestamp,                                 -- 会话创建时间
    expire_time        timestamp,                                 -- 留存到期时间 = 创建时间 + 180 天
    CONSTRAINT pk_agent_conversation PRIMARY KEY (id)
);

COMMENT ON TABLE  public.agent_conversation IS '智能体会话表（一次连续对话，留存不少于180天）';
COMMENT ON COLUMN public.agent_conversation.expire_time IS '留存到期时间，创建时间+180天，到期后才可清理';

-- 会话高频查询：按用户、创建时间、最近消息时间检索
CREATE INDEX IF NOT EXISTS idx_agent_conv_user      ON public.agent_conversation (user_id);
CREATE INDEX IF NOT EXISTS idx_agent_conv_create    ON public.agent_conversation (create_time);
CREATE INDEX IF NOT EXISTS idx_agent_conv_lastmsg   ON public.agent_conversation (last_message_time);

-- ---------------------------------------------------------------------------
-- 2. 会话消息表：一行代表一条消息（用户问题或助手回答）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.agent_conversation_message (
    id                bigint       NOT NULL,
    conversation_id   bigint       NOT NULL,                       -- 所属会话 ID
    role              varchar(16),                                -- user 用户消息 / assistant 助手消息
    content           text,                                        -- 消息文本（问题原文或回答）
    route_type        varchar(32),                                -- 助手路由类型：BUSINESS_DATA/KNOWLEDGE/CHITCHAT/UNKNOWN
    source_label      varchar(256),                               -- 来源标注文案
    status            varchar(32),                                -- REPLY / NEED_CONFIRM / NEED_PARAMS / FAILED
    tool_name         varchar(64),                                -- 命中的业务工具名，可空
    receipt_no        varchar(64),                                -- 写操作回执编号 RC-xxx，可空
    biz_ref_no        varchar(64),                                -- 关联业务单号（跳转业务单用），可空
    biz_ref_type      varchar(32),                                -- 业务单类型：回执/作业单/结算单/复核单
    confirm_token     varchar(64),                                -- 写操作确认令牌，可空
    cost_ms           bigint,                                     -- 本轮处理耗时（毫秒）
    create_time       timestamp,                                   -- 消息时间
    CONSTRAINT pk_agent_conversation_message PRIMARY KEY (id)
);

COMMENT ON TABLE  public.agent_conversation_message IS '智能体会话消息表（每轮问与答，可核对、可跳转业务单）';
COMMENT ON COLUMN public.agent_conversation_message.role IS 'user 用户提问 / assistant 助手回答';
COMMENT ON COLUMN public.agent_conversation_message.biz_ref_no IS '关联业务单号，前端据此跳转对应业务单/回执';

-- 消息高频查询：按会话拉取、按时间排序、按业务单反查
CREATE INDEX IF NOT EXISTS idx_agent_cm_conv    ON public.agent_conversation_message (conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_cm_time    ON public.agent_conversation_message (create_time);
CREATE INDEX IF NOT EXISTS idx_agent_cm_bizref  ON public.agent_conversation_message (biz_ref_no);
