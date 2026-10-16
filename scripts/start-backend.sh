#!/usr/bin/env bash
# =============================================================
# 后端启动器（Windows / Git Bash / MSYS2）
#
# 为什么需要这个脚本？——本机踩过的两个坑，都在这里一次性封死：
#
#   坑 1：精简 jar 陷阱
#     若 target/*.jar 只有几百 KB，说明只跑了 jar:jar，没跑
#     spring-boot:repackage，jar 里没有 BOOT-INF/lib/。
#     这种 jar 启动后，依赖类会在运行期才报 NoClassDefFoundError
#     （例如上传时找不到 StandardMultipartHttpServletRequest，
#      前端看到的是「系统繁忙」）。
#     → 本脚本会先校验 jar 体积，过小则强制重新打包。
#
#   坑 2：SERVER__PORT 环境变量干扰
#     Spring Boot 宽松绑定会把 SERVER__PORT（双下划线）识别为
#     server.port，且【环境变量优先级高于 application.yml】，
#     于是 .env 里的 SERVER_PORT 被静默覆盖，端口跑到别处。
#     → 本脚本启动前会 unset 掉这些会干扰的变量。
#
# 用法：
#   bash scripts/start-backend.sh          # 直接启动
#   bash scripts/start-backend.sh --rebuild # 强制重新打包后启动
# =============================================================
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$ROOT/backend"
ENV_FILE="$BACKEND/.env"
JAR="$BACKEND/target/food-community-backend-0.0.1-SNAPSHOT.jar"

REBUILD=0
[ "${1:-}" = "--rebuild" ] && REBUILD=1

# ---------- 读取端口（与前端、各验收脚本共用 backend/.env 这一份真源）----------
ENV_PORT=""
if [ -f "$ENV_FILE" ]; then
  ENV_PORT=$(grep -E '^SERVER_PORT=' "$ENV_FILE" | head -1 | cut -d= -f2 | tr -d '[:space:]')
fi
PORT="${BACKEND_PORT:-${ENV_PORT:-8080}}"

echo "======================================================================"
echo "  美食社区 · 后端启动器"
echo "  端口来源: ${BACKEND_PORT:+.env 覆盖（BACKEND_PORT）}${BACKEND_PORT:-backend/.env 的 SERVER_PORT}"
echo "  启动端口: $PORT"
echo "======================================================================"

# ---------- 坑 2：清理会干扰端口解析的环境变量 ----------
for v in SERVER__PORT SERVER_PORT SPRING_APPLICATION_JSON; do
  if [ -n "${!v:-}" ]; then
    echo "  ! 检测到环境变量 $v=${!v}（会覆盖 .env），已解除"
    unset "$v"
  fi
done

# ---------- 打包 ----------
need_build=0
if [ "$REBUILD" = "1" ] || [ ! -f "$JAR" ]; then
  need_build=1
else
  # 校验是不是完整的 Spring Boot fat jar（必须含 BOOT-INF/lib）
  if ! unzip -l "$JAR" 2>/dev/null | grep -q "BOOT-INF/lib/spring-web-"; then
    size=$(du -k "$JAR" 2>/dev/null | cut -f1)
    echo "  ! 现有 jar 不完整（${size}KB，缺少 BOOT-INF/lib）——这就是「系统繁忙」类错误的根源"
    need_build=1
  fi
fi

if [ "$need_build" = "1" ]; then
  echo ""
  echo ">> 正在打包（mvn clean package -DskipTests），请稍候..."
  ( cd "$BACKEND" && mvn -q -DskipTests clean package ) || {
    echo "!! 打包失败，请检查上方 Maven 输出"; exit 1;
  }
  echo ">> 打包完成: $(ls -lh "$JAR" | awk '{print $5}')"
fi

# ---------- 端口占用检查 ----------
if netstat -ano 2>/dev/null | grep -q ":$PORT .*LISTENING"; then
  pid=$(netstat -ano 2>/dev/null | grep ":$PORT .*LISTENING" | head -1 | awk '{print $NF}')
  echo ""
  echo "!! 端口 $PORT 已被进程 $pid 占用"
  echo "   如确认是旧后端：taskkill /PID $pid /F"
  echo "   如想换端口：改 backend/.env 的 SERVER_PORT"
  exit 1
fi

# ---------- 启动 ----------
echo ""
echo ">> 启动后端（Ctrl+C 停止）..."
echo "   连通性自检: curl http://localhost:$PORT/api/recipe?page=1&size=1"
echo "   （本项目没有 /api/health 接口，用菜谱列表探活）"
echo ""
cd "$BACKEND" && exec java -jar "$JAR"
