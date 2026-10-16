@echo off
REM ============================================================
REM 内容审核链路自动化测试 —— Windows 一键入口
REM
REM 用法：双击，或在项目根目录执行  scripts\verify-audit.bat
REM
REM 端口无需在此指定：脚本会自动读取 backend\.env 的 SERVER_PORT。
REM 仅当需要临时覆盖时才设置，例如：
REM     set BACKEND_PORT=9090 && scripts\verify-audit.bat
REM ============================================================

setlocal
cd /d "%~dp0.."

REM 中文输出需要 UTF-8，否则 Windows 控制台会乱码
set PYTHONIOENCODING=utf-8

echo.

REM 依次尝试 python / python3 / py（部分环境只装了 py 启动器）
where python >nul 2>nul
if %errorlevel%==0 (
    python scripts\verify-audit.py
    goto :done
)

where python3 >nul 2>nul
if %errorlevel%==0 (
    python3 scripts\verify-audit.py
    goto :done
)

where py >nul 2>nul
if %errorlevel%==0 (
    py scripts\verify-audit.py
    goto :done
)

echo   [错误] 未找到 Python，请先安装 Python 3 并加入 PATH。
echo          下载：https://www.python.org/downloads/
echo.
pause

:done
set EXITCODE=%errorlevel%
endlocal & exit /b %EXITCODE%
