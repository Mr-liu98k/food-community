#!/usr/bin/env bash
# 一次性：AI SSE 流式冒烟（M4）
# 注册随机用户 → 登录 → curl -N 流式请求 AI 专家，输出前若干行，验证逐字 SSE。
set -u
BASE=http://localhost:8080/api
if command -v python >/dev/null 2>&1; then PY=python; elif command -v python3 >/dev/null 2>&1; then PY=python3; else PY=node; fi
json() { $PY -c "import sys,json;print(json.load(sys.stdin)$1)"; }

STAMP=$(date +%s)
U="smoke_$STAMP"
P="pw$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"

curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$P\",\"nickname\":\"Smoke\"}" >/dev/null
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$P\"}" | json "['data']['token']")
echo "== 已登录，发起 SSE 流式请求 =="
curl -s -N -X POST $BASE/ai/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"messages":[{"role":"user","content":"请用一句话告诉我：红烧肉怎么去腥？"}]}' \
  | head -c 800
echo ""
echo "== SSE 冒烟结束 =="
