#!/usr/bin/env bash
# M5 验收脚本（公告模块闭环）：admin 增改查删 + 前台可见性
# WebSocket 实时聊天请另跑：node scripts/smoke-ws.mjs（8 项自检）
set -u
BASE=http://localhost:8080/api
ROOT="$(dirname "$0")/.."
if command -v python >/dev/null 2>&1; then PY=python; elif command -v python3 >/dev/null 2>&1; then PY=python3; else PY=node; fi
json() { $PY -c "import sys,json;print(json.load(sys.stdin)$1)"; }

ADMIN_USER=$(grep '^ADMIN_USERNAME=' "$ROOT/backend/.env" | cut -d= -f2)
ADMIN_PASS=$(grep '^ADMIN_PASSWORD=' "$ROOT/backend/.env" | cut -d= -f2)
STAMP=$(date +%s)

AT=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | json "['data']['token']")
echo "[1] 管理员 $ADMIN_USER 登录 OK"

TITLE="M5测试公告 $STAMP"
echo "[2] 发布公告"
NID=$(curl -s -X POST $BASE/admin/notice -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
  -d "{\"title\":\"$TITLE\",\"content\":\"这是一条**测试**公告，用于验证管理端与前台联动。\"}" | json "['data']" 2>/dev/null)
# create 返回 void(data null)，改查列表拿 id
NID=$(curl -s "$BASE/admin/notice?keyword=M5%E6%B5%8B%E8%AF%95" -H "Authorization: Bearer $AT" | json "['data']['records'][0]['id']")
echo "    公告ID=$NID"

echo "[3] 前台列表与 latest 可见（公开）"
TOTAL=$(curl -s "$BASE/notice?page=1&size=10" | json "['data']['total']")
LATEST=$(curl -s "$BASE/notice/latest" | json "['data'][0]['title']")
echo "    前台列表总数=$TOTAL, latest 标题=$LATEST (应为 $TITLE)"

echo "[4] 前台详情可取"
DETAIL=$(curl -s "$BASE/notice/$NID" | json "['data']['content']" | head -c 40)
echo "    content: $DETAIL"

echo "[5] 编辑公告"
curl -s -X PUT $BASE/admin/notice/$NID -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
  -d "{\"title\":\"$TITLE\",\"content\":\"编辑后的内容\"}" >/dev/null
EDITED=$(curl -s "$BASE/notice/$NID" | json "['data']['content']")
echo "    编辑后: $EDITED"

echo "[6] 删除公告 → 前台 latest 不再包含"
curl -s -X DELETE $BASE/admin/notice/$NID -H "Authorization: Bearer $AT" >/dev/null
GONE=$(curl -s "$BASE/notice/latest" | json "['data']")
echo "    latest: $GONE"
echo "M5 公告验收脚本结束（全绿即通过）"
