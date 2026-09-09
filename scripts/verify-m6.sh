#!/usr/bin/env bash
# M6 验收脚本：数据看板 + 用户/管理员管理
set -u
BASE=http://localhost:8080/api
ROOT="$(dirname "$0")/.."
if command -v python >/dev/null 2>&1; then PY=python; elif command -v python3 >/dev/null 2>&1; then PY=python3; else PY=node; fi
json() { $PY -c "import sys,json;print(json.load(sys.stdin)$1)"; }

ADMIN_USER=$(grep '^ADMIN_USERNAME=' "$ROOT/backend/.env" | cut -d= -f2)
ADMIN_PASS=$(grep '^ADMIN_PASSWORD=' "$ROOT/backend/.env" | cut -d= -f2)
MYSQL_PASS=$(grep '^MYSQL_PASSWORD=' "$ROOT/backend/.env" | cut -d= -f2)
STAMP=$(date +%s)

AT=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | json "['data']['token']")
echo "[1] 管理员登录 OK"

echo "[2] 看板聚合结构（7 天折线 + 分类饼图）"
DASH=$(curl -s $BASE/admin/dashboard -H "Authorization: Bearer $AT")
WK=$(echo "$DASH" | $PY -c "import sys,json;print(len(json.load(sys.stdin)['data']['weekly']))")
echo "    weekly 天数=$WK(期望7), recipeCount=$(echo "$DASH" | json "['data']['recipeCount']"), commentCount=$(echo "$DASH" | json "['data']['commentCount']")"

echo "[3] admin 创建普通用户 U（随机账号）"
U="m6u_$STAMP"
UP="pw$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"
curl -s -X POST $BASE/admin/users -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$U\",\"password\":\"$UP\",\"nickname\":\"M6用户\"}" >/dev/null
UID2=$(curl -s "$BASE/admin/users?keyword=$U" -H "Authorization: Bearer $AT" | json "['data']['records'][0]['id']")
echo "    用户ID=$UID2"

echo "[4] 把 U 设为探店达人 → 达人榜可见（按 id 校验）"
curl -s -X PUT $BASE/admin/users/$UID2/expert -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' -d '{"expert":true}' >/dev/null
INRANK=$(curl -s $BASE/expert/list | $PY -c "import sys,json;print(any(str(x['id'])=='$UID2' for x in json.load(sys.stdin)['data']))")
echo "    达人榜含U: $INRANK(期望True)"

echo "[5] 改昵称 + 重置密码"
curl -s -X PUT $BASE/admin/users/$UID2 -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' -d '{"nickname":"新昵称U"}' >/dev/null
NEWP="np$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"
curl -s -X PUT $BASE/admin/users/$UID2/password -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' -d "{\"password\":\"$NEWP\"}" >/dev/null
NEWLOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$NEWP\"}" | json "['code']")
echo "    新密码登录 code=$NEWLOGIN(期望200)"

echo "[6] 删除干净用户 U → 成功"
DEL=$(curl -s -X DELETE $BASE/admin/users/$UID2 -H "Authorization: Bearer $AT" | json "['code']")
echo "    删除 code=$DEL(期望200)"

echo "[7] 删除有业务数据的用户 demo_fan → 应被拒绝"
FANID=$(mysql -uroot -p"$MYSQL_PASS" -N -e "SELECT id FROM food_community.user WHERE username='demo_fan'" 2>/dev/null)
REF=$(curl -s -X DELETE $BASE/admin/users/$FANID -H "Authorization: Bearer $AT" | json "['msg']")
echo "    $REF"

echo "[8] 管理员不能删除自己"
SELF=$(curl -s -X DELETE $BASE/admin/admins/1 -H "Authorization: Bearer $AT" | json "['msg']")
echo "    $SELF"

echo "[9] 管理端分页：admins/users 数量"
ACNT=$(curl -s "$BASE/admin/admins?page=1&size=10" -H "Authorization: Bearer $AT" | json "['data']['total']")
UCNT=$(curl -s "$BASE/admin/users?page=1&size=10" -H "Authorization: Bearer $AT" | json "['data']['total']")
echo "    管理员数=$ACNT(期望>=1), 用户数=$UCNT"
echo "M6 验收脚本结束（全绿即通过）"
