#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
内容审核链路自动化测试（菜谱 + 探店双线）

用途
    每次改动审核相关代码后，手动跑一条命令即可回归全链路：
        python scripts/verify-audit.py

设计要点
    1. 零第三方依赖 —— 仅用 Python 标准库（urllib.request），无需 pip install。
    2. 独立隔离数据 —— 测试数据均带时间戳前缀 AUDIT-<stamp>-，不影响演示数据。
    3. 自动清理 —— 全部通过则删除测试数据；失败则保留现场便于排查。
    4. 断言真实实现 —— 每条预期文案都取自后端源码，非臆测。

覆盖矩阵
    菜谱线：提交 → 待审不可见 → 拒绝(无理由被拦/有理由成功) → 通过 → 前台可见
            → 重复审核拦截 → 编辑重置 → 拒绝理由清空 → 轮播联动
    探店线：同上（规则对称，文案不同）
    公共：参数校验(status=0/3)、鉴权(未登录/非管理员)、不存在ID
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

# ---------------------------------------------------------------- 环境配置
#
# 端口解析优先级（三级回退，正常情况无需手工传任何变量）：
#   1) 显式环境变量 BACKEND_PORT（临时覆盖用）
#   2) backend/.env 里的 SERVER_PORT  ← 项目唯一真源
#   3) 8080（application.yml 的默认值）
#
# 说明：本项目的端口统一由 backend/.env 的 SERVER_PORT 控制，
# 因此脚本直接读取同一份配置，避免"改了端口就要手工传参"的割裂。

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ENV_FILE = PROJECT_ROOT / "backend" / ".env"


def read_env_port(env_file: Path):
    """从 backend/.env 读取 SERVER_PORT。读不到返回 None。"""
    if not env_file.exists():
        return None
    try:
        for raw in env_file.read_text(encoding="utf-8", errors="replace").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            if k.strip() == "SERVER_PORT":
                v = v.strip()
                return v or None
    except OSError:
        return None
    return None


def resolve_port():
    """按三级优先级解析端口，并说明来源。"""
    if os.environ.get("BACKEND_PORT"):
        return os.environ["BACKEND_PORT"], "环境变量 BACKEND_PORT"
    p = read_env_port(ENV_FILE)
    if p:
        return p, f"{ENV_FILE.name} 的 SERVER_PORT"
    return "8080", "默认值（未找到 SERVER_PORT 配置）"


PORT, PORT_SOURCE = resolve_port()
BASE = os.environ.get("BACKEND_BASE", f"http://localhost:{PORT}/api")


def read_env_value(key, env_file=ENV_FILE):
    """从 backend/.env 读取任意键的值。"""
    if not env_file.exists():
        return None
    try:
        for raw in env_file.read_text(encoding="utf-8", errors="replace").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            if k.strip() == key:
                v = v.strip()
                return v or None
    except OSError:
        return None
    return None


# 账号：优先读 backend/.env（与后端同一份配置），保证与启动账号一致
ADMIN_USER = os.environ.get("ADMIN_USERNAME") or read_env_value("ADMIN_USERNAME") or "admin"
ADMIN_PASS = os.environ.get("ADMIN_PASSWORD") or read_env_value("ADMIN_PASSWORD") or "admin123"
USER_USER = os.environ.get("DEMO_USERNAME") or "demo_chef"
USER_PASS = os.environ.get("DEMO_PASSWORD") or "123456"

STAMP = str(int(time.time()))[-8:]          # 8 位时间戳后缀
PREFIX = f"AUDIT-{STAMP}-"                  # 所有测试数据的统一前缀

# 临时测试账号：固定用户名（不带时间戳），复用而非每次新建，避免堆积垃圾账号
TEMP_USER = "audit_tester"
TEMP_PASS = "audit123456"

# ---------------------------------------------------------------- 终端着色

if os.name == "nt":
    os.system("")  # 启用 Windows ANSI 转义支持

C_RED = "\033[31m"
C_GREEN = "\033[32m"
C_YELLOW = "\033[33m"
C_CYAN = "\033[36m"
C_GRAY = "\033[90m"
C_BOLD = "\033[1m"
C_OFF = "\033[0m"


def c(text, color):
    return f"{color}{text}{C_OFF}"


# ---------------------------------------------------------------- HTTP 客户端

class ApiError(Exception):
    """业务/传输层失败，携带足够信息用于输出。"""

    def __init__(self, msg, status=None, body=None):
        super().__init__(msg)
        self.msg = msg
        self.status = status
        self.body = body


def call(method, path, body=None, token=None, expect_http_ok=True):
    """
    发起一次 API 调用，返回解析后的响应字典 {code, msg, data}。

    注意：本项目的鉴权失败 HTTP 状态恒为 200，错误码走响应体 code 字段，
    因此这里默认不因 code != 200 抛异常，由调用方自行断言。
    """
    url = BASE + path
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    if token:
        headers["Authorization"] = "Bearer " + token

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            http_status = resp.status
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        http_status = e.code
    except urllib.error.URLError as e:
        raise ApiError(f"无法连接后端 {BASE}（{e.reason}）")

    if expect_http_ok and http_status != 200:
        raise ApiError(f"HTTP {http_status}", status=http_status, body=raw)

    if not raw:
        return {"code": None, "msg": "", "data": None, "_http": http_status}
    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError:
        raise ApiError(f"响应非 JSON（HTTP {http_status}）：{raw[:200]}",
                       status=http_status, body=raw)
    parsed["_http"] = http_status
    return parsed


def login(username, password):
    r = call("POST", "/auth/login", {"username": username, "password": password})
    if r.get("code") != 200:
        # 端口指错时，别的服务可能返回非本项目的响应结构，这里给出可诊断的提示
        if r.get("code") is None and r.get("data") is None:
            raise ApiError(
                f"登录接口返回异常结构（{r.get('msg')!r}）——"
                f"很可能 {BASE} 不是本项目后端，请用 BACKEND_PORT 指定正确端口"
            )
        raise ApiError(f"登录失败 [{username}]：{r.get('msg')}")
    return r["data"]["token"]


# ---------------------------------------------------------------- 测试框架

class Runner:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.results = []
        self.cleanup = []          # 清理回调列表
        self.current = None

    def section(self, title):
        print()
        print(c("─" * 68, C_GRAY))
        print(c(f"  {title}", C_BOLD + C_CYAN))
        print(c("─" * 68, C_GRAY))

    def case(self, name, ok, detail="", expected="", actual=""):
        if ok:
            self.passed += 1
            print(f"  {c('PASS', C_GREEN)}  {name}")
            if detail:
                print(f"        {c(detail, C_GRAY)}")
        else:
            self.failed += 1
            print(f"  {c('FAIL', C_RED)}  {name}")
            if detail:
                print(f"        {c(detail, C_RED)}")
            if expected:
                print(f"        {c('期望: ' + str(expected), C_YELLOW)}")
            if actual:
                print(f"        {c('实际: ' + str(actual), C_YELLOW)}")
        self.results.append((name, ok, detail))

    def check(self, name, cond, detail="", expected="", actual=""):
        self.case(name, bool(cond), detail if cond else "", expected, actual)
        return bool(cond)


def expect_error(runner, name, resp, want_msg, want_code=None):
    """断言响应是业务失败，且 msg 包含指定文案。"""
    code = resp.get("code")
    msg = resp.get("msg") or ""
    ok_msg = want_msg in msg
    ok_code = (want_code is None) or (code == want_code)
    ok = ok_msg and ok_code
    runner.case(
        name, ok,
        detail=f"code={code} msg={msg}" if ok else "",
        expected=f"code={want_code} 且 msg 含「{want_msg}」" if not ok else "",
        actual=f"code={code} msg={msg}" if not ok else "",
    )
    return ok


def expect_ok(runner, name, resp, detail=""):
    code = resp.get("code")
    ok = code == 200
    runner.case(
        name, ok,
        detail=detail if ok else "",
        expected="code=200" if not ok else "",
        actual=f"code={code} msg={resp.get('msg')}" if not ok else "",
    )
    return ok


# ---------------------------------------------------------------- 预备数据

def pick_category(token):
    """取一个可用分类 ID（菜谱发布必填）。"""
    r = call("GET", "/category/list", token=token)
    if r.get("code") != 200:
        r = call("GET", "/category", token=token)
    data = r.get("data")
    items = data.get("records") if isinstance(data, dict) else data
    if items:
        return items[0]["id"]
    raise ApiError("没有可用分类，无法发布菜谱（请先执行种子脚本）")


def recipe_payload(cat_id, title, desc="自动化测试数据"):
    """构造合规的菜谱发布请求体（cover 是 @NotBlank，不能为空串）。"""
    return {
        "categoryId": cat_id,
        "title": title,
        "cover": "/uploads/audit-test-placeholder.png",
        "description": desc,
        "ingredients": [{"name": "测试食材", "amount": "1 份"}],
        "steps": [{"content": "测试步骤：混合后完成。"}],
    }


def visit_payload(title, content="自动化测试探店内容"):
    return {
        "title": title,
        "cover": "/uploads/audit-test-placeholder.png",
        "content": content,
        "address": "自动化测试地址",
    }


# ---------------------------------------------------------------- 主流程

def main():
    runner = Runner()
    started = datetime.now()

    print()
    print(c("=" * 68, C_BOLD))
    print(c("  内容审核链路自动化测试  recipe + store_visit", C_BOLD))
    print(c(f"  后端: {BASE}", C_GRAY))
    print(c(f"  端口来源: {PORT_SOURCE}", C_GRAY))
    print(c(f"  数据前缀: {PREFIX}   开始: {started:%Y-%m-%d %H:%M:%S}", C_GRAY))
    print(c("=" * 68, C_BOLD))

    # ---------- 0. 连通性 & 登录 ----------
    runner.section("0. 环境连通与登录")

    try:
        health = call("GET", "/recipe?page=1&size=1")
    except ApiError as e:
        print(f"  {c('ABORT', C_RED)}  {e.msg}")
        print(f"  {c('排查建议：', C_YELLOW)}")
        print(f"  {c('  1) 后端是否已启动？启动命令见 README「本地启动」章节', C_GRAY)}")
        print(f"  {c('  2) 端口是否与 backend/.env 的 SERVER_PORT 一致？', C_GRAY)}")
        print(f"  {c(f'     当前读到: {PORT}（来源：{PORT_SOURCE}）', C_GRAY)}")
        return 2

    # 连通性断言：必须是【本项目的】后端，而不是恰好占用该端口的其他服务
    ok_health = health.get("code") == 200 and isinstance(health.get("data"), dict)
    runner.check("后端可达且为本项目服务（GET /api/recipe）", ok_health,
                 detail=f"code={health.get('code')}",
                 expected="code=200 且 data 为分页对象",
                 actual=f"code={health.get('code')} msg={health.get('msg')!r}")
    if not ok_health:
        print()
        print(f"  {c('端口 ' + str(PORT) + ' 上不是本项目后端（响应结构不匹配）。', C_RED)}")
        print(f"  {c('可能原因：该端口被其他项目占用，或本后端跑在别的端口。', C_YELLOW)}")
        print(f"  {c('排查步骤：', C_YELLOW)}")
        print(f"  {c('  1) 确认后端真实端口：查看启动日志的 “Tomcat started on port(s): XXXX”', C_GRAY)}")
        print(f"  {c('  2) 把该端口写入 backend/.env 的 SERVER_PORT，再重启后端', C_GRAY)}")
        print(f"  {c(f'  3) 或临时指定：BACKEND_PORT=实际端口 python scripts/verify-audit.py', C_GRAY)}")
        return 2

    try:
        admin_token = login(ADMIN_USER, ADMIN_PASS)
        runner.case("管理员登录", True, detail=f"用户 {ADMIN_USER}")
    except ApiError as e:
        runner.case("管理员登录", False, expected="登录成功", actual=e.msg)
        return 2

    # 普通用户：优先用种子用户，否则用固定名的临时账号（复用，不堆积）
    user_token, used_user = None, None
    for uname, upass in ((USER_USER, USER_PASS), (TEMP_USER, TEMP_PASS)):
        try:
            user_token, used_user = login(uname, upass), uname
            break
        except ApiError:
            continue

    if user_token is None:
        # 临时账号不存在 → 注册一次；此后每次运行都会走上面的登录分支复用
        reg = call("POST", "/auth/register",
                   {"username": TEMP_USER, "password": TEMP_PASS, "nickname": "审核测试账号"})
        if reg.get("code") == 200:
            try:
                user_token, used_user = login(TEMP_USER, TEMP_PASS), TEMP_USER
            except ApiError as e:
                runner.case("普通用户登录", False,
                            expected="登录成功", actual=e.msg)
                return 2
        else:
            runner.case("普通用户登录", False,
                        expected="demo 用户可登录，或可注册临时账号",
                        actual=reg.get("msg"))
            return 2

    runner.case("普通用户登录", True, detail=f"用户 {used_user}")

    # 查出临时账号的 id，供收尾时清理（仅当用的是临时账号；种子用户不动）
    temp_user_id = None
    if used_user == TEMP_USER:
        us = call("GET", "/admin/users?page=1&size=200&keyword=" + TEMP_USER,
                  token=admin_token)
        rows = (us.get("data") or {}).get("records") or []
        for u in rows:
            if u.get("username") == TEMP_USER:
                temp_user_id = u.get("id")
                break

    cat_id = pick_category(admin_token)
    runner.case("获取可用分类", True, detail=f"categoryId={cat_id}")

    # ================================================================
    # 第一部分：菜谱审核链路
    # ================================================================
    runner.section("1. 菜谱审核 —— 提交与待审隔离")

    r_title = f"{PREFIX}菜谱"
    r_created = call("POST", "/my/recipe", recipe_payload(cat_id, r_title), token=user_token)
    if not expect_ok(runner, "用户发布菜谱", r_created):
        return 1
    rid = r_created["data"]
    runner.cleanup.append(("菜谱", rid))
    runner.check(f"返回新菜谱 id={rid}", isinstance(rid, int), detail=f"id={rid}")

    # 待审：前台详情应不可见
    r_public = call("GET", f"/recipe/{rid}")
    expect_error(runner, "待审菜谱前台详情不可见", r_public,
                 want_msg="该菜谱不存在或尚未通过审核")

    # 待审：前台列表不含该菜谱
    r_list = call("GET", f"/recipe?page=1&size=100&keyword={PREFIX}")
    records = (r_list.get("data") or {}).get("records") or []
    ids = [x["id"] for x in records]
    runner.check("待审菜谱不出现在前台列表", rid not in ids,
                 detail=f"前台检索到 {len(ids)} 条，不含 id={rid}")

    # 待审：我的是可见的
    r_my = call("GET", f"/my/recipe/{rid}", token=user_token)
    expect_ok(runner, "待审菜谱在「我的菜谱」可见", r_my)
    if r_my.get("code") == 200:
        st = r_my["data"]["recipe"]["status"]
        runner.check("我的菜谱状态为待审核(0)", st == 0,
                     detail=f"status={st}", expected="0", actual=str(st))

    # 待审：管理员可通过后台列表按 status=0 查到
    r_admin0 = call("GET", f"/admin/recipe?page=1&size=100&status=0&keyword={PREFIX}",
                    token=admin_token)
    a_ids = [x["id"] for x in (r_admin0.get("data") or {}).get("records", [])]
    runner.check("待审菜谱出现在管理端 status=0 列表", rid in a_ids,
                 detail=f"管理端待审 {len(a_ids)} 条，含 id={rid}")

    # ---------- 2. 拒绝分支 ----------
    runner.section("2. 菜谱审核 —— 拒绝分支与理由校验")

    r_no_reason = call("PUT", f"/admin/recipe/{rid}/audit",
                       {"status": 2}, token=admin_token)
    expect_error(runner, "拒绝但不填理由 → 被拦截", r_no_reason,
                 want_msg="拒绝时必须填写拒绝理由", want_code=500)

    r_blank = call("PUT", f"/admin/recipe/{rid}/audit",
                   {"status": 2, "reason": "   "}, token=admin_token)
    expect_error(runner, "拒绝但理由为空白字符 → 被拦截", r_blank,
                 want_msg="拒绝时必须填写拒绝理由")

    r_reject = call("PUT", f"/admin/recipe/{rid}/audit",
                    {"status": 2, "reason": "自动化测试：内容不合规"}, token=admin_token)
    expect_ok(runner, "拒绝并填写理由 → 成功", r_reject)

    # 拒绝后：状态 2 + 理由落库
    r_after = call("GET", f"/admin/recipe/{rid}", token=admin_token)
    if r_after.get("code") == 200:
        rec = r_after["data"]["recipe"]
        runner.check("拒绝后 status=2", rec["status"] == 2,
                     detail=f"status={rec['status']}", expected="2", actual=str(rec["status"]))
        runner.check("拒绝理由已落库", rec.get("rejectReason") == "自动化测试：内容不合规",
                     detail=f"rejectReason={rec.get('rejectReason')!r}")

    r_public2 = call("GET", f"/recipe/{rid}")
    expect_error(runner, "被拒菜谱前台详情不可见", r_public2,
                 want_msg="该菜谱不存在或尚未通过审核")

    # 已拒绝的记录再次送审 → 应被拦截（审核是一次性动作，改内容才能重审）
    r_rejected_again = call("PUT", f"/admin/recipe/{rid}/audit",
                            {"status": 1}, token=admin_token)
    expect_error(runner, "已拒绝的菜谱直接改判通过 → 被拦截", r_rejected_again,
                 want_msg="该菜谱不在待审核状态，请勿重复操作")

    # ---------- 3. 通过分支（独立记录） ----------
    runner.section("3. 菜谱审核 —— 通过分支")

    rp_title = f"{PREFIX}菜谱-通过"
    rp_created = call("POST", "/my/recipe", recipe_payload(cat_id, rp_title), token=user_token)
    if not expect_ok(runner, "新建菜谱用于通过分支", rp_created):
        return 1
    rid_pass = rp_created["data"]
    runner.cleanup.append(("菜谱", rid_pass))

    r_pass = call("PUT", f"/admin/recipe/{rid_pass}/audit",
                  {"status": 1}, token=admin_token)
    expect_ok(runner, "审核通过 → 成功", r_pass)

    r_detail = call("GET", f"/admin/recipe/{rid_pass}", token=admin_token)
    if r_detail.get("code") == 200:
        rec = r_detail["data"]["recipe"]
        runner.check("通过后 status=1", rec["status"] == 1, detail=f"status={rec['status']}")
        runner.check("通过后拒绝理由为空", rec.get("rejectReason") in (None, ""),
                     detail=f"rejectReason={rec.get('rejectReason')!r}")

    r_public3 = call("GET", f"/recipe/{rid_pass}")
    expect_ok(runner, "通过后前台详情可见", r_public3)
    if r_public3.get("code") == 200:
        runner.check("前台详情返回正确标题",
                     r_public3["data"]["recipe"]["title"] == rp_title,
                     detail=f"title={r_public3['data']['recipe']['title']}")

    r_list2 = call("GET", f"/recipe?page=1&size=100&keyword={PREFIX}")
    ids2 = [x["id"] for x in (r_list2.get("data") or {}).get("records", [])]
    runner.check("通过后菜谱出现在前台列表", rid_pass in ids2, detail=f"id={rid_pass}")

    # 浏览量自增（通过后详情可访问，views 应 +1）
    v_before = call("GET", f"/admin/recipe/{rid_pass}", token=admin_token)
    before_views = v_before["data"]["recipe"]["views"] if v_before.get("code") == 200 else None
    call("GET", f"/recipe/{rid_pass}")
    v_after = call("GET", f"/admin/recipe/{rid_pass}", token=admin_token)
    after_views = v_after["data"]["recipe"]["views"] if v_after.get("code") == 200 else None
    if before_views is not None and after_views is not None:
        runner.check("前台详情访问使浏览量 +1", after_views == before_views + 1,
                     detail=f"{before_views} → {after_views}",
                     expected=str(before_views + 1), actual=str(after_views))

    # ---------- 4. 重复审核拦截 ----------
    runner.section("4. 菜谱审核 —— 重复审核与参数边界")

    r_again = call("PUT", f"/admin/recipe/{rid_pass}/audit",
                   {"status": 1}, token=admin_token)
    expect_error(runner, "已通过后重复审核 → 被拦截", r_again,
                 want_msg="该菜谱不在待审核状态，请勿重复操作")

    r_again2 = call("PUT", f"/admin/recipe/{rid_pass}/audit",
                    {"status": 2, "reason": "再次拒绝"}, token=admin_token)
    expect_error(runner, "已通过后再拒绝 → 被拦截", r_again2,
                 want_msg="该菜谱不在待审核状态，请勿重复操作")

    # status 越界：走 Bean Validation，code=400
    r_s0 = call("PUT", f"/admin/recipe/{rid_pass}/audit", {"status": 0}, token=admin_token)
    expect_error(runner, "status=0 → 参数校验拦截(400)", r_s0,
                 want_msg="审核结果只能为通过(1)或拒绝(2)", want_code=400)

    r_s3 = call("PUT", f"/admin/recipe/{rid_pass}/audit", {"status": 3}, token=admin_token)
    expect_error(runner, "status=3 → 参数校验拦截(400)", r_s3,
                 want_msg="审核结果只能为通过(1)或拒绝(2)", want_code=400)

    r_snull = call("PUT", f"/admin/recipe/{rid_pass}/audit", {}, token=admin_token)
    expect_error(runner, "status 缺省 → 参数校验拦截(400)", r_snull,
                 want_msg="审核结果不能为空", want_code=400)

    # 不存在的 ID
    r_404 = call("PUT", "/admin/recipe/99999999/audit", {"status": 1}, token=admin_token)
    expect_error(runner, "审核不存在的菜谱 → 提示不存在", r_404,
                 want_msg="菜谱不存在")

    # ---------- 5. 鉴权 ----------
    runner.section("5. 审核接口鉴权")

    r_anon = call("PUT", f"/admin/recipe/{rid_pass}/audit", {"status": 1})
    expect_error(runner, "未登录调用审核接口 → 401", r_anon,
                 want_msg="", want_code=401)

    r_nonadmin = call("PUT", f"/admin/recipe/{rid_pass}/audit", {"status": 1}, token=user_token)
    expect_error(runner, "普通用户调用审核接口 → 403", r_nonadmin,
                 want_msg="", want_code=403)

    r_anon_list = call("GET", "/admin/recipe?page=1&size=1")
    expect_error(runner, "未登录访问管理端列表 → 401", r_anon_list,
                 want_msg="", want_code=401)

    # ---------- 6. 编辑重置 ----------
    runner.section("6. 菜谱审核 —— 编辑后重置为待审核")

    # 编辑前先记录快照（views 应为此前测试累计值）
    snap = call("GET", f"/admin/recipe/{rid_pass}", token=admin_token)
    views_before_edit = snap["data"]["recipe"]["views"] if snap.get("code") == 200 else None

    r_edit = call("PUT", f"/my/recipe/{rid_pass}",
                  recipe_payload(cat_id, rp_title, "编辑后的简介"), token=user_token)
    expect_ok(runner, "用户编辑已通过的菜谱", r_edit)

    r_after_edit = call("GET", f"/admin/recipe/{rid_pass}", token=admin_token)
    if r_after_edit.get("code") == 200:
        rec = r_after_edit["data"]["recipe"]
        runner.check("编辑后 status 重置为 0", rec["status"] == 0,
                     detail=f"status={rec['status']}", expected="0", actual=str(rec["status"]))
        runner.check("编辑后 rejectReason 被清空", rec.get("rejectReason") in (None, ""),
                     detail=f"rejectReason={rec.get('rejectReason')!r}")
        if views_before_edit is not None:
            runner.check("编辑后浏览量保留（不清零）", rec["views"] == views_before_edit,
                         detail=f"views {views_before_edit} → {rec['views']}（应保持不变）",
                         expected=str(views_before_edit), actual=str(rec["views"]))

    r_hidden = call("GET", f"/recipe/{rid_pass}")
    expect_error(runner, "编辑后前台立即不可见（需重审）", r_hidden,
                 want_msg="该菜谱不存在或尚未通过审核")

    # 重新通过，供轮播联动测试
    call("PUT", f"/admin/recipe/{rid_pass}/audit", {"status": 1}, token=admin_token)

    # ---------- 7. 轮播联动 ----------
    runner.section("7. 轮播联动 —— 仅可关联已通过菜谱")

    # 先造一个待审菜谱
    r2_created = call("POST", "/my/recipe",
                      recipe_payload(cat_id, f"{PREFIX}待审菜谱"), token=user_token)
    rid2 = r2_created["data"] if r2_created.get("code") == 200 else None
    if rid2:
        runner.cleanup.append(("菜谱", rid2))

    if rid2:
        r_banner_bad = call("POST", "/admin/banner",
                            {"recipeId": rid2, "image": "/uploads/audit-banner.png", "sort": 99},
                            token=admin_token)
        expect_error(runner, "关联待审菜谱 → 被拦截", r_banner_bad,
                     want_msg="只能关联【已审核通过】的菜谱")

    r_banner_ok = call("POST", "/admin/banner",
                       {"recipeId": rid_pass, "image": "/uploads/audit-banner.png", "sort": 99},
                       token=admin_token)
    expect_ok(runner, "关联已通过菜谱 → 成功", r_banner_ok)

    # 清理轮播：找到刚建的并删除
    r_banner_page = call("GET", "/admin/banner?page=1&size=100", token=admin_token)
    if r_banner_page.get("code") == 200:
        for b in (r_banner_page.get("data") or {}).get("records", []):
            if b.get("recipeId") == rid_pass and b.get("sort") == 99:
                runner.cleanup.append(("轮播", b["id"]))
                break

    # ================================================================
    # 第二部分：探店审核链路（与菜谱对称）
    # ================================================================
    runner.section("8. 探店审核 —— 提交与待审隔离")

    s_title = f"{PREFIX}探店"
    s_created = call("POST", "/my/store-visit", visit_payload(s_title), token=user_token)
    if not expect_ok(runner, "用户发布探店", s_created):
        return 1
    sid = s_created["data"]
    runner.cleanup.append(("探店", sid))

    r_sv_public = call("GET", f"/store-visit/{sid}")
    expect_error(runner, "待审探店前台详情不可见", r_sv_public,
                 want_msg="该探店信息不存在或尚未通过审核")

    r_sv_list = call("GET", f"/store-visit?page=1&size=100&keyword={PREFIX}")
    sv_ids = [x["id"] for x in (r_sv_list.get("data") or {}).get("records", [])]
    runner.check("待审探店不出现在前台列表", sid not in sv_ids,
                 detail=f"前台探店 {len(sv_ids)} 条，不含 id={sid}")

    # ---------- 9. 探店拒绝分支 ----------
    runner.section("9. 探店审核 —— 拒绝/通过/重复审核")

    r_sv_nr = call("PUT", f"/admin/store-visit/{sid}/audit", {"status": 2}, token=admin_token)
    expect_error(runner, "探店拒绝不填理由 → 被拦截", r_sv_nr,
                 want_msg="拒绝时必须填写拒绝理由")

    r_sv_rej = call("PUT", f"/admin/store-visit/{sid}/audit",
                    {"status": 2, "reason": "自动化测试：探店信息不完整"}, token=admin_token)
    expect_ok(runner, "探店拒绝并填理由 → 成功", r_sv_rej)

    r_sv_check = call("GET", f"/admin/store-visit?page=1&size=100&status=2&keyword={PREFIX}",
                      token=admin_token)
    recs = (r_sv_check.get("data") or {}).get("records", [])
    target = next((x for x in recs if x["id"] == sid), None)
    if target:
        runner.check("探店拒绝后 status=2", target["status"] == 2,
                     detail=f"status={target['status']}")
        runner.check("探店拒绝理由落库",
                     target.get("rejectReason") == "自动化测试：探店信息不完整",
                     detail=f"rejectReason={target.get('rejectReason')!r}")

    r_sv_dup = call("PUT", f"/admin/store-visit/{sid}/audit",
                    {"status": 1}, token=admin_token)
    expect_error(runner, "已拒绝的探店再审核 → 被拦截", r_sv_dup,
                 want_msg="该探店信息不在待审核状态，请勿重复操作")

    # 编辑重置后重新通过
    r_sv_edit = call("PUT", f"/my/store-visit/{sid}",
                     visit_payload(s_title, "编辑后的探店内容"), token=user_token)
    expect_ok(runner, "用户编辑探店", r_sv_edit)

    r_sv_after = call("GET", f"/admin/store-visit?page=1&size=100&status=0&keyword={PREFIX}",
                      token=admin_token)
    recs0 = (r_sv_after.get("data") or {}).get("records", [])
    t0 = next((x for x in recs0 if x["id"] == sid), None)
    runner.check("探店编辑后重置为待审核(0)", t0 is not None,
                 detail=f"status=0 列表中{'找到' if t0 else '未找到'} id={sid}")

    r_sv_pass = call("PUT", f"/admin/store-visit/{sid}/audit",
                     {"status": 1}, token=admin_token)
    expect_ok(runner, "探店审核通过 → 成功", r_sv_pass)

    r_sv_pub2 = call("GET", f"/store-visit/{sid}")
    expect_ok(runner, "通过后探店前台详情可见", r_sv_pub2)

    # 探店参数边界
    r_sv_s0 = call("PUT", f"/admin/store-visit/{sid}/audit", {"status": 0}, token=admin_token)
    expect_error(runner, "探店 status=0 → 参数校验拦截(400)", r_sv_s0,
                 want_msg="审核结果只能为通过(1)或拒绝(2)", want_code=400)

    r_sv_404 = call("PUT", "/admin/store-visit/99999999/audit", {"status": 1}, token=admin_token)
    expect_error(runner, "审核不存在的探店 → 提示不存在", r_sv_404,
                 want_msg="探店信息不存在")

    # ================================================================
    # 第三部分：清理
    # ================================================================
    runner.section("10. 测试数据清理")

    if runner.failed == 0:
        cleaned, failed_clean = 0, []
        for kind, tid in runner.cleanup:
            path = {"菜谱": f"/admin/recipe/{tid}",
                    "探店": f"/admin/store-visit/{tid}",
                    "轮播": f"/admin/banner/{tid}"}[kind]
            r = call("DELETE", path, token=admin_token)
            if r.get("code") == 200:
                cleaned += 1
            else:
                failed_clean.append(f"{kind}#{tid}({r.get('msg')})")
        runner.case("自动清理测试数据", not failed_clean,
                    detail=f"已删除 {cleaned}/{len(runner.cleanup)} 条测试数据",
                    expected="全部删除成功", actual="; ".join(failed_clean))

        # 顺手把临时账号也删掉，让脚本对数据库零残留。
        # 注意：本脚本每次运行都会复用同一账号（audit_tester），
        # 若不清理，跑 N 次就会在用户表里永久留下一条记录。
        if not failed_clean and temp_user_id:
            rc = call("DELETE", f"/admin/users/{temp_user_id}", token=admin_token)
            if rc.get("code") == 200:
                runner.case("清理临时测试账号", True,
                            detail=f"{TEMP_USER} (id={temp_user_id}) 已删除",
                            expected="删除成功", actual="已删除")
            else:
                # 账号删除失败不该让整个用例集变红——测试数据已清干净，
                # 残留账号不影响业务，仅提示即可。
                runner.case("清理临时测试账号", True,
                            detail=f"跳过：{rc.get('msg')}（不影响业务数据）",
                            expected="删除或跳过", actual="已跳过")
    else:
        print(f"  {c('SKIP', C_YELLOW)}  存在失败用例，保留现场数据以便排查：")
        for kind, tid in runner.cleanup:
            print(f"        {kind} id={tid}")
        print(f"  {c('排查完毕可手动删除：管理端列表搜索关键字 ' + PREFIX, C_GRAY)}")

    # ---------- 汇总 ----------
    elapsed = (datetime.now() - started).total_seconds()
    total = runner.passed + runner.failed
    print()
    print(c("=" * 68, C_BOLD))
    if runner.failed == 0:
        print(c(f"  全部通过：{runner.passed}/{total}    耗时 {elapsed:.1f}s", C_BOLD + C_GREEN))
    else:
        print(c(f"  通过 {runner.passed}/{total}，失败 {runner.failed}    耗时 {elapsed:.1f}s",
                C_BOLD + C_RED))
        print()
        print(c("  失败用例：", C_RED))
        for name, ok, _ in runner.results:
            if not ok:
                print(c(f"    · {name}", C_RED))
    print(c("=" * 68, C_BOLD))
    print()
    return 0 if runner.failed == 0 else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except ApiError as e:
        print()
        print(c(f"  ABORT  {e.msg}", C_RED))
        if e.body:
            print(c(f"         {str(e.body)[:300]}", C_GRAY))
        print()
        sys.exit(2)
    except KeyboardInterrupt:
        print()
        print(c("  已中断", C_YELLOW))
        sys.exit(130)
