-- ============================================================================
-- 智能体指令审计表（A-06）
-- 记录每次智能体调用的用户、时间、位置、指令原文、意图、工具调用与结果，
-- 支撑后台审计、问题追溯与答辩演示。
-- 数据源：PostgreSQL / PostGIS（@DS("pg")）
-- 幂等：使用 CREATE TABLE IF NOT EXISTS，重复执行不会清空已有审计数据。
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.agent_audit_log (
    id              bigint         NOT NULL,
    user_id         varchar(64),                                  -- 操作用户 ID
    user_name       varchar(64),                                  -- 登录账号（手机号）
    nick_name       varchar(64),                                  -- 用户昵称
    side            varchar(16),                                  -- 所属端：ag 农机端 / ma 管理端
    intent          varchar(64),                                  -- 识别意图 code（A-04），可空
    message         text,                                         -- 指令原文
    tool_name       varchar(64),                                  -- 调用的工具名（A-05），可空
    tool_arguments  text,                                         -- 工具参数 JSON 字符串，可空
    tool_result     text,                                         -- 工具执行结果文本，可空
    receipt_no      varchar(64),                                  -- 写操作回执编号，可空
    status          varchar(32),                                  -- REPLY / NEED_CONFIRM / NEED_PARAMS / FAILED
    reply           text,                                         -- 最终自然语言回复，可空
    error_msg       text,                                         -- 失败/兜底原因，可空
    longitude       numeric(12, 8),                               -- 位置经度，可空
    latitude        numeric(12, 8),                               -- 位置纬度，可空
    cost_ms         bigint,                                       -- 本次处理耗时（毫秒）
    create_time     timestamp,                                    -- 记录时间
    CONSTRAINT pk_agent_audit_log PRIMARY KEY (id)
);

COMMENT ON TABLE  public.agent_audit_log IS '智能体指令审计表（用户、时间、位置、指令原文、工具结果，可查可追溯）';
COMMENT ON COLUMN public.agent_audit_log.status IS 'REPLY 直接回复 / NEED_CONFIRM 待二次确认 / NEED_PARAMS 缺参 / FAILED 失败或兜底';

-- 审计高频查询维度：按用户、时间、意图检索
CREATE INDEX IF NOT EXISTS idx_agent_audit_user   ON public.agent_audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_agent_audit_time   ON public.agent_audit_log (create_time);
CREATE INDEX IF NOT EXISTS idx_agent_audit_intent ON public.agent_audit_log (intent);
