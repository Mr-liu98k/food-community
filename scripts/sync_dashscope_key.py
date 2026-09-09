#!/usr/bin/env python
"""一次性工具：把 Windows 用户/机器级环境变量 ALIYUN_MAAS_API_KEY 写入 backend/.env 的 DASHSCOPE_API_KEY。
不在任何输出中泄露 Key 值。"""
import sys
import winreg
from pathlib import Path

ENV_FILE = Path(r"C:\新建文件夹 (2)\food-community\backend\.env")
SRC_KEY = "ALIYUN_MAAS_API_KEY"

def read_env_var():
    candidates = [
        (winreg.HKEY_CURRENT_USER, "Environment"),
        (winreg.HKEY_LOCAL_MACHINE, r"SYSTEM\CurrentControlSet\Control\Session Manager\Environment"),
    ]
    for hive, sub in candidates:
        try:
            with winreg.OpenKey(hive, sub) as k:
                try:
                    v, _ = winreg.QueryValueEx(k, SRC_KEY)
                except FileNotFoundError:
                    continue
                if v:
                    return str(v).strip()
        except OSError:
            continue
    return None

def main():
    val = read_env_var()
    if not val:
        print("NOT_FOUND: 注册表中未找到 ALIYUN_MAAS_API_KEY")
        sys.exit(1)
    if not ENV_FILE.exists():
        print("ERROR: backend/.env 不存在")
        sys.exit(1)

    lines = ENV_FILE.read_text(encoding="utf-8").splitlines()
    replaced = False
    out = []
    for line in lines:
        if line.strip().startswith("DASHSCOPE_API_KEY"):
            out.append(f"DASHSCOPE_API_KEY={val}")
            replaced = True
        else:
            out.append(line)
    if not replaced:
        out.append(f"DASHSCOPE_API_KEY={val}")
    ENV_FILE.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"OK: DASHSCOPE_API_KEY 已写入 .env（长度 {len(val)}，未打印内容）")

if __name__ == "__main__":
    main()
