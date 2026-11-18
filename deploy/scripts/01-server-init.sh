#!/bin/bash
# ============================================================
# AgriNav 服务器初始化（Ubuntu 22.04 / 24.04，root 执行）
# 作用：安装 Docker/Compose、配置 swap、时区、基础防火墙
# 用法：bash 01-server-init.sh
# ============================================================
set -e

if [ "$(id -u)" -ne 0 ]; then
  echo "请用 root 用户执行（或 sudo bash 01-server-init.sh）"
  exit 1
fi

echo "==> [1/6] 更新软件源并安装基础工具"
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release unzip ufw

echo "==> [2/6] 安装 Docker（官方脚本）"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable docker
systemctl start docker

echo "==> [3/6] 配置 Docker 镜像加速（国内拉取更快）"
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.m.daocloud.io"
  ],
  "log-driver": "json-file",
  "log-opts": { "max-size": "20m", "max-file": "3" }
}
EOF
systemctl daemon-reload
systemctl restart docker

echo "==> [4/6] 配置 2GB swap（内存兜底，防止 OOM）"
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo 'vm.swappiness=10' >> /etc/sysctl.conf
fi

echo "==> [5/6] 设置时区为 Asia/Shanghai"
timedatectl set-timezone Asia/Shanghai || true

echo "==> [6/6] 配置防火墙（仅放行 SSH/HTTP/JT808）"
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp        # SSH
ufw allow 80/tcp        # Nginx Web
ufw allow 7100/tcp      # JT808 TCP
ufw allow 7100/udp      # JT808 UDP
ufw --force enable

echo ""
echo "初始化完成。Docker 版本："
docker --version
docker compose version
echo ""
echo "提醒：腾讯云轻量控制台的「防火墙」也要放行 80、7100(TCP/UDP)，ufw 与云防火墙是两层。"
