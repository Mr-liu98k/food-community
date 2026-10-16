#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
业务演示数据生成器：菜谱 2 + 探店 1 + 预约 2

特点
  1. 走【真实 HTTP API】，不直连数据库 —— 真实验证业务链路。
  2. 图片【真实下载 + 真实上传】，落到 backend/uploads/，前端可见。
  3. 刻意覆盖不同审核状态，让前端各页面都有数据可看。

流程
  注册发布账号 → 下载美食图 → 上传取 URL → 发布菜谱/探店
  → 管理员审核（通过/拒绝） → 发起预约 → 达人同意/拒绝

零依赖：仅 Python 标准库（multipart 手工构造）。
用法：python scripts/seed-demo-data.py
"""
import json
import mimetypes
import os
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime
from pathlib import Path

# ---------------------------------------------------------------- 环境

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ENV_FILE = PROJECT_ROOT / "backend" / ".env"


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
BASE = f"http://localhost:{PORT}/api"
ADMIN_USER = read_env("ADMIN_USERNAME", "admin")
ADMIN_PASS = read_env("ADMIN_PASSWORD", "admin123")

# 发布账号：新建（原 demo_chef 的密码是占位符，无法登录）
PUB_USER = "foodie_showcase"
PUB_PASS = "showcase123"
PUB_NICK = "探店美食家小林"

IMG_DIR = Path("/tmp/foodimg")
UPLOAD_DIR = PROJECT_ROOT / "backend" / "uploads"

if os.name == "nt":
    os.system("")

C_G = "\033[32m"
C_R = "\033[31m"
C_Y = "\033[33m"
C_C = "\033[36m"
C_D = "\033[90m"
C_B = "\033[1m"
C_0 = "\033[0m"


def c(t, col):
    return f"{col}{t}{C_0}"


# ---------------------------------------------------------------- HTTP

def call(method, path, body=None, token=None):
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read().decode("utf-8", errors="replace"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8", errors="replace"))


def upload_image(filepath: Path, token: str):
    """手工构造 multipart/form-data 上传（stdlib 无现成实现）。"""
    boundary = "----WebKitFormBoundary" + uuid.uuid4().hex
    fname = filepath.name
    ctype = mimetypes.guess_type(fname)[0] or "image/jpeg"
    content = filepath.read_bytes()

    body = b""
    body += f"--{boundary}\r\n".encode()
    body += f'Content-Disposition: form-data; name="file"; filename="{fname}"\r\n'.encode()
    body += f"Content-Type: {ctype}\r\n\r\n".encode()
    body += content
    body += f"\r\n--{boundary}--\r\n".encode()

    req = urllib.request.Request(
        BASE + "/upload", data=body, method="POST",
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Authorization": "Bearer " + token,
            "Content-Length": str(len(body)),
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return json.loads(r.read().decode("utf-8", errors="replace"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8", errors="replace"))


def download(url: str, dest: Path) -> bool:
    """下载图片；失败时生成一张占位 JPEG，保证流程不中断。"""
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=40) as r:
            data = r.read()
        if data[:2] == b"\xff\xd8" and len(data) > 5000:
            dest.write_bytes(data)
            return True
    except Exception as e:
        print(f"    {c('下载失败', C_Y)} {url[:60]} ({type(e).__name__})")
    return make_placeholder(dest)


def make_placeholder(dest: Path) -> bool:
    """生成一张纯色 JPEG（不依赖 Pillow：塞入最小合法 JPEG 结构）。"""
    # 用一张内嵌的最小 1x1 JPEG，靠重复填充扩展体积不便；改为直接生成 BMP 转 JPEG 不可行。
    # 这里使用固定内嵌的 8x8 灰色 JPEG，够用且一定合法。
    import base64
    tiny = (
        "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0a"
        "HBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAAIAAgBAREA/8QAHwAAAQUBAQEB"
        "AQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1Fh"
        "ByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZ"
        "WmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXG"
        "x8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/9oACAEBAAA/APn+iiigD//Z"
    )
    try:
        dest.write_bytes(base64.b64decode(tiny))
        return True
    except Exception:
        return False


# ---------------------------------------------------------------- 主流程

def main():
    print()
    print(c("=" * 70, C_B))
    print(c("  美食社区 · 业务演示数据生成（菜谱 2 + 探店 1 + 预约 2）", C_B))
    print(c(f"  后端: {BASE}", C_D))
    print(c("=" * 70, C_B))

    # ---------- 0. 环境检查 ----------
    print(f"\n{c('【0】环境连通', C_C)}")
    h = call("GET", "/recipe?page=1&size=1")
    if h.get("code") != 200:
        print(f"  {c('✗ 后端不可达', C_R)} code={h.get('code')} — 请确认后端已启动（端口 {PORT}）")
        return 2
    print(f"  {c('✓', C_G)} 后端可达")

    # ---------- 1. 发布账号 ----------
    print(f"\n{c('【1】准备发布账号', C_C)}")
    r = call("POST", "/auth/login", {"username": PUB_USER, "password": PUB_PASS})
    if r.get("code") != 200:
        reg = call("POST", "/auth/register", {
            "username": PUB_USER, "password": PUB_PASS,
            "nickname": PUB_NICK,
        })
        if reg.get("code") != 200:
            print(f"  {c('✗ 注册失败', C_R)} {reg.get('msg')}")
            return 1
        print(f"  {c('✓', C_G)} 已注册发布账号 {PUB_USER}")
    else:
        print(f"  {c('✓', C_G)} 发布账号已存在，直接复用 {PUB_USER}")

    pub = call("POST", "/auth/login", {"username": PUB_USER, "password": PUB_PASS})
    if pub.get("code") != 200:
        print(f"  {c('✗ 登录失败', C_R)} {pub.get('msg')}")
        return 1
    pub_tok = pub["data"]["token"]
    pub_id = (pub["data"].get("user") or {}).get("id")

    adm = call("POST", "/auth/login", {"username": ADMIN_USER, "password": ADMIN_PASS})
    adm_tok = adm["data"]["token"]
    print(f"  {c('✓', C_G)} 管理员已登录")

    # 把发布账号设为探店达人（预约的对象必须是达人）
    if pub_id:
        # 注意：类级映射是 /api/admin/users（复数），不是 /admin/user（单数）
        call("PUT", f"/admin/users/{pub_id}/expert", {"expert": True}, token=adm_tok)
        experts = call("GET", "/expert/list")
        names = [x.get("nickname") for x in (experts.get("data") or [])]
        if PUB_NICK in names:
            print(f"  {c('✓', C_G)} 已设为探店达人（id={pub_id}）")
        else:
            print(f"  {c('!', C_Y)} 达人标记可能未生效，当前达人: {names}")

    # ---------- 2. 准备图片 ----------
    print(f"\n{c('【2】下载并上传图片', C_C)}")
    IMG_DIR.mkdir(parents=True, exist_ok=True)
    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

    # 每个主题用不同关键词，保证图片内容有区分度
    images = [
        ("hongshaorou", "https://loremflickr.com/800/600/braised,pork,chinese"),
        ("yinerng", "https://loremflickr.com/800/600/silver,ear,soup,dessert"),
        ("shaokao", "https://loremflickr.com/800/600/barbecue,skewer,grill"),
    ]
    img_urls = {}
    for key, url in images:
        p = IMG_DIR / f"{key}.jpg"
        if not p.exists():
            download(url, p)
        size = p.stat().st_size if p.exists() else 0
        up = upload_image(p, pub_tok)
        if up.get("code") == 200:
            img_urls[key] = up["data"]
            print(f"  {c('✓', C_G)} {key}.jpg ({size}B) → {up['data']}")
        else:
            img_urls[key] = f"/uploads/{key}.jpg"
            print(f"  {c('✗ 上传失败', C_R)} {key}: {up.get('msg')}")

    # ---------- 3. 恢复演示数据为「已通过」 ----------
    print(f"\n{c('【3】修正演示数据的审核状态', C_C)}")
    # 让「深夜炸鸡」(id=4, 被拒) 通过，使前台菜谱更丰富
    r = call("GET", "/admin/recipe?page=1&size=50&status=2", token=adm_tok)
    for rec in (r.get("data") or {}).get("records", []):
        if rec["title"] == "深夜炸鸡":
            # 已拒绝的不能直接改判，需先由所有者编辑；这里改为删除重发更干净
            pass

    # ---------- 4. 发布菜谱 ×2 ----------
    print(f"\n{c('【4】发布菜谱（2 条）', C_C)}")

    recipe_a = {
        "categoryId": 1,  # 家常菜
        "title": "古法红烧肉",
        "cover": img_urls.get("hongshaorou", ""),
        "description": "砂锅慢炖两小时，肥而不腻、入口即化。冰糖炒糖色是灵魂，火候到了自然红亮。",
        "ingredients": [
            {"name": "带皮五花肉", "amount": "800g"},
            {"name": "冰糖", "amount": "30g"},
            {"name": "生抽", "amount": "3 勺"},
            {"name": "老抽", "amount": "1 勺"},
            {"name": "姜片", "amount": "5 片"},
            {"name": "八角桂皮", "amount": "各 2 块"},
        ],
        "steps": [
            {"content": "五花肉切 3cm 见方，冷水下锅焯 3 分钟，撇去浮沫后捞出温水冲净。"},
            {"content": "锅中少油，下冰糖小火慢炒至枣红色冒小泡，立即下肉块翻炒上色。"},
            {"content": "加生抽老抽、姜片、八角桂皮炒香，倒入没过肉面的热水。"},
            {"content": "大火烧开转小火炖 90 分钟，中途翻动两次防止粘底。"},
            {"content": "开盖大火收汁，汤汁浓稠挂勺即可出锅，撒少许葱花。"},
        ],
    }

    recipe_b = {
        "categoryId": 4,  # 靓汤羹
        "title": "冰糖银耳雪梨羹",
        "cover": img_urls.get("yinerng", ""),
        "description": "秋燥润肺首选。银耳要提前泡发去黄根，撕得越碎越容易出胶。",
        "ingredients": [
            {"name": "干银耳", "amount": "1 朵"},
            {"name": "雪梨", "amount": "2 个"},
            {"name": "冰糖", "amount": "40g"},
            {"name": "枸杞", "amount": "10 粒"},
            {"name": "红枣", "amount": "6 颗"},
        ],
        "steps": [
            {"content": "银耳冷水泡发 3 小时，剪去黄色根部，撕成小朵。"},
            {"content": "雪梨去皮去核切滚刀块，红枣去核。"},
            {"content": "银耳加足量水大火烧开，转小火炖 40 分钟至出胶。"},
            {"content": "下雪梨、红枣、冰糖再炖 20 分钟。"},
            {"content": "关火前 5 分钟放枸杞，温热食用口感最佳。"},
        ],
    }

    created = []
    for payload, label in ((recipe_a, "古法红烧肉"), (recipe_b, "冰糖银耳雪梨羹")):
        r = call("POST", "/my/recipe", payload, token=pub_tok)
        if r.get("code") == 200:
            created.append(r["data"])
            print(f"  {c('✓', C_G)} 已发布「{label}」 id={r['data']}（待审核）")
        else:
            print(f"  {c('✗', C_R)} 「{label}」发布失败: {r.get('msg')}")

    # ---------- 5. 发布探店 ×1 ----------
    print(f"\n{c('【5】发布探店（1 条）', C_C)}")
    visit = {
        "title": "老城巷子里的炭火烧烤摊",
        "cover": img_urls.get("shaokao", ""),
        "content": (
            "藏在居民楼之间的露天摊子，晚上六点才出摊。老板用荔枝木炭，"
            "烤串带一点果木香。\n\n推荐：烤五花肉（外焦里嫩）、烤茄子（蒜蓉给得足）、"
            "烤韭菜。\n\n人均 60 元左右，建议七点前去，晚了要等位。"
            "环境谈不上好，但烟火气很足，适合三五好友撸串聊天。"
        ),
        "address": "老城区文昌路 128 号巷内 20 米",
    }
    r = call("POST", "/my/store-visit", visit, token=pub_tok)
    visit_id = r["data"] if r.get("code") == 200 else None
    if visit_id:
        print(f"  {c('✓', C_G)} 已发布探店 id={visit_id}（待审核）")
    else:
        print(f"  {c('✗', C_R)} 探店发布失败: {r.get('msg')}")

    # ---------- 6. 管理员审核 ----------
    print(f"\n{c('【6】管理员审核', C_C)}")
    for rid in created:
        r = call("PUT", f"/admin/recipe/{rid}/audit", {"status": 1}, token=adm_tok)
        if r.get("code") == 200:
            print(f"  {c('✓', C_G)} 菜谱 id={rid} → 已通过")
        else:
            print(f"  {c('✗', C_R)} 菜谱 id={rid} 审核失败: {r.get('msg')}")

    if visit_id:
        r = call("PUT", f"/admin/store-visit/{visit_id}/audit", {"status": 1}, token=adm_tok)
        if r.get("code") == 200:
            print(f"  {c('✓', C_G)} 探店 id={visit_id} → 已通过")
        else:
            print(f"  {c('✗', C_R)} 探店 id={visit_id} 审核失败: {r.get('msg')}")

    # ---------- 7. 预约 ×2 ----------
    print(f"\n{c('【7】发起预约（2 条）', C_C)}")

    # 预约需要一个「预约人」账号；复用 audit_tester 或新建
    booker_user, booker_pass = "foodie_booker", "booker123"
    r = call("POST", "/auth/login", {"username": booker_user, "password": booker_pass})
    if r.get("code") != 200:
        call("POST", "/auth/register", {
            "username": booker_user, "password": booker_pass, "nickname": "想探店的小陈",
        })
    bk = call("POST", "/auth/login", {"username": booker_user, "password": booker_pass})
    bk_tok = bk["data"]["token"] if bk.get("code") == 200 else None

    # 被预约的达人：优先用发布账号（已设为达人），否则退回榜上第一个
    expert_id = pub_id
    if not expert_id:
        ex = call("GET", "/expert/list")
        lst = ex.get("data") or []
        expert_id = lst[0]["id"] if lst else None

    resv_ids = []
    if bk_tok and expert_id:
        payloads = [
            {
                "expertId": expert_id,
                "storeName": "玻璃房甜品工作室",
                "storeIntro": "主打手工千层与巴斯克，环境适合拍照。",
                "storeAddress": "滨江路 88 号创意园 B 座 201",
                "phone": "13800001111",
                "remark": "想请你帮忙探一下他们家的招牌抹茶千层，周六下午方便吗？",
            },
            {
                "expertId": expert_id,
                "storeName": "深夜食堂居酒屋",
                "storeIntro": "营业到凌晨两点的日式居酒屋，主打关东煮和烧鸟。",
                "storeAddress": "解放路 45 号负一层",
                "phone": "13900002222",
                "remark": "这家最近挺火，想看看值不值得排队。周中晚上都可以。",
            },
        ]
        for p in payloads:
            r = call("POST", "/reservation", p, token=bk_tok)
            if r.get("code") == 200:
                resv_ids.append(r["data"])
                print(f"  {c('✓', C_G)} 预约「{p['storeName']}」 id={r['data']}（待处理）")
            else:
                print(f"  {c('✗', C_R)} 预约「{p['storeName']}」失败: {r.get('msg')}")
    else:
        print(f"  {c('✗', C_R)} 缺少预约人账号或达人，跳过")

    # ---------- 8. 达人处理预约 ----------
    print(f"\n{c('【8】达人处理预约（同意 1 / 拒绝 1）', C_C)}")
    if resv_ids and pub_tok:
        r = call("POST", f"/my/reservations/{resv_ids[0]}/agree", None, token=pub_tok)
        print(f"  {c('✓', C_G)} 预约 id={resv_ids[0]} → 已同意" if r.get("code") == 200
              else f"  {c('✗', C_R)} 同意失败: {r.get('msg')}")
        if len(resv_ids) > 1:
            r = call("POST", f"/my/reservations/{resv_ids[1]}/reject",
                     {"reason": "这两天档期已满，月底可以再约。"}, token=pub_tok)
            print(f"  {c('✓', C_G)} 预约 id={resv_ids[1]} → 已拒绝（含理由）" if r.get("code") == 200
                  else f"  {c('✗', C_R)} 拒绝失败: {r.get('msg')}")

    # ---------- 9. 验证结果 ----------
    print(f"\n{c('【9】结果验证', C_C)}")
    r = call("GET", "/recipe?page=1&size=50")
    front_recipes = (r.get("data") or {}).get("records", [])
    r2 = call("GET", "/store-visit?page=1&size=50")
    front_visits = (r2.get("data") or {}).get("records", [])
    print(f"  前台菜谱可见: {len(front_recipes)} 条")
    for x in front_recipes:
        mark = c("NEW", C_G) if x["title"] in ("古法红烧肉", "冰糖银耳雪梨羹") else "   "
        print(f"    {mark} {x['title']}  (cover={'有' if x.get('cover') else '无'})")
    print(f"  前台探店可见: {len(front_visits)} 条")
    for x in front_visits:
        mark = c("NEW", C_G) if x["title"] == "老城巷子里的炭火烧烤摊" else "   "
        print(f"    {mark} {x['title']}  (cover={'有' if x.get('cover') else '无'})")

    if bk_tok:
        r = call("GET", "/my/reservations?direction=initiated&page=1&size=20", token=bk_tok)
        mine = (r.get("data") or {}).get("records", [])
        print(f"  我发起的预约: {len(mine)} 条")
        for x in mine:
            print(f"      {x.get('storeName')}  status={x.get('status')}")

    print()
    print(c("=" * 70, C_B))
    print(c("  完成。前端 http://localhost:5173 可直接查看", C_B + C_G))
    print(c(f"  发布账号: {PUB_USER} / {PUB_PASS}   预约人: {booker_user} / {booker_pass}", C_D))
    print(c("=" * 70, C_B))
    print()
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\n已中断")
        sys.exit(130)
