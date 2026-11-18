# -*- coding: utf-8 -*-
"""生成两条作业轨迹的缩略图（模拟地图底图 + 地块边界 + 作业轨迹）"""
from PIL import Image, ImageDraw
import os

OUT_DIR = r"D:\ruoyi\uploadPath\agri\trajectory"
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 480, 360
# 地块矩形（留边距）
X0, Y0, X1, Y1 = 70, 55, 410, 305

# 配色
BG = (245, 244, 238)          # 地图底图（浅米）
OTHER = (225, 224, 216)       # 周边田块线
FILL = (224, 240, 221)        # 地块填充 0xe0f0dd
BORDER = (120, 150, 140)      # 地块边界
TRACK = (79, 158, 130)        # 轨迹 0x95dda2 附近
START = (60, 170, 90)         # 起点绿
END = (210, 70, 70)           # 终点红


def base_canvas():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    # 周边田块网格（模拟地图）
    for x in range(0, W, 48):
        d.line([(x, 0), (x, H)], fill=OTHER, width=1)
    for y in range(0, H, 48):
        d.line([(0, y), (W, y)], fill=OTHER, width=1)
    # 地块填充与边界
    d.rectangle([X0, Y0, X1, Y1], fill=FILL, outline=BORDER, width=3)
    return img, d


def marker(d, xy, color):
    r = 6
    d.ellipse([xy[0]-r, xy[1]-r, xy[0]+r, xy[1]+r], fill=color, outline=(255, 255, 255), width=2)


def draw_lanes(d, n_lanes):
    """在地块内画 n 条往返耙地轨迹"""
    top, bottom = Y0 + 22, Y1 - 22
    step = (bottom - top) / max(n_lanes, 1)
    pts = []
    for i in range(n_lanes):
        y = top + i * step
        if i % 2 == 0:
            pts.append((X0 + 24, y))
            pts.append((X1 - 24, y))
        else:
            if i == 0:
                pts.append((X1 - 24, y))
            pts.append((X1 - 24, y))
            pts.append((X0 + 24, y))
        # 连接到下一行（转弯）
        if i < n_lanes - 1:
            ny = top + (i + 1) * step
            anchor = (X1 - 24) if i % 2 == 0 else (X0 + 24)
            pts.append((anchor, ny))
    d.line(pts, fill=TRACK, width=4, joint="curve")
    marker(d, pts[0], START)
    marker(d, pts[-1], END)
    return pts[0], pts[-1]


# 图1：完整作业（area=1.11），6 条往返
img1, d1 = base_canvas()
s, e = draw_lanes(d1, 6)
d1.text((X0, Y1 + 12), "Work trajectory  Area: 1.11 mu", fill=(90, 90, 90))
img1.save(os.path.join(OUT_DIR, "traj_1.jpg"), quality=90)

# 图2：几乎未作业（area=0.00），仅起点附近一小段
img2, d2 = base_canvas()
p0 = (X0 + 24, Y0 + 22)
p1 = (X0 + 95, Y0 + 22)
d2.line([p0, p1], fill=TRACK, width=4)
marker(d2, p0, START)
marker(d2, p1, END)
d2.text((X0, Y1 + 12), "Work trajectory  Area: 0.00 mu", fill=(90, 90, 90))
img2.save(os.path.join(OUT_DIR, "traj_2.jpg"), quality=90)

for f in ("traj_1.jpg", "traj_2.jpg"):
    p = os.path.join(OUT_DIR, f)
    print(f, os.path.getsize(p))
