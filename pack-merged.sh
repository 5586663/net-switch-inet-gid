#!/bin/sh
# 把 CI 编译出的 zygisk zip 与本地 module/ 合并成单一 Magisk 模块。
# 用法：./pack-merged.sh path/to/net-switch-ZYGISK-*.zip
#
# 编译 zygisk 需要 JDK 21 + Android SDK，本地无此环境时走 GitHub Actions：
#   git push 后从 Actions artifacts 下载 net-switch-zygisk-raw

set -e

ZYGISK_ZIP="$1"
if [ -z "$ZYGISK_ZIP" ] || [ ! -f "$ZYGISK_ZIP" ]; then
    echo "用法: $0 <net-switch-ZYGISK-*.zip>" >&2
    exit 1
fi

WORK="/tmp/ns-merge-$$"
rm -rf "$WORK"
mkdir -p "$WORK/base" "$WORK/zygisk"

# 1. net-switch 原模块为基底
cp -r module/. "$WORK/base/"

# 2. 解开 zygisk 产物
unzip -q "$ZYGISK_ZIP" -d "$WORK/zygisk"

# 3. 叠加 .so / dex / dexopt
cp -r "$WORK/zygisk/zygisk" "$WORK/base/" 2>/dev/null || true
cp "$WORK/zygisk/classes.dex" "$WORK/base/" 2>/dev/null || true
cp -r "$WORK/zygisk/dexopt" "$WORK/base/" 2>/dev/null || true

# 4. 校验
[ -f "$WORK/base/zygisk/arm64-v8a.so" ] || { echo "FATAL: zygisk .so 缺失" >&2; exit 1; }
[ -f "$WORK/base/service.sh" ]         || { echo "FATAL: service.sh 缺失" >&2; exit 1; }
[ -f "$WORK/base/module.prop" ]        || { echo "FATAL: module.prop 缺失" >&2; exit 1; }

# 5. 打包
OUT="$(pwd)/net-switch-1.4-fusion.zip"
( cd "$WORK/base" && zip -r -9 "$OUT" . -x '.*' >/dev/null )
rm -rf "$WORK"

echo "已生成: $OUT"
unzip -l "$OUT" | tail -20
