-- ============================================================================
-- 作业调度派单表（A-19，MP-02 作业调度）
-- 记录管理员/队长对农活（farm_work）的单选或批量派单，指定机手与完成时限，
-- 支撑调度页派单、状态跟踪、超时标黄与片区完成率统计。
-- 数据源：PostgreSQL / PostGIS（@DS("pg")）
-- 幂等：CREATE TABLE IF NOT EXISTS，重复执行不清空已有派单数据。
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.work_dispatch (
    id              bigint       NOT NULL,
    dispatch_no     varchar(64)  NOT NULL,                          -- 派单编号 DP-yyyyMMdd-xxxx
    farm_work_id    varchar(64)  NOT NULL,                          -- 派单的农活 ID（farm_work.id）
    farm_work_name  varchar(200),                                   -- 农活名称（冗余，便于列表展示）
    plot_id         bigint,                                         -- 关联地块 ID（地图定位用）
    work_type       varchar(64),                                    -- 作业类型（可空）
    assignee_id     varchar(64)  NOT NULL,                          -- 被派机手用户 ID
    assignee_name   varchar(64),                                    -- 机手登录账号（手机号）
    assignee_nick   varchar(64),                                    -- 机手昵称
    deadline        timestamp,                                      -- 完成时限（截止时间）
    status          varchar(16)  NOT NULL DEFAULT 'DISPATCHED',     -- 状态机见下方注释
    dispatch_by     varchar(64),                                    -- 派单人（管理员）用户 ID
    dispatch_time   timestamp,                                      -- 派单时间
    accept_time     timestamp,                                      -- 接单时间
    complete_time   timestamp,                                      -- 完成时间
    remark          varchar(255),                                   -- 备注
    CONSTRAINT pk_work_dispatch PRIMARY KEY (id)
);

COMMENT ON TABLE  public.work_dispatch IS '作业调度派单表（单选/批量派单、指定机手与时限、超时跟踪）';
COMMENT ON COLUMN public.work_dispatch.status IS 'DISPATCHED 已派单待接单 / ACCEPTED 已接单进行中 / COMPLETED 已完成 / TIMEOUT 已超时 / CANCELLED 已取消';

-- 派单编号唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_work_dispatch_no  ON public.work_dispatch (dispatch_no);
-- 按机手查我的派单、按农活反查、按状态筛选、按时限扫描超时
CREATE INDEX IF NOT EXISTS idx_work_dispatch_assignee ON public.work_dispatch (assignee_id);
CREATE INDEX IF NOT EXISTS idx_work_dispatch_work     ON public.work_dispatch (farm_work_id);
CREATE INDEX IF NOT EXISTS idx_work_dispatch_status   ON public.work_dispatch (status);
CREATE INDEX IF NOT EXISTS idx_work_dispatch_deadline ON public.work_dispatch (deadline);
