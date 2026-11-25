-- ============================================================================
-- 异常上报表（A-14，AG-04 异常上报）
-- 记录农机手通过智能体主动上报的设备故障、定位异常、作业争议、陷车、障碍物等
-- 异常工单，包含异常类型、现象描述、作业单/地块/位置、处理人、处理时限与处理状态，
-- 支撑异常工单生成、进度查询与管理端处理闭环。
-- 数据源：PostgreSQL / PostGIS（@DS("pg")）
-- 幂等：CREATE TABLE IF NOT EXISTS，重复执行不清空已有工单数据。
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.agent_alert (
    id              bigint        NOT NULL,
    alert_no        varchar(64)   NOT NULL,                         -- 异常工单编号 AT-yyyyMMdd-xxxx
    alert_type      varchar(32)   NOT NULL,                         -- 异常类型（设备故障/定位异常/作业争议/陷车/障碍物/其他）
    description     varchar(1000) NOT NULL,                         -- 异常现象描述
    work_order_id   varchar(64),                                    -- 关联作业单号（可空）
    plot_id         bigint,                                         -- 关联地块 ID（可空）
    longitude       double precision,                               -- 异常发生位置经度（可空）
    latitude        double precision,                               -- 异常发生位置纬度（可空）
    occur_time      timestamp,                                      -- 异常发生时间（可空）
    reporter_id     varchar(64)   NOT NULL,                         -- 上报人用户 ID
    reporter_name   varchar(64),                                    -- 上报人登录账号（手机号）
    reporter_nick   varchar(64),                                    -- 上报人昵称
    handler_name    varchar(64),                                    -- 负责处理人（片区生产队长）
    deadline        timestamp,                                      -- 预计处理时限
    status          varchar(16)   NOT NULL DEFAULT 'PENDING',       -- 状态机见下方注释
    handle_result   varchar(1000),                                  -- 处理结果
    handle_by       varchar(64),                                    -- 处理人用户 ID
    handle_time     timestamp,                                      -- 实际处理时间
    create_time     timestamp,                                      -- 工单创建时间
    update_time     timestamp,                                      -- 工单更新时间
    remark          varchar(255),                                   -- 备注
    CONSTRAINT pk_agent_alert PRIMARY KEY (id)
);

COMMENT ON TABLE  public.agent_alert IS '异常上报表（机手主动上报异常工单、处理人/时限/进度跟踪）';
COMMENT ON COLUMN public.agent_alert.status IS 'PENDING 待处理 / PROCESSING 处理中 / RESOLVED 已处理 / CLOSED 已关闭';
COMMENT ON COLUMN public.agent_alert.alert_type IS '设备故障 / 定位异常 / 作业争议 / 陷车 / 障碍物 / 其他';

-- 工单编号唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_alert_no       ON public.agent_alert (alert_no);
-- 按上报人查我的工单、按状态筛选、按创建时间排序、按处理时限扫描超时
CREATE INDEX IF NOT EXISTS idx_agent_alert_reporter ON public.agent_alert (reporter_id);
CREATE INDEX IF NOT EXISTS idx_agent_alert_status   ON public.agent_alert (status);
CREATE INDEX IF NOT EXISTS idx_agent_alert_create   ON public.agent_alert (create_time);
CREATE INDEX IF NOT EXISTS idx_agent_alert_deadline ON public.agent_alert (deadline);
