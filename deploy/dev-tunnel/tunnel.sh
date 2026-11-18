#!/usr/bin/env bash
# AgriNav SSH 隧道（云端共享数据库）
# 开发期间请保持本终端开启，Ctrl+C 或关闭窗口即断开隧道。
echo "============================================================"
echo "  AgriNav SSH 隧道（云端共享数据库）"
echo "  本地端口映射："
echo "    MySQL         127.0.0.1:13306"
echo "    PostgreSQL    127.0.0.1:15432"
echo "    Redis         127.0.0.1:16379"
echo "    Elasticsearch 127.0.0.1:19200"
echo "============================================================"
ssh -N \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  -o ExitOnForwardFailure=yes \
  -L 13306:127.0.0.1:3306 \
  -L 15432:127.0.0.1:5432 \
  -L 16379:127.0.0.1:6379 \
  -L 19200:127.0.0.1:9200 \
  root@82.156.224.217
