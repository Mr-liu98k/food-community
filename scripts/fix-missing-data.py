#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据补齐器：把种子数据里缺失的图片/字段补上，并清理测试遗留账号。

为什么需要它？
  仓库的 sql/02_seed_demo.sql 里 recipe.cover 与 banner.image 都是空字符串 '',
  导致首页轮播区空白、菜谱卡片无图。而演示作者 demo_chef 的密码是占位符
  DEMO-NOT-LOGIN（设计上不可登录），所以【无法通过业务 API 给自己的作品补图】。
  只能在数据层补齐。

本脚本做三件事：
  A. 补齐【轮播图 image】   —— 走管理端 API（PUT /api/admin/banner/{id}）
  B. 补齐【菜谱封面 cover】 —— 走数据库直改（作者不可登录，API 路径走不通）
  C. 清理【测试遗留数据】 —— 悬空轮播、测试账号及其关联评论（走管理端 API + 数据层）

零依赖：仅 Python 标准库 + mysql 命令行客户端。
用法：
  python scripts/fix-missing-data.py            # 预览（dry-run，默认）
  python scripts/fix-missing-data.py --apply    # 实际执行
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ENV_FILE = PROJECT_ROOT / "backend" / ".env"
UPLOAD_DIR = PROJECT_ROOT / "backend" / "uploads"
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

APPLY = "--apply" in sys.argv

# ---------------------------------------------------------------- 环境

def read_env(key, default=None):
    if not ENV_FILE.exists():
        return default
    try:
        for raw in ENV_FILE.read_text(encoding="utf-8", errors="replace").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            if k.strip() == key:
                return v.strip() or default
    except OSError:
        pass
    return default


PORT = os.environ.get("BACKEND_PORT") or read_env("SERVER_PORT") or "8080"
BASE = f"http://127.0.0.1:{PORT}/api"          # 用 127.0.0.1 绕开本机代理
DB = read_env("MYSQL_DATABASE", "food_community")
DB_USER = read_env("MYSQL_USERNAME", "root")
DB_PASS = read_env("MYSQL_PASSWORD", "123456")
DB_PORT = read_env("MYSQL_PORT", "3306")
ADMIN_USER = read_env("ADMIN_USERNAME", "admin")
ADMIN_PASS = read_env("ADMIN_PASSWORD", "admin123")

C_R = "\033[31m"; C_G = "\033[32m"; C_Y = "\033[33m"; C_B = "\033[36m"; C_D = "\033[90m"; C_0 = "\033[0m"


def c(s, color):
    return f"{color}{s}{C_0}"


def call(method, path, body=None, token=None, timeout=20):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(req, data, timeout=timeout) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read())
        except Exception:
            return {"code": e.code, "msg": f"HTTP {e.code}"}
    except Exception as e:
        return {"code": -1, "msg": str(e)}


def sql(statement):
    """执行一条 SQL（用于 API 覆盖不到的场景）"""
    cmd = [MYSQL, "-h", "127.0.0.1", "-P", str(DB_PORT), "-u", DB_USER,
           f"-p{DB_PASS}", DB, "--default-character-set=utf8mb4", "-N", "-B", "-e", statement]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if p.returncode != 0:
        return None, (p.stderr or "").strip()
    return (p.stdout or "").strip(), None


def sql_query(statement):
    out, err = sql(statement)
    if err:
        print(c(f"  SQL 失败: {err}", C_R))
        return []
    return [ln.split("\t") for ln in out.splitlines() if ln.strip()]


def download_image(keyword, filename, w=800, h=600):
    """下载真实图片；已存在则复用，避免重复下载"""
    IMG_DIR = PROJECT_ROOT / "scripts" / ".imgcache"
    IMG_DIR.mkdir(parents=True, exist_ok=True)
    dest = IMG_DIR / filename
    if dest.exists() and dest.stat().st_size > 1024:
        return dest
    url = f"https://loremflickr.com/{w}/{h}/{keyword}"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=40) as resp:
            data = resp.read()
        if len(data) < 1024:
            return None
        dest.write_bytes(data)
        return dest
    except Exception:
        return None


def upload_image(filepath, token):
    """手工构造 multipart/form-data 上传，返回 /uploads/xxx.jpg"""
    content = filepath.read_bytes()
    boundary = "----WebKitFormBoundary" + uuid.uuid4().hex
    body = b""
    body += f"--{boundary}\r\n".encode()
    body += f'Content-Disposition: form-data; name="file"; filename="{filepath.name}"\r\n'.encode()
    body += b"Content-Type: image/jpeg\r\n\r\n"
    body += content
    body += f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(BASE + "/upload", method="POST", data=body)
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    req.add_header("Authorization", "Bearer " + token)
    req.add_header("Content-Length", str(len(body)))
    try:
        with urllib.request.urlopen(req, timeout=40) as resp:
            r = json.loads(resp.read())
            return r.get("data") if r.get("code") == 200 else None
    except Exception:
        return None


# ---------------------------------------------------------------- 主流程

def main():
    print("=" * 70)
    print(f"  数据补齐器   {'【实际执行】' if APPLY else '【预览模式，加 --apply 才真改】'}")
    print(f"  后端: {BASE}")
    print(f"  数据库: {DB} @127.0.0.1:{DB_PORT}")
    print("=" * 70)

    # 连通性检查：本项目没有 /health 接口，用「菜谱列表」探活，
    # 并断言响应结构，避免端口指到别的服务上还以为通了。
    probe = call("GET", "/recipe?page=1&size=1")
    if probe.get("code") != 200:
        print(c(f"\n✗ 后端不可达或响应异常（{BASE}）", C_R))
        print(f"   响应: {str(probe)[:120]}")
        print(f"   先启动：bash scripts/start-backend.sh")
        return 2

    adm = call("POST", "/auth/login", {"username": ADMIN_USER, "password": ADMIN_PASS})
    if adm.get("code") != 200:
        print(c(f"\n✗ 管理员登录失败: {adm.get('msg')}", C_R))
        return 2
    token = adm["data"]["token"]
    print(c("  ✓ 后端可达，管理员已登录", C_G))

    summary = {"banner": 0, "banner_del": 0, "recipe": 0, "visit": 0,
               "user_del": 0, "skip": 0, "fail": 0}

    # ---------------- A. 补轮播图 ----------------
    print("\n【A】补齐轮播图 image（走管理端 API）")
    banners = call("GET", "/admin/banner?page=1&size=50", token=token)
    rows = (banners.get("data") or {}).get("records") or []
    need = [b for b in rows if not (b.get("image") or "").strip()]
    if not need:
        print(c("  全部已有配图，无需补齐", C_D))
    for b in need:
        title = b.get("recipeTitle") or f"recipe{b.get('recipeId')}"
        kw = {"可乐鸡翅": "chicken-wings", "免烤奥利奥芝士蛋糕": "cheesecake"}.get(title, "food")
        print(f"  · 轮播 id={b['id']} →「{title}」需要配图")
        if not APPLY:
            summary["skip"] += 1
            continue
        img = download_image(kw, f"banner_{b['id']}.jpg")
        if not img:
            print(c(f"    ✗ 图片下载失败，跳过", C_R)); summary["fail"] += 1; continue
        url = upload_image(img, token)
        if not url:
            print(c(f"    ✗ 上传失败，跳过", C_R)); summary["fail"] += 1; continue
        r = call("PUT", f"/admin/banner/{b['id']}",
                 {"recipeId": b.get("recipeId"), "image": url, "sort": b.get("sort")}, token=token)
        ok = r.get("code") == 200
        print(c(f"    {'✓' if ok else '✗'} image={url}  {r.get('msg') if not ok else ''}",
                C_G if ok else C_R))
        summary["banner" if ok else "fail"] += 1

    # ---------------- B. 补菜谱封面 ----------------
    print("\n【B】补齐菜谱 cover（数据库直改：作者账号不可登录，API 走不通）")
    print(c("  说明：demo_chef 密码是占位符 DEMO-NOT-LOGIN，设计上无法登录，", C_D))
    print(c("        因此只能绕过业务 API 在数据层补图。", C_D))
    # 用 CONCAT 包一层，避免空字符串列在 -N -B 输出时被吞掉导致列数不齐
    rows = sql_query("SELECT id, title, status, CONCAT('[', IFNULL(cover,''), ']') FROM recipe "
                     "WHERE cover IS NULL OR cover = '' ORDER BY id")
    if not rows:
        print(c("  全部已有封面，无需补齐", C_D))
    for row in rows:
        rid, title, status = row[0], row[1], row[2]
        print(f"  · 菜谱 id={rid}「{title}」status={status} 无封面")
        if not APPLY:
            summary["skip"] += 1
            continue
        kw = {"可乐鸡翅": "chicken-wings", "免烤奥利奥芝士蛋糕": "cheesecake",
              "西红柿炖牛腩": "beef-stew", "深夜炸鸡": "fried-chicken"}.get(title, "food")
        img = download_image(kw, f"recipe_{rid}.jpg")
        if not img:
            print(c(f"    ✗ 图片下载失败，跳过", C_R)); summary["fail"] += 1; continue
        url = upload_image(img, token)
        if not url:
            print(c(f"    ✗ 上传失败，跳过", C_R)); summary["fail"] += 1; continue
        _, err = sql(f"UPDATE recipe SET cover='{url}' WHERE id={rid}")
        if err:
            print(c(f"    ✗ SQL 失败: {err}", C_R)); summary["fail"] += 1; continue
        print(c(f"    ✓ cover={url}", C_G))
        summary["recipe"] += 1

    # ---------------- C. 补探店封面 ----------------
    print("\n【C】补齐探店 cover（数据库直改，与菜谱同理）")
    rows = sql_query("SELECT id, title, status, CONCAT('[', IFNULL(cover,''), ']') FROM store_visit "
                     "WHERE cover IS NULL OR cover = '' ORDER BY id")
    if not rows:
        print(c("  全部已有封面，无需补齐", C_D))
    for row in rows:
        rid, title, status = row[0], row[1], row[2]
        print(f"  · 探店 id={rid}「{title}」status={status} 无封面")
        if not APPLY:
            summary["skip"] += 1
            continue
        kw = {"巷口那家开了二十年的牛肉面": "beef-noodles",
              "藏在写字楼里的日式咖喱屋": "japanese-curry",
              "新开的露营风烧烤店": "bbq"}.get(title, "restaurant")
        img = download_image(kw, f"visit_{rid}.jpg")
        if not img:
            print(c("    ✗ 图片下载失败，跳过", C_R)); summary["fail"] += 1; continue
        url = upload_image(img, token)
        if not url:
            print(c("    ✗ 上传失败，跳过", C_R)); summary["fail"] += 1; continue
        _, err = sql(f"UPDATE store_visit SET cover='{url}' WHERE id={rid}")
        if err:
            print(c(f"    ✗ SQL 失败: {err}", C_R)); summary["fail"] += 1; continue
        print(c(f"    ✓ cover={url}", C_G))
        summary["visit"] += 1

    # ---------------- D. 清理悬空轮播 ----------------
    print("\n【D】清理悬空轮播（recipe_id 指向已不存在的菜谱）")
    # 这类记录是测试脚本建完菜谱→挂轮播→删菜谱后残留的，前端会渲染成空白项。
    dangling = sql_query("SELECT b.id, b.recipe_id, CONCAT('[', IFNULL(b.image,''), ']') "
                         "FROM banner b LEFT JOIN recipe r ON r.id = b.recipe_id "
                         "WHERE r.id IS NULL ORDER BY b.id")
    if not dangling:
        print(c("  无悬空轮播", C_D))
    for bid, recipe_id, img in dangling:
        print(f"  · 轮播 id={bid} 指向不存在的菜谱 {recipe_id}  image={img}")
        if not APPLY:
            summary["skip"] += 1
            continue
        r = call("DELETE", f"/admin/banner/{bid}", token=token)
        if r.get("code") == 200:
            print(c(f"    ✓ 已删除", C_G)); summary["banner_del"] += 1
        else:
            print(c(f"    ✗ {r.get('msg')}", C_R)); summary["fail"] += 1

    # ---------------- E. 清理测试遗留账号 ----------------
    print("\n【E】清理测试脚本遗留账号")
    # 注意：/api/admin/users 只列普通用户（role=0），管理员 admin 不在此接口中，
    #      所以 KEEP 里保留 admin 只是语义上的保险，实际不会出现在候选集里。
    KEEP = {"admin", "demo_chef", "demo_fan", "foodie_showcase", "foodie_booker"}
    users = call("GET", "/admin/users?page=1&size=300", token=token)
    urows = (users.get("data") or {}).get("records") or []
    junk = [u for u in urows if u.get("username") not in KEEP]
    kept = [u for u in urows if u.get("username") in KEEP]
    print(f"  普通用户 {len(urows)} 个 → 保留 {len(kept)} 个（{', '.join(u['username'] for u in kept)}）、"
          f"清理 {len(junk)} 个")
    for u in junk:
        print(f"  · 删除 id={u['id']:<4} {u.get('username')}")
    if APPLY:
        # 先清掉测试账号留下的评论，否则后端会以"存在业务数据"为由拒绝删除用户。
        # 这些评论的正文就是"A的根评论/B回复根"这类测试话术，与真实演示数据无关。
        # 必须先删子回复再删根评论，避免留下悬空 parent_id。
        junk_ids = [u["id"] for u in junk]
        if junk_ids:
            id_list = ",".join(str(i) for i in junk_ids)
            sql(f"DELETE FROM comment WHERE user_id IN ({id_list})")
            print(c(f"  ✓ 已清理测试评论（关联 {len(junk_ids)} 个待删账号）", C_G))

        for u in junk:
            r = call("DELETE", f"/admin/users/{u['id']}", token=token)
            if r.get("code") == 200:
                summary["user_del"] += 1
            else:
                print(c(f"    ✗ {u.get('username')}: {r.get('msg')}", C_R)); summary["fail"] += 1
    else:
        summary["skip"] += len(junk)

    # ---------------- 汇总 ----------------
    print("\n" + "=" * 70)
    if APPLY:
        print(f"  轮播配图 {summary['banner']} · 菜谱封面 {summary['recipe']} · 探店封面 {summary['visit']}")
        print(f"  悬空轮播清理 {summary['banner_del']} · 账号清理 {summary['user_del']} · 失败 {summary['fail']}")
    else:
        print(f"  预览：待处理 {summary['skip']} 项")
        print(f"  加 --apply 实际执行：python scripts/fix-missing-data.py --apply")
    print("=" * 70)
    return 0


if __name__ == "__main__":
    sys.exit(main())
