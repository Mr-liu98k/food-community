#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
设计 token 完整性校验
--------------------------------------------------------------
扫描全部样式，确认每一个 var(--fc-*) 引用都能在 index.css 中找到定义。

为什么需要这个校验？
  CSS 变量不存在时【不会报错】，而是静默失效：
    color: var(--fc-nonexistent);   → 该声明整体降级为 unset（文字变继承色）
  这类问题构建能通过、页面能打开，只在肉眼观察时才可能发现。
  本项目历史上就出现过一次：`var(--fc-primary)` 被引用 3 次却从未定义，
  导致品牌名与导航激活态颜色失效、管理端菜单高亮背景变透明。

用法：
  python scripts/check-design-tokens.py
退出码：0 = 全部有定义；1 = 存在悬空引用
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CSS_FILE = ROOT / 'src' / 'styles' / 'index.css'

if not CSS_FILE.exists():
    print('找不到 %s' % CSS_FILE)
    sys.exit(2)

css = CSS_FILE.read_text(encoding='utf-8')
defined = set(re.findall(r'(--fc-[a-z0-9-]+)\s*:', css))

used = {}
for f in list((ROOT / 'src').rglob('*.vue')) + list((ROOT / 'src').rglob('*.css')):
    text = f.read_text(encoding='utf-8')
    for name in re.findall(r'var\((--fc-[a-z0-9-]+)', text):
        used.setdefault(name, set()).add(str(f.relative_to(ROOT)).replace('\\', '/'))

missing = sorted(set(used) - defined)
unused = sorted(defined - set(used))

print('=' * 64)
print('  设计 token 完整性校验')
print('=' * 64)
print('  token 定义    : %d' % len(defined))
print('  token 被引用  : %d（共 %d 处）' % (len(used), sum(len(v) for v in used.values())))
print()

if missing:
    print('  ✗ 以下 var() 引用没有定义 —— 会导致该声明静默失效：')
    for m in missing:
        files = ', '.join(sorted(used[m])[:3])
        more = ' 等 %d 个文件' % len(used[m]) if len(used[m]) > 3 else ''
        print('      %-28s <- %s%s' % (m, files, more))
    print()
    print('  修复方式：在 src/styles/index.css 的 :root 中补上定义，')
    print('            或把调用方改为已有的等价 token。')
    print()
    print('=' * 64)
    print('  结果：失败（%d 个悬空引用）' % len(missing))
    print('=' * 64)
    sys.exit(1)

print('  ✓ 无悬空引用：全部 var(--fc-*) 都能解析到定义')
print()
if unused:
    print('  未被引用的 token（预留或已废弃，非错误）: %d 个' % len(unused))
    for u in unused:
        print('      %s' % u)
    print()
print('=' * 64)
print('  结果：通过')
print('=' * 64)
sys.exit(0)
