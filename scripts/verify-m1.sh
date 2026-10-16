#!/usr/bin/env bash
# M1 验收脚本：分类 + 菜谱发布/审核/可见性闭环
# 用法：bash scripts/verify-m1.sh
# 管理员密码从 backend/.env 读取；测试账号密码随机生成，避免命令行明文。
set -u
ENV_FILE="$(dirname "$0")/../backend/.env"
ENV_PORT=$(grep -E '^SERVER_PORT=' "$ENV_FILE" 2>/dev/null | cut -d= -f2 | tr -d '[:space:]')
BASE=http://localhost:${BACKEND_PORT:-${ENV_PORT:-8080}}/api

if command -v python >/dev/null 2>&1; then PY=python; elif command -v python3 >/dev/null 2>&1; then PY=python3; else PY=node; fi
json() { $PY -c "import sys,json;print(json.load(sys.stdin)$1)"; }

ADMIN_USER=$(grep '^ADMIN_USERNAME=' "$(dirname "$0")/../backend/.env" | cut -d= -f2)
ADMIN_PASS=$(grep '^ADMIN_PASSWORD=' "$(dirname "$0")/../backend/.env" | cut -d= -f2)
TPASS="m1$(head -c 6 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 8)"
STAMP=$(date +%s)

echo "[1] 注册并登录测试用户"
U="m1_$STAMP"
curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$TPASS\",\"nickname\":\"M1测试\"}" >/dev/null
UTOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$TPASS\"}" | json "['data']['token']")
echo "    用户 $U OK"

ATOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | json "['data']['token']")
echo "[2] 管理员 $ADMIN_USER 登录 OK"

echo "[3] 管理员新增分类（名字含时间戳避免重复）"
CNAME="测试分类$STAMP"
curl -s -X POST $BASE/admin/category -H "Authorization: Bearer $ATOKEN" -H 'Content-Type: application/json' -d "{\"name\":\"$CNAME\",\"sort\":99}" >/dev/null
# 注意：name 含中文，必须用 --data-urlencode 让 curl 编码，否则 URL 里的原始中文
# 会被 Tomcat 判为非法字符直接返回 HTTP 400（脚本会误报成"分类为空"）。
# 同时显式传 size，避免默认 size=10 时新分类(sort=99)被挤出第一页。
CID=$(curl -s --get "$BASE/admin/category" \
  --data-urlencode "name=$CNAME" --data "size=50" \
  -H "Authorization: Bearer $ATOKEN" | json "['data']['records'][0]['id']")
echo "    分类ID=$CID"

echo "[4] 用户发布菜谱（主从表）"
# cover 在后端 RecipeSaveRequest 中是 @NotBlank，空串会被拒("请上传封面图")。
# 验收脚本没有真实上传文件，这里给一个占位 URL 走通链路。
BODY="{\"categoryId\":$CID,\"title\":\"M1验收菜$STAMP\",\"cover\":\"/uploads/verify-placeholder.png\",\"description\":\"验收用\",\"ingredients\":[{\"name\":\"土豆\",\"amount\":\"2个\"}],\"steps\":[{\"content\":\"洗净切块\"}]}"
RID=$(curl -s -X POST $BASE/my/recipe -H "Authorization: Bearer $UTOKEN" -H 'Content-Type: application/json' -d "$BODY" | json "['data']")
echo "    菜谱ID=$RID"

echo "[5] 待审核状态校验：前台详情应被拒"
ST=$(curl -s $BASE/recipe/$RID | json "['code']")
echo "    前台访问 code=$ST (期望500/拒绝)"

echo "[6] 拒绝校验：不带理由拒绝 → 期望失败"
RC=$(curl -s -X PUT $BASE/admin/recipe/$RID/audit -H "Authorization: Bearer $ATOKEN" -H 'Content-Type: application/json' -d '{"status":2}' | json "['msg']")
echo "    $RC"

echo "[7] 管理员审核通过"
curl -s -X PUT $BASE/admin/recipe/$RID/audit -H "Authorization: Bearer $ATOKEN" -H 'Content-Type: application/json' -d '{"status":1,"reason":""}' >/dev/null
echo "[8] 前台详情可看 + 浏览量自增"
V1=$(curl -s $BASE/recipe/$RID | json "['data']['recipe']['views']")
V2=$(curl -s $BASE/recipe/$RID | json "['data']['recipe']['views']")
echo "    views $V1 → $V2 (期望 +1)"

echo "[9] 公开列表含该菜谱"
CNT=$(curl -s "$BASE/recipe?keyword=M1%E9%AA%8C%E6%94%B6" | json "['data']['total']")
echo "    检索到 $CNT 条"

echo "[10] 用户删除自己的菜谱"
curl -s -X DELETE $BASE/my/recipe/$RID -H "Authorization: Bearer $UTOKEN" >/dev/null
echo "[11] 清理：管理员删除空分类"
curl -s -X DELETE $BASE/admin/category/$CID -H "Authorization: Bearer $ATOKEN" >/dev/null
echo "M1 验收脚本结束（全绿即通过）"
