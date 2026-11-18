-- ============================================================
-- 作业热力图密集点种子（复现 DenseWorkSimulator 的蛇形作业点）
-- 在两块真实耕地 3011 / 3012 内各生成 8 行 x 12 点 = 96 个内缩作业点，共 192 点
-- 作业类型 work_type=4001（耕地），幅宽 width=200，内缩 MARGIN=0.0002
-- 可重复执行：先清理该终端的种子点再插入
-- ============================================================

DELETE FROM public.effective_point_buffer WHERE phone_number = '15888888888';

WITH params(plot_id, lng_min, lng_max, lat_min, lat_max) AS (
    VALUES
        (3011::bigint, 114.811664 + 0.0002, 114.815312 - 0.0002,
                     38.005872 + 0.0002, 38.007817 - 0.0002),
        (3012::bigint, 114.806542 + 0.0002, 114.808366 - 0.0002,
                     38.006397 + 0.0002, 38.008156 - 0.0002)
),
grid AS (
    SELECT p.plot_id, p.lng_min, p.lng_max, p.lat_min, p.lat_max,
           r.r AS row_no, c.c AS col_no
    FROM params p
    CROSS JOIN generate_series(0, 7) AS r(r)
    CROSS JOIN generate_series(0, 11) AS c(c)
),
pts AS (
    SELECT
        plot_id,
        (lat_min + (lat_max - lat_min) * row_no / 7.0) AS latitude,
        CASE WHEN row_no % 2 = 0
             THEN lng_min + (lng_max - lng_min) * col_no / 11.0
             ELSE lng_max - (lng_max - lng_min) * col_no / 11.0
        END AS longitude,
        row_number() OVER (ORDER BY plot_id, row_no, col_no) AS seq
    FROM grid
)
INSERT INTO public.effective_point_buffer
    (id, phone_number, plot_id, work_type, width, longitude, latitude, device_time, create_time)
SELECT
    8800000000000000000 + seq,
    '15888888888',
    plot_id,
    4001,
    200,
    longitude,
    latitude,
    now() - make_interval(secs => (192 - seq) * 2),
    now()
FROM pts;

-- 核对：每个地块的点数
SELECT plot_id, count(*) AS point_count
FROM public.effective_point_buffer
WHERE phone_number = '15888888888'
GROUP BY plot_id
ORDER BY plot_id;
