#!/usr/bin/env bash
# M0 验收脚本：注册 → 登录 → 401/403 → 资料 → 改密
# 用法：bash scripts/verify-m0.sh
# 说明：管理员密码从 backend/.env 读取；测试用户密码随机生成，避免命令行明文凭据。
set -u

# 端口：优先环境变量 BACKEND_PORT；否则读 backend/.env 的 SERVER_PORT；再否则 8080
ENV_FILE="$(dirname "$0")/../backend/.env"
ENV_PORT=$(grep -E '^SERVER_PORT=' "$ENV_FILE" 2>/dev/null | cut -d= -f2 | tr -d '[:space:]')
BASE=http://localhost:${BACKEND_PORT:-${ENV_PORT:-8080}}/api

# JSON 解析器
if command -v python >/dev/null 2>&1; then PY=python; elif command -v python3 >/dev/null 2>&1; then PY=python3; else PY=node; fi

# 从 backend/.env 读管理员账号密码
ADMIN_USER=$(grep '^ADMIN_USERNAME=' "$(dirname "$0")/../backend/.env" | cut -d= -f2)
ADMIN_PASS=$(grep '^ADMIN_PASSWORD=' "$(dirname "$0")/../backend/.env" | cut -d= -f2)
# 测试用户密码：随机 10 位，避免字面量
TPASS=$(head -c 8 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 10)
TPASS2="${TPASS}x2"

echo "[解析器] $PY | 管理员: $ADMIN_USER"
U="m0user_$(date +%H%M%S)"
echo "== 1. 注册 $U =="
curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$TPASS\",\"nickname\":\"M0Tester\"}"; echo

echo "== 2. 用户登录 =="
URESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$TPASS\"}")
echo "$URESP" | head -c 300; echo
UTOKEN=$(echo "$URESP" | $PY -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

echo "== 3. 管理员登录（启动自动创建）=="
ARESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")
echo "$ARESP" | head -c 300; echo
ATOKEN=$(echo "$ARESP" | $PY -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
AROLE=$(echo "$ARESP" | $PY -c "import sys,json;print(json.load(sys.stdin)['data']['user']['role'])")
echo ">>> 管理员 role = $AROLE (期望 1)"

echo "== 4. 无 token 调 PUT /api/user/me → 期望 code=401 =="
curl -s -X PUT $BASE/user/me -H 'Content-Type: application/json' -d '{"nickname":"x","avatar":"","intro":""}'; echo

echo "== 5. 普通用户调 /api/admin/xxx → 期望 code=403 =="
curl -s -X GET $BASE/admin/anything -H "Authorization: Bearer $UTOKEN"; echo

echo "== 6. 管理员调 /api/admin/anything → 期望非 401/403（接口不存在则 404）=="
curl -s -X GET $BASE/admin/anything -H "Authorization: Bearer $ATOKEN" -w " [http %{http_code}]"; echo

echo "== 7. GET /api/user/me（带 token）→ 期望资料含 role=0 =="
curl -s $BASE/user/me -H "Authorization: Bearer $UTOKEN" | head -c 300; echo

echo "== 8. 修改资料 =="
curl -s -X PUT $BASE/user/me -H "Authorization: Bearer $UTOKEN" -H 'Content-Type: application/json' -d '{"nickname":"M0Updater","avatar":"","intro":"test intro"}'; echo

echo "== 9. 修改密码（旧→新）=="
curl -s -X PUT $BASE/user/me/password -H "Authorization: Bearer $UTOKEN" -H 'Content-Type: application/json' -d "{\"oldPassword\":\"$TPASS\",\"newPassword\":\"$TPASS2\"}"; echo

echo "== 10. 新密码重新登录 → 期望成功 =="
curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"$TPASS2\"}" | head -c 150; echo
echo "== 验收脚本结束 =="
