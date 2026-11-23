-- B-01 GeoJSON 导入编码关联。增量脚本，不重建现有业务表。
-- 先经团队确认，再在 AgriNav 的 PostgreSQL/PostGIS 数据库执行。
-- 既有地块没有外部编码，本脚本不猜测或回填其编码。
-- 编码全库唯一，逻辑删除的地块也保留编码，避免历史记录串号。
BEGIN;
CREATE TABLE IF NOT EXISTS public.land_plot_import_code (
    plot_code varchar(64) PRIMARY KEY,
    land_plot_id bigint NOT NULL UNIQUE REFERENCES public.land_plot(id),
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_land_plot_import_code_nonblank CHECK (length(btrim(plot_code)) > 0)
);
COMMENT ON TABLE public.land_plot_import_code IS '导入地块外部编码与内部雪花ID关联（一地一码）';
COMMIT;