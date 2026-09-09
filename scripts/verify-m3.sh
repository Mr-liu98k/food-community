#!/usr/bin/env bash
# M3 验收脚本：探店审核 / 达人榜热度 / 预约双主体
# 用法：bash scripts/verify-m3.sh
# 说明：脚本通过 mysql（密码读 backend/.env）把一个测试用户临时设为探店达人，
#       以便走通"达人处理预约"的闭环，结束时会复原。
set -u
BASE=http://localhost:8080/api
ROOT="$(dirname "$0")/.."
if command -v python >/dev/null 2>&1; then PY=python; elif command -v python3 >/dev/null 2>&1; then PY=python3; else PY=node; fi
json() { $PY -c "import sys,json;print(json.load(sys.stdin)$1)"; }

ADMIN_USER=$(grep '^ADMIN_USERNAME=' "$ROOT/backend/.env" | cut -d= -f2)
ADMIN_PASS=$(grep '^ADMIN_PASSWORD=' "$ROOT/backend/.env" | cut -d= -f2)
MYSQL_PASS=$(grep '^MYSQL_PASSWORD=' "$ROOT/backend/.env" | cut -d= -f2)
STAMP=$(date +%s)

login() { curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\"}" | json "['data']['token']"; }
reg() { curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\",\"nickname\":\"$3\"}" >/dev/null; }

D="m3d_$STAMP"; E="m3e_$STAMP"
PD="pw$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"
PE="pw$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"
reg "$D" "$PD" "准达人D"; reg "$E" "$PE" "预约人E"
TD=$(login "$D" "$PD"); TE=$(login "$E" "$PE"); AT=$(login "$ADMIN_USER" "$ADMIN_PASS")
DID=$(curl -s $BASE/user/me -H "Authorization: Bearer $TD" | json "['data']['id']")
echo "[1] 注册 D(id=$DID)/E 并登录 OK"

echo "[2] 临时把 D 设为探店达人（读 .env 密码操作本地 MySQL）"
mysql -uroot -p"$MYSQL_PASS" -e "USE food_community; UPDATE \`user\` SET is_expert=1 WHERE id=$DID;" 2>/dev/null
RANK=$(curl -s $BASE/expert/list | json "['data']" | head -c 120)
echo "    达人榜现含 D: $RANK"

echo "[3] D 发布探店（待审）"
SVID=$(curl -s -X POST $BASE/my/store-visit -H "Authorization: Bearer $TD" -H 'Content-Type: application/json' \
  -d "{\"title\":\"M3验收探店\",\"cover\":\"\",\"content\":\"验收用探店内容\",\"address\":\"测试路1号\"}" | json "['data']")
echo "    探店ID=$SVID"

echo "[4] 匿名/前台访问应被拒（待审）；admin 审核通过"
ST=$(curl -s $BASE/store-visit/$SVID | json "['code']")
curl -s -X PUT $BASE/admin/store-visit/$SVID/audit -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' -d '{"status":1}' >/dev/null
echo "    审核前前台 code=$ST(期望500)，已通过"

echo "[5] E 浏览一次 → views=1；达人榜 D 热度=2"
curl -s $BASE/store-visit/$SVID >/dev/null
HEAT=$(curl -s $BASE/expert/list | $PY -c "
import sys,json
for x in json.load(sys.stdin)['data']:
    print(x['heat'])" 2>/dev/null | grep -v '^$' | head -5)
echo "    达人榜热度列表: $HEAT (D 应为 2，排序最前或匹配)"

echo "[6] E 预约 D：先试预约管理员(非达人) → 应失败"
BAD=$(curl -s -X POST $BASE/reservation -H "Authorization: Bearer $TE" -H 'Content-Type: application/json' \
  -d '{"expertId":1,"storeName":"测试店"}' | json "['msg']")
echo "    $BAD"
RVID=$(curl -s -X POST $BASE/reservation -H "Authorization: Bearer $TE" -H 'Content-Type: application/json' \
  -d "{\"expertId\":$DID,\"storeName\":\"M3测试咖啡店\",\"storeAddress\":\"测试路2号\",\"phone\":\"13900000000\"}" | json "['data']")
echo "    E 预约 D 成功，预约ID=$RVID"

echo "[7] 双视角：E 我发起的可见 / D 我接收的可见且待处理"
A=$(curl -s "$BASE/my/reservations?direction=initiated" -H "Authorization: Bearer $TE" | json "['data']['total']")
B=$(curl -s "$BASE/my/reservations?direction=received" -H "Authorization: Bearer $TD" | json "['data']['total']")
echo "    E 发起=$A(期望1), D 接收=$B(期望1)"

echo "[8] D 拒绝不带理由 → 应失败；带理由拒绝成功"
BADR=$(curl -s -X POST $BASE/my/reservations/$RVID/reject -H "Authorization: Bearer $TD" -H 'Content-Type: application/json' -d '{}' | json "['msg']")
curl -s -X POST $BASE/my/reservations/$RVID/reject -H "Authorization: Bearer $TD" -H 'Content-Type: application/json' -d '{"reason":"档期排满"}' >/dev/null
STA=$(curl -s "$BASE/my/reservations?direction=initiated" -H "Authorization: Bearer $TE" | json "['data']['records'][0]['status']")
echo "    无理由拒绝: $BADR; 带理由拒绝后 E 视角 status=$STA(期望2)"
# 再造一条用于同意
RVID2=$(curl -s -X POST $BASE/reservation -H "Authorization: Bearer $TE" -H 'Content-Type: application/json' \
  -d "{\"expertId\":$DID,\"storeName\":\"M3测试面馆\",\"phone\":\"13900000000\"}" | json "['data']")
curl -s -X POST $BASE/my/reservations/$RVID2/agree -H "Authorization: Bearer $TD" >/dev/null
STA2=$(curl -s "$BASE/my/reservations?direction=initiated" -H "Authorization: Bearer $TE" | json "['data']['records'][0]['status']")
echo "    第二条同意后 status=$STA2(期望1)"

echo "[9] 管理端预约页 keyword 搜索可见"
TOTAL=$(curl -s "$BASE/admin/reservation?keyword=M3" -H "Authorization: Bearer $AT" | json "['data']['total']")
echo "    搜索到 $TOTAL 条(期望2)"

echo "[10] 清理：admin 删除探店与两条预约；D 复原为普通用户"
curl -s -X DELETE $BASE/admin/store-visit/$SVID -H "Authorization: Bearer $AT" >/dev/null
for id in $RVID $RVID2; do curl -s -X DELETE $BASE/admin/reservation/$id -H "Authorization: Bearer $AT" >/dev/null; done
mysql -uroot -p"$MYSQL_PASS" -e "USE food_community; UPDATE \`user\` SET is_expert=0 WHERE id=$DID;" 2>/dev/null
echo "M3 验收脚本结束（全绿即通过）"
