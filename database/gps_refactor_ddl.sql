-- ============================================================
-- AgriNav-GPS 去 Kafka 直连改造（方案 B）
-- 1) 新建实时有效点暂存表 effective_point_buffer，替代 InfluxDB
-- 2) 依据地块几何计算并填充 boundary 包围盒（原表为空会导致有效点判定落空）
-- 可重复执行
-- ============================================================

-- ------------------------------------------------------------
-- 1. 实时有效作业点暂存表（替代 InfluxDB measurement: effective_point8）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS public.effective_point_buffer;
CREATE TABLE public.effective_point_buffer (
    id           bigint          NOT NULL,
    phone_number varchar(20),
    plot_id      bigint,
    work_type    integer,
    width        integer,
    longitude    double precision,
    latitude     double precision,
    device_time  timestamp,
    create_time  timestamp,
    CONSTRAINT pk_effective_point_buffer PRIMARY KEY (id)
);
COMMENT ON TABLE  public.effective_point_buffer IS '实时有效作业点暂存表（替代 InfluxDB，生成轨迹后清理）';
COMMENT ON COLUMN public.effective_point_buffer.phone_number IS '终端手机号（T0200 clientId）';
COMMENT ON COLUMN public.effective_point_buffer.plot_id IS '点所属地块 ID';
COMMENT ON COLUMN public.effective_point_buffer.work_type IS '作业类型（附加信息 0xEE），无则 -1';
COMMENT ON COLUMN public.effective_point_buffer.width IS '作业幅宽（附加信息 0xFF），无则 -1';
COMMENT ON COLUMN public.effective_point_buffer.longitude IS '经度 WGS84';
COMMENT ON COLUMN public.effective_point_buffer.latitude IS '纬度 WGS84';
COMMENT ON COLUMN public.effective_point_buffer.device_time IS '设备上报时间';
COMMENT ON COLUMN public.effective_point_buffer.create_time IS '服务入库时间';

CREATE INDEX idx_epb_phone_time ON public.effective_point_buffer (phone_number, device_time);
CREATE INDEX idx_epb_plot       ON public.effective_point_buffer (plot_id);

-- ------------------------------------------------------------
-- 2. 填充地块包围盒 boundary（坐标放大 1e6 取整，下界 floor、上界 ceil 保证包住点）
-- ------------------------------------------------------------
DELETE FROM public.boundary;
INSERT INTO public.boundary (
    id, left_down_longitude, left_down_latitude,
    right_top_longitude, right_top_latitude, land_plot_id, status
)
SELECT
    id,
    floor(st_xmin(boundary) * 1000000) AS left_down_longitude,
    floor(st_ymin(boundary) * 1000000) AS left_down_latitude,
    ceil (st_xmax(boundary) * 1000000) AS right_top_longitude,
    ceil (st_ymax(boundary) * 1000000) AS right_top_latitude,
    id AS land_plot_id,
    0  AS status
FROM public.land_plot
WHERE is_deleted = 0;
