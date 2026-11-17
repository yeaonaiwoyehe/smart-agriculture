-- ============================================================================
-- AgriNav（田界智航）PostgreSQL/PostGIS 业务库 DDL
-- 目标库：AgriNav（PostgreSQL 17 + PostGIS 3.5），默认 schema：public
-- 说明：本脚本根据 CMS 实体类（@TableName/@TableId/字段类型）与 mapper 反推重建，
--       共 28 张业务表。主键多为应用层雪花 ID（bigint/字符串），app_permission 为数据库自增。
-- 可重复执行：先 DROP 再 CREATE。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 地块表 land_plot（含空间边界与中心点）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.land_plot CASCADE;
CREATE TABLE public.land_plot (
    id                 bigint        NOT NULL,
    land_name          varchar(100),
    land_type          varchar(50),
    land_area          real,
    is_deleted         integer       NOT NULL DEFAULT 0,
    address            varchar(255),
    create_by          varchar(64),
    create_time        timestamp,
    update_by          varchar(64),
    update_time        timestamp,
    image              varchar(500),
    measurement_method varchar(50),
    boundary           geometry(Polygon, 4326),
    center_point       geometry(Point, 4326),
    owner_name         varchar(64),
    owner_phone        varchar(20),
    CONSTRAINT pk_land_plot PRIMARY KEY (id)
);
COMMENT ON TABLE  public.land_plot IS '地块表';
COMMENT ON COLUMN public.land_plot.boundary IS '地块边界多边形，WGS84(4326)';
COMMENT ON COLUMN public.land_plot.center_point IS '地块中心点，WGS84(4326)';

-- ----------------------------------------------------------------------------
-- 2. 地块无用区域表 land_plot_exclude
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.land_plot_exclude CASCADE;
CREATE TABLE public.land_plot_exclude (
    id          bigint  NOT NULL,
    land_plot_id bigint NOT NULL,
    boundary    geometry(Polygon, 4326),
    area        real,
    is_deleted  integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_land_plot_exclude PRIMARY KEY (id)
);
COMMENT ON TABLE public.land_plot_exclude IS '地块无用（排除）区域表';

-- ----------------------------------------------------------------------------
-- 3. 地块外接边界表 boundary（包围盒，数值列）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.boundary CASCADE;
CREATE TABLE public.boundary (
    id                   bigint  NOT NULL,
    left_down_longitude  bigint,
    left_down_latitude   bigint,
    right_top_longitude  bigint,
    right_top_latitude   bigint,
    land_plot_id         bigint,
    status               integer,
    CONSTRAINT pk_boundary PRIMARY KEY (id)
);
COMMENT ON TABLE public.boundary IS '地块外接边界（包围盒）表';

-- ----------------------------------------------------------------------------
-- 4. 作物表 crop
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.crop CASCADE;
CREATE TABLE public.crop (
    id          bigint NOT NULL,
    crop_name   varchar(100),
    is_deleted  boolean DEFAULT FALSE,
    create_time timestamp,
    update_time timestamp,
    CONSTRAINT pk_crop PRIMARY KEY (id)
);
COMMENT ON TABLE public.crop IS '作物表';

-- ----------------------------------------------------------------------------
-- 5. 作业类型表 work
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.work CASCADE;
CREATE TABLE public.work (
    id          bigint NOT NULL,
    work_name   varchar(100),
    is_deleted  boolean DEFAULT FALSE,
    create_time timestamp,
    update_time timestamp,
    CONSTRAINT pk_work PRIMARY KEY (id)
);
COMMENT ON TABLE public.work IS '作业类型表';

-- ----------------------------------------------------------------------------
-- 6. 作物-作业类型关联表 crop_work
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.crop_work CASCADE;
CREATE TABLE public.crop_work (
    crop_id      bigint NOT NULL,
    work_id      bigint NOT NULL,
    is_available boolean DEFAULT TRUE
);
COMMENT ON TABLE public.crop_work IS '作物-作业类型关联表';

-- ----------------------------------------------------------------------------
-- 7. 农机表 agricultural_machinery
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.agricultural_machinery CASCADE;
CREATE TABLE public.agricultural_machinery (
    id                   varchar(64) NOT NULL,
    type_id              varchar(64),
    phone_number         varchar(20),
    status               integer,
    create_by            varchar(64),
    create_time          timestamp,
    update_by            varchar(64),
    update_time          timestamp,
    having_grain_carrier boolean DEFAULT FALSE,
    end_time             timestamp,
    name                 varchar(100),
    address_prefix       varchar(255),
    CONSTRAINT pk_agricultural_machinery PRIMARY KEY (id)
);
COMMENT ON TABLE public.agricultural_machinery IS '农机（找活）表';

-- ----------------------------------------------------------------------------
-- 8. 农机类型表 agricultural_machinery_type
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.agricultural_machinery_type CASCADE;
CREATE TABLE public.agricultural_machinery_type (
    id          varchar(64) NOT NULL,
    name        varchar(100),
    status      integer,
    create_time timestamp,
    create_by   varchar(64),
    update_time timestamp,
    update_by   varchar(64),
    CONSTRAINT pk_agricultural_machinery_type PRIMARY KEY (id)
);
COMMENT ON TABLE public.agricultural_machinery_type IS '农机类型表';

-- ----------------------------------------------------------------------------
-- 9. 农机-作物关联表 agricultural_machinery_crop
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.agricultural_machinery_crop CASCADE;
CREATE TABLE public.agricultural_machinery_crop (
    agricultural_machinery_id varchar(64) NOT NULL,
    crop_id                   bigint      NOT NULL
);
COMMENT ON TABLE public.agricultural_machinery_crop IS '农机-作物关联表';

-- ----------------------------------------------------------------------------
-- 10. 农机图片表 agricultural_machinery_image
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.agricultural_machinery_image CASCADE;
CREATE TABLE public.agricultural_machinery_image (
    agricultural_machinery_id varchar(64) NOT NULL,
    image                     varchar(500)
);
COMMENT ON TABLE public.agricultural_machinery_image IS '农机图片表';

-- ----------------------------------------------------------------------------
-- 11. 农机位置表 agricultural_machinery_location（列名沿用原拼写）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.agricultural_machinery_location CASCADE;
CREATE TABLE public.agricultural_machinery_location (
    agriculture_machinery_id varchar(64) NOT NULL,
    location                 varchar(255)
);
COMMENT ON TABLE public.agricultural_machinery_location IS '农机位置表';

-- ----------------------------------------------------------------------------
-- 12. 农活表 farm_work
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.farm_work CASCADE;
CREATE TABLE public.farm_work (
    id           varchar(64) NOT NULL,
    name         varchar(100),
    area         numeric(12, 2),
    end_time     timestamp,
    phone_number varchar(20),
    create_by    varchar(64),
    create_time  timestamp,
    status       integer,
    update_by    varchar(64),
    update_time  timestamp,
    CONSTRAINT pk_farm_work PRIMARY KEY (id)
);
COMMENT ON TABLE public.farm_work IS '农活（招工）表';

-- ----------------------------------------------------------------------------
-- 13. 农活-作物关联表 farm_work_crop
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.farm_work_crop CASCADE;
CREATE TABLE public.farm_work_crop (
    farm_work_id varchar(64) NOT NULL,
    crop_id      bigint      NOT NULL
);
COMMENT ON TABLE public.farm_work_crop IS '农活-作物关联表';

-- ----------------------------------------------------------------------------
-- 14. 农活-地块关联表 farm_work_land_plot
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.farm_work_land_plot CASCADE;
CREATE TABLE public.farm_work_land_plot (
    farm_work_id varchar(64) NOT NULL,
    land_plot_id bigint      NOT NULL
);
COMMENT ON TABLE public.farm_work_land_plot IS '农活-地块关联表';

-- ----------------------------------------------------------------------------
-- 15. 农活-作业类型关联表 farm_work_type
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.farm_work_type CASCADE;
CREATE TABLE public.farm_work_type (
    farm_work_id  varchar(64) NOT NULL,
    work_type_id  bigint      NOT NULL
);
COMMENT ON TABLE public.farm_work_type IS '农活-作业类型关联表';

-- ----------------------------------------------------------------------------
-- 16. 工作类型表 work_type
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.work_type CASCADE;
CREATE TABLE public.work_type (
    id          bigint NOT NULL,
    work_name   varchar(100),
    create_time timestamp,
    update_time timestamp,
    delete      boolean DEFAULT FALSE,
    CONSTRAINT pk_work_type PRIMARY KEY (id)
);
COMMENT ON TABLE public.work_type IS '工作类型表';

-- ----------------------------------------------------------------------------
-- 17. APP 组件权限表 app_permission（数据库自增主键）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.app_permission CASCADE;
CREATE TABLE public.app_permission (
    component_id    integer GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    permission_name varchar(100),
    parent_id       integer,
    component_str   varchar(100),
    is_visible      integer DEFAULT 0,
    is_deleted      integer DEFAULT 0,
    CONSTRAINT pk_app_permission PRIMARY KEY (component_id)
);
COMMENT ON TABLE public.app_permission IS 'APP 组件权限表';

-- ----------------------------------------------------------------------------
-- 18. 轨迹信息表 trajectory_info
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.trajectory_info CASCADE;
CREATE TABLE public.trajectory_info (
    trajectory_info_id varchar(64) NOT NULL,
    work_type          varchar(50),
    phone_number       varchar(20),
    width              integer,
    created_by         varchar(64),
    created_time       timestamp,
    status             smallint,
    start_time         timestamp,
    end_time           timestamp,
    trajectory_img     varchar(500),
    plot_id            bigint,
    area               numeric(12, 2),
    CONSTRAINT pk_trajectory_info PRIMARY KEY (trajectory_info_id)
);
COMMENT ON TABLE public.trajectory_info IS '轨迹信息表';

-- ----------------------------------------------------------------------------
-- 19. 轨迹点集表 trajectory_points（点集以多边形存储）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.trajectory_points CASCADE;
CREATE TABLE public.trajectory_points (
    trajectory_id varchar(64) NOT NULL,
    points        geometry(Polygon, 4326),
    times         text,
    CONSTRAINT pk_trajectory_points PRIMARY KEY (trajectory_id)
);
COMMENT ON TABLE public.trajectory_points IS '轨迹点集表';

-- ----------------------------------------------------------------------------
-- 20. 轨迹排除片段表 trajectory_exclude
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.trajectory_exclude CASCADE;
CREATE TABLE public.trajectory_exclude (
    id                 varchar(64) NOT NULL,
    trajectory_info_id varchar(64),
    points             geometry(LineString, 4326),
    times              text,
    CONSTRAINT pk_trajectory_exclude PRIMARY KEY (id)
);
COMMENT ON TABLE public.trajectory_exclude IS '轨迹排除片段表';

-- ----------------------------------------------------------------------------
-- 21. 意见反馈表 sys_feedback
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.sys_feedback CASCADE;
CREATE TABLE public.sys_feedback (
    id           varchar(64) NOT NULL,
    content      varchar(1000),
    username     varchar(64),
    phone_number varchar(20),
    commit_time  timestamp,
    checked_time timestamp,
    is_checked   integer DEFAULT 0,
    checked_by   varchar(64),
    reply        varchar(1000),
    CONSTRAINT pk_sys_feedback PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_feedback IS '意见反馈表';

-- ----------------------------------------------------------------------------
-- 22. 反馈标签关联表 sys_feedback_label
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.sys_feedback_label CASCADE;
CREATE TABLE public.sys_feedback_label (
    id          varchar(64) NOT NULL,
    feedback_id varchar(64),
    label_code  bigint,
    CONSTRAINT pk_sys_feedback_label PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_feedback_label IS '反馈标签关联表';

-- ----------------------------------------------------------------------------
-- 23. 版本变更日志表 sys_change_log
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.sys_change_log CASCADE;
CREATE TABLE public.sys_change_log (
    id              varchar(64) NOT NULL,
    release_note_id varchar(64),
    type            varchar(20),
    content         varchar(500),
    sort_order      integer,
    created_by      varchar(64),
    created_at      timestamp,
    updated_at      timestamp,
    deleted         varchar(10),
    CONSTRAINT pk_sys_change_log PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_change_log IS '版本变更日志表';

-- ----------------------------------------------------------------------------
-- 24. 系统通知表 sys_notification
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.sys_notification CASCADE;
CREATE TABLE public.sys_notification (
    id          varchar(64) NOT NULL,
    type_name   varchar(50),
    receiver_id varchar(64),
    relation_id varchar(64),
    create_time timestamp,
    is_read     integer DEFAULT 0,
    read_time   timestamp,
    CONSTRAINT pk_sys_notification PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_notification IS '系统通知表';

-- ----------------------------------------------------------------------------
-- 25. 版本发布记录表 sys_release_note
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.sys_release_note CASCADE;
CREATE TABLE public.sys_release_note (
    id           varchar(64) NOT NULL,
    version      varchar(50),
    title        varchar(200),
    description  text,
    status       varchar(20),
    release_date timestamp,
    created_by   varchar(64),
    created_at   timestamp,
    updated_at   timestamp,
    deleted      boolean DEFAULT FALSE,
    CONSTRAINT pk_sys_release_note PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_release_note IS '版本发布记录表';

-- ----------------------------------------------------------------------------
-- 26. 大屏统计表 screen_statistics
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.screen_statistics CASCADE;
CREATE TABLE public.screen_statistics (
    id               varchar(64) NOT NULL,
    crop_name        varchar(100),
    work_name        varchar(100),
    total_area       numeric(14, 2),
    work_count       integer,
    last_update_time timestamp,
    CONSTRAINT pk_screen_statistics PRIMARY KEY (id)
);
COMMENT ON TABLE public.screen_statistics IS '大屏统计汇总表';

-- ----------------------------------------------------------------------------
-- 27. 用户微信关联表 sys_user_wechat
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.sys_user_wechat CASCADE;
CREATE TABLE public.sys_user_wechat (
    user_id     bigint,
    wechat_uuid varchar(64),
    token       varchar(500)
);
COMMENT ON TABLE public.sys_user_wechat IS '用户微信关联表';

-- ----------------------------------------------------------------------------
-- 28. 行政区划地址表 address
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public.address CASCADE;
CREATE TABLE public.address (
    code     bigint NOT NULL,
    name     varchar(100),
    level    smallint,
    pcode    bigint,
    category integer,
    CONSTRAINT pk_address PRIMARY KEY (code)
);
COMMENT ON TABLE public.address IS '行政区划地址表';

-- ============================================================================
-- 空间索引（GIST）与常用查询索引
-- ============================================================================
CREATE INDEX idx_land_plot_boundary     ON public.land_plot USING GIST (boundary);
CREATE INDEX idx_land_plot_center       ON public.land_plot USING GIST (center_point);
CREATE INDEX idx_land_plot_exclude_gist  ON public.land_plot_exclude USING GIST (boundary);
CREATE INDEX idx_traj_points_gist        ON public.trajectory_points USING GIST (points);
CREATE INDEX idx_traj_exclude_gist       ON public.trajectory_exclude USING GIST (points);

CREATE INDEX idx_land_plot_exclude_plot  ON public.land_plot_exclude (land_plot_id);
CREATE INDEX idx_boundary_plot           ON public.boundary (land_plot_id);
CREATE INDEX idx_am_crop_machinery       ON public.agricultural_machinery_crop (agricultural_machinery_id);
CREATE INDEX idx_am_image_machinery      ON public.agricultural_machinery_image (agricultural_machinery_id);
CREATE INDEX idx_am_location_machinery   ON public.agricultural_machinery_location (agriculture_machinery_id);
CREATE INDEX idx_fw_crop_farmwork        ON public.farm_work_crop (farm_work_id);
CREATE INDEX idx_fw_plot_farmwork        ON public.farm_work_land_plot (farm_work_id);
CREATE INDEX idx_fw_type_farmwork        ON public.farm_work_type (farm_work_id);
CREATE INDEX idx_traj_info_plot          ON public.trajectory_info (plot_id);
CREATE INDEX idx_traj_exclude_info       ON public.trajectory_exclude (trajectory_info_id);
CREATE INDEX idx_feedback_label_feedback ON public.sys_feedback_label (feedback_id);
CREATE INDEX idx_change_log_release      ON public.sys_change_log (release_note_id);
