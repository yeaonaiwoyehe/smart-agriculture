-- B-02 行政村/片区分组。增量建表；不修改现有地块数据。
BEGIN;
CREATE TABLE IF NOT EXISTS public.land_plot_group (
    group_type varchar(10) NOT NULL CHECK (group_type IN ('VILLAGE', 'REGION')),
    group_code varchar(64) NOT NULL,
    group_name varchar(100) NOT NULL CHECK (length(btrim(group_name)) > 0),
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_type, group_code),
    CHECK ((group_type = 'VILLAGE' AND group_code ~ '^[0-9]{12}$') OR
           (group_type = 'REGION' AND group_code ~ '^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$'))
);
CREATE TABLE IF NOT EXISTS public.land_plot_group_assignment (
    land_plot_id bigint NOT NULL REFERENCES public.land_plot(id),
    group_type varchar(10) NOT NULL,
    group_code varchar(64) NOT NULL,
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (land_plot_id, group_type),
    FOREIGN KEY (group_type, group_code) REFERENCES public.land_plot_group(group_type, group_code)
);
CREATE INDEX IF NOT EXISTS ix_land_plot_group_assignment_group
    ON public.land_plot_group_assignment(group_type, group_code, land_plot_id);
COMMENT ON TABLE public.land_plot_group IS '行政村与业务片区字典；村使用数据提供方给出的12位村级区划代码';
COMMENT ON TABLE public.land_plot_group_assignment IS '地块归组：每块地可分别归属一个行政村和一个片区';
COMMIT;
