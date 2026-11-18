#!/usr/bin/env bash
# 首次配置：生成密钥并安装到服务器（整台电脑只需运行一次）
set -e
KEY="$HOME/.ssh/id_ed25519"
if [ ! -f "$KEY" ]; then
  echo "未发现密钥，正在生成 ed25519 密钥..."
  ssh-keygen -t ed25519 -N "" -f "$KEY"
else
  echo "已存在密钥，跳过生成。"
fi
echo
echo "接下来需要输入【一次】服务器登录密码。"
cat "$KEY.pub" | ssh root@82.156.224.217 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys"
echo
echo "配置完成。以后运行 ./tunnel.sh 即可免密连接。"
