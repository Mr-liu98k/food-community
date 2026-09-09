#!/usr/bin/env bash
# M2 验收脚本：楼中楼评论 / 点赞 / 收藏 / 轮播图
# 用法：bash scripts/verify-m2.sh
set -u
BASE=http://localhost:8080/api
if command -v python >/dev/null 2>&1; then PY=python; elif command -v python3 >/dev/null 2>&1; then PY=python3; else PY=node; fi
json() { $PY -c "import sys,json;print(json.load(sys.stdin)$1)"; }

ADMIN_USER=$(grep '^ADMIN_USERNAME=' "$(dirname "$0")/../backend/.env" | cut -d= -f2)
ADMIN_PASS=$(grep '^ADMIN_PASSWORD=' "$(dirname "$0")/../backend/.env" | cut -d= -f2)
STAMP=$(date +%s)

login() { # $1 username $2 password -> token
  curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\"}" | json "['data']['token']"
}
reg() { # $1 username $2 password $3 nickname
  curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\",\"nickname\":\"$3\"}" >/dev/null
}
A="m2a_$STAMP"; B="m2b_$STAMP"
PA="pw$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"
PB="pw$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"
reg "$A" "$PA" "楼中楼A"; reg "$B" "$PB" "楼中楼B"
TA=$(login "$A" "$PA"); TB=$(login "$B" "$PB")
AT=$(login "$ADMIN_USER" "$ADMIN_PASS")
echo "[1] 用户A/B 与管理员均已登录"

RID=1  # 演示数据：可乐鸡翅（已通过）
echo "[2] B 点赞 + 收藏菜谱 #$RID"
curl -s -X POST $BASE/recipe/$RID/like -H "Authorization: Bearer $TB" >/dev/null
curl -s -X POST $BASE/recipe/$RID/favorite -H "Authorization: Bearer $TB" >/dev/null
LIKE=$(curl -s $BASE/recipe/$RID -H "Authorization: Bearer $TB" | json "['data']['recipe']['likesCount']")
FAV=$(curl -s "$BASE/my/favorites" -H "Authorization: Bearer $TB" | json "['data']['total']")
echo "    likesCount=$LIKE(期望12→13), 我的收藏数=$FAV(期望1)"

echo "[3] B 取消点赞（likesCount 回落）"
curl -s -X DELETE $BASE/recipe/$RID/like -H "Authorization: Bearer $TB" >/dev/null
LIKE2=$(curl -s $BASE/recipe/$RID | json "['data']['recipe']['likesCount']")
echo "    likesCount=$LIKE2(期望回12)"

echo "[4] 楼中楼：A 发根评论 → B 回复 → A 回复 B 的子评论"
C1=$(curl -s -X POST $BASE/recipe/$RID/comment -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' -d '{"content":"A的根评论"}' | json "['code']")
ROOT=$(curl -s "$BASE/recipe/$RID/comments?page=1&size=10" | json "['data']['records'][0]['id']")
C2=$(curl -s -X POST $BASE/recipe/$RID/comment -H "Authorization: Bearer $TB" -H 'Content-Type: application/json' -d "{\"content\":\"B回复根\",\"parentId\":$ROOT}" | json "['code']")
CHILD=$(curl -s "$BASE/recipe/$RID/comments?page=1&size=10" | json "['data']['records'][0]['children'][0]['id']")
C3=$(curl -s -X POST $BASE/recipe/$RID/comment -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' -d "{\"content\":\"A回复B的回复\",\"parentId\":$CHILD}" | json "['code']")
STRUCT=$(curl -s "$BASE/recipe/$RID/comments?page=1&size=10" | json "['data']['records'][0]['children']" | head -c 160)
echo "    发评codes=$C1,$C2,$C3(期望均200); children 含2条=$STRUCT"

echo "[5] A 无权删除 B 的评论 → 应失败"
ECHILD=$(curl -s "$BASE/recipe/$RID/comments?page=1&size=10" | json "['data']['records'][0]['children'][0]['id']")
CODE=$(curl -s -X DELETE $BASE/comment/$ECHILD -H "Authorization: Bearer $TA" | json "['msg']")
echo "    $CODE"

echo "[6] B 删除自己的子评论（其下 A 的回复应一并删除）"
curl -s -X DELETE $BASE/comment/$CHILD -H "Authorization: Bearer $TB" >/dev/null
CNT=$(curl -s "$BASE/recipe/$RID/comments?page=1&size=10" | json "['data']['records'][0]['children']")
echo "    children=$CNT"

echo "[7] 管理端轮播：关联已通过菜谱成功，关联待审菜谱应失败"
BN=$(curl -s -X POST $BASE/admin/banner -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' -d "{\"recipeId\":1,\"image\":\"\",\"sort\":1}" | json "['code']")
BNBAD=$(curl -s -X POST $BASE/admin/banner -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' -d '{"recipeId":3,"image":"","sort":2}' | json "['msg']")
echo "    关联通过菜谱 code=$BN(期望200); 关联待审菜谱: $BNBAD"

echo "[8] 前台轮播列表可见 1 条"
FRO=$(curl -s $BASE/banner/list | json "['data']" | head -c 150)
echo "    $FRO"

echo "[9] 管理端评论搜索（内容含'根'）"
PAGE=$(curl -s "$BASE/admin/comment?page=1&size=10" -H "Authorization: Bearer $AT" | json "['data']['total']")
echo "    管理端评论总数=$PAGE"

echo "[10] 清理：B 删根? (不可删 A 根) → A 删根评论级联；管理端删轮播"
curl -s -X DELETE $BASE/comment/$ROOT -H "Authorization: Bearer $TA" >/dev/null
TOTAL=$(curl -s "$BASE/recipe/$RID/comments?page=1&size=10" | json "['data']['total']")
echo "    删根后菜谱评论总数=$TOTAL(期望0)"
BID=$(curl -s "$BASE/admin/banner?page=1&size=10" -H "Authorization: Bearer $AT" | json "['data']['records'][0]['id']")
curl -s -X DELETE $BASE/admin/banner/$BID -H "Authorization: Bearer $AT" >/dev/null
echo "    轮播已清理"
echo "M2 验收脚本结束（全绿即通过）"
