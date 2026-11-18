-- ============================================================================
-- AgriNav（田界智航）业务闭环 DDL：复核 + 内部结算（对应 PRD MP-04、端到端流程第7节）
-- 目标库：AgriNav（PostgreSQL 17 + PostGIS 3.5），默认 schema：public
-- 说明：在已有"作业单 trajectory_info"基础上补齐业务闭环，共新增 4 张表：
--   1) pricing_rule    作业单价规则（按作业类型、阶梯面积配置单价）
--   2) review_request  复核申请（机手异议 -> 队长初核 -> 管理员终审）
--   3) settlement      结算单（按月/季、按机手汇总）
--   4) settlement_item 结算明细（结算单下每条作业单的面积、单价、金额）
-- 主键均为应用层雪花 ID（bigint），无外键，仅靠应用层关联。
-- 可重复执行：先 DROP 再 CREATE。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 作业单价规则表 pricing_rule
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.pricing_rule CASCADE;
CREATE TABLE public.pricing_rule (
    id           bigint        NOT NULL,
    rule_name    varchar(100),                                  -- 规则名称
    work_type    bigint,                                        -- 作业类型（work.id，如 4001 耕地）
    unit_price   numeric(10, 2),                                -- 单价（元/亩）
    min_area     numeric(12, 2) DEFAULT 0,                      -- 适用面积下限（亩），阶梯计价
    max_area     numeric(12, 2),                                -- 适用面积上限（亩），NULL 表示不限
    status       smallint      DEFAULT 0,                       -- 0 启用 1 停用
    create_by    varchar(64),
    create_time  timestamp,
    update_by    varchar(64),
    update_time  timestamp,
    CONSTRAINT pk_pricing_rule PRIMARY KEY (id)
);
COMMENT ON TABLE  public.pricing_rule IS '作业单价规则表（按作业类型、阶梯面积配置单价）';
COMMENT ON COLUMN public.pricing_rule.status IS '0 启用 1 停用';

-- ----------------------------------------------------------------------------
-- 2. 复核申请表 review_request
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.review_request CASCADE;
CREATE TABLE public.review_request (
    id                   bigint        NOT NULL,
    review_no            varchar(50),                               -- 复核单号（FH-...）
    trajectory_info_id   varchar(64),                               -- 关联作业单ID
    phone_number         varchar(20),                               -- 申请人（机手）手机号
    plot_id              bigint,                                    -- 地块ID
    work_type            varchar(50),                               -- 作业类型
    system_area          numeric(12, 2),                            -- 系统核算面积（亩）
    claimed_area         numeric(12, 2),                            -- 机手主张面积（亩）
    reason               varchar(500),                              -- 异议原因
    status               smallint      DEFAULT 0,                   -- 0待初核 1初核通过 2初核驳回 3终审通过 4终审驳回
    initial_reviewer     varchar(64),                               -- 初核人（队长）
    initial_comment      varchar(500),                              -- 初核意见
    initial_time         timestamp,                                 -- 初核时间
    final_reviewer       varchar(64),                               -- 终审人（管理员）
    final_comment        varchar(500),                              -- 终审意见
    final_time           timestamp,                                 -- 终审时间
    create_by            varchar(64),
    create_time          timestamp,
    update_time          timestamp,
    CONSTRAINT pk_review_request PRIMARY KEY (id)
);
COMMENT ON TABLE  public.review_request IS '复核申请表（机手异议 -> 队长初核 -> 管理员终审）';
COMMENT ON COLUMN public.review_request.status IS '0待初核 1初核通过 2初核驳回 3终审通过 4终审驳回';

-- ----------------------------------------------------------------------------
-- 3. 结算单表 settlement
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.settlement CASCADE;
CREATE TABLE public.settlement (
    id              bigint        NOT NULL,
    settlement_no   varchar(50),                                  -- 结算单号（ST-...）
    period_type     smallint,                                     -- 周期类型 1 月 2 季
    period_start    date,                                         -- 周期开始日期
    period_end      date,                                         -- 周期结束日期
    phone_number    varchar(20),                                  -- 机手手机号
    total_area      numeric(12, 2),                               -- 结算总亩数
    total_amount    numeric(12, 2),                               -- 结算总金额（元）
    status          smallint      DEFAULT 0,                      -- 0 已生成 1 已确认 2 已付款
    create_by       varchar(64),
    create_time     timestamp,
    update_time     timestamp,
    CONSTRAINT pk_settlement PRIMARY KEY (id)
);
COMMENT ON TABLE  public.settlement IS '结算单表（按月/季、按机手汇总）';
COMMENT ON COLUMN public.settlement.period_type IS '周期类型 1 月 2 季';
COMMENT ON COLUMN public.settlement.status IS '0 已生成 1 已确认 2 已付款';

-- ----------------------------------------------------------------------------
-- 4. 结算明细表 settlement_item
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.settlement_item CASCADE;
CREATE TABLE public.settlement_item (
    id                   bigint        NOT NULL,
    settlement_id        bigint,                                   -- 关联结算单ID
    trajectory_info_id   varchar(64),                              -- 作业单ID
    plot_id              bigint,                                   -- 地块ID
    work_type            varchar(50),                              -- 作业类型
    area                 numeric(12, 2),                           -- 作业面积（亩）
    unit_price           numeric(10, 2),                           -- 适用单价（元/亩）
    amount               numeric(12, 2),                           -- 金额 = 面积 × 单价
    create_time          timestamp,
    CONSTRAINT pk_settlement_item PRIMARY KEY (id)
);
COMMENT ON TABLE public.settlement_item IS '结算明细表（结算单下每条作业单的面积、单价、金额）';

-- ============================================================================
-- 初始单价规则种子（可重复执行；显式指定雪花风格ID，避免与应用生成ID冲突）
-- 对应 work 表：4001 耕地 4002 旋耕 4003 播种 4004 植保 4005 收割 4006 秸秆还田
-- ============================================================================
DELETE FROM public.pricing_rule WHERE id BETWEEN 9001 AND 9006;
INSERT INTO public.pricing_rule (id, rule_name, work_type, unit_price, min_area, max_area, status, create_time) VALUES
    (9001, '耕地作业基准单价', 4001, 45.00, 0, NULL, 0, now()),
    (9002, '旋耕作业基准单价', 4002, 38.00, 0, NULL, 0, now()),
    (9003, '播种作业基准单价', 4003, 30.00, 0, NULL, 0, now()),
    (9004, '植保作业基准单价', 4004, 12.00, 0, NULL, 0, now()),
    (9005, '收割作业基准单价', 4005, 55.00, 0, NULL, 0, now()),
    (9006, '秸秆还田基准单价', 4006, 28.00, 0, NULL, 0, now());
