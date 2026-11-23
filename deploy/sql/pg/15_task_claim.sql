-- ============================================================================
-- 智能体任务认领记录表（A-11，AG-01 任务助手）
-- 记录农机手对农活（farm_work）的认领动作与回执，支撑"我的任务"查询、
-- 防重复认领与全程可追溯。
-- 数据源：PostgreSQL / PostGIS（@DS("pg")）
-- 幂等：CREATE TABLE IF NOT EXISTS，重复执行不清空已有认领数据。
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.agent_task_claim (
    id            bigint       NOT NULL,
    farm_work_id  varchar(64)  NOT NULL,                          -- 认领的农活 ID（farm_work.id）
    user_id       varchar(64),                                   -- 认领农机手的用户 ID
    user_name     varchar(64),                                   -- 登录账号（手机号）
    nick_name     varchar(64),                                   -- 农机手昵称
    receipt_no    varchar(64),                                   -- 认领回执编号 RC-xxx
    status        varchar(16)  DEFAULT 'CLAIMED',                -- CLAIMED 已认领 / CANCELLED 已取消
    create_time   timestamp,                                     -- 认领时间
    CONSTRAINT pk_agent_task_claim PRIMARY KEY (id)
);

COMMENT ON TABLE  public.agent_task_claim IS '智能体任务认领记录表（农机手认领农活与回执，可追溯）';
COMMENT ON COLUMN public.agent_task_claim.status IS 'CLAIMED 已认领 / CANCELLED 已取消';

-- 认领高频查询：按农机手查我的任务、按农活反查认领人、按时间检索
CREATE INDEX IF NOT EXISTS idx_agent_claim_user  ON public.agent_task_claim (user_id);
CREATE INDEX IF NOT EXISTS idx_agent_claim_work  ON public.agent_task_claim (farm_work_id);
CREATE INDEX IF NOT EXISTS idx_agent_claim_time  ON public.agent_task_claim (create_time);
