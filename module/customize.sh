# net-switch 数据源：与已装 v1.2 / WebUI / CLI 保持一致（旧路径）
ISOLATED="/data/adb/net-switch/isolated.json"
NEW_PATH="/data/adb/.config/net-switch/isolated.json"
SYNC_DIR="/data/system/net-switch"

# 迁移：若旧路径无数据但新路径有（从 v1.3 降级回来），搬回旧路径
if [ ! -f "$ISOLATED" ] && [ -f "$NEW_PATH" ]; then
	mkdir -p "$(dirname "$ISOLATED")"
	cp "$NEW_PATH" "$ISOLATED"
fi

# 保证数据目录与文件存在
mkdir -p "$(dirname "$ISOLATED")"
if [ ! -f "$ISOLATED" ]; then
	touch "$ISOLATED"
fi

# 初始化 system_server 可读目录（ZygoteHook 从这里读名单）
mkdir -p "$SYNC_DIR"
if [ -f "$ISOLATED" ]; then
	cp "$ISOLATED" "$SYNC_DIR/isolated.json"
fi
chown system:system "$SYNC_DIR" "$SYNC_DIR/isolated.json" 2>/dev/null
chmod 755 "$SYNC_DIR"
chmod 644 "$SYNC_DIR/isolated.json" 2>/dev/null

if [ "$KSU" = "true" ] || [ "$APATCH" = "true" ]; then
	rm "$MODPATH/action.sh"
	touch "$MODPATH/skip_mount"
	manager_paths="/data/adb/ap/bin /data/adb/ksu/bin"
	for dir in $manager_paths; do
		if [ -d "$dir" ]; then
			echo "- creating symlink in $dir"
			ln -sf /data/adb/modules/net-switch/system/bin/netswitch "$dir/netswitch"
		fi
	done
fi

set_perm_recursive "$MODPATH/system" 0 0 0755 0755
