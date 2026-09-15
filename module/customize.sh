# net-switch 数据路径：与已装 v1.2 / WebUI / CLI 保持一致（旧路径）
ISOLATED="/data/adb/net-switch/isolated.json"
NEW_PATH="/data/adb/.config/net-switch/isolated.json"

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

if [ "$KSU" = "true" ] || [ "$APATCH" = "true" ]; then
	# remove action on APatch / KernelSU
	rm "$MODPATH/action.sh"
	# skip mount on APatch / KernelSU
	touch "$MODPATH/skip_mount"
	# symlink ourselves on $PATH
	manager_paths="/data/adb/ap/bin /data/adb/ksu/bin"
	for dir in $manager_paths; do
		if [ -d "$dir" ]; then
			echo "- creating symlink in $dir"
			ln -sf /data/adb/modules/net-switch/system/bin/netswitch "$dir/netswitch"
		fi
	done
fi

set_perm_recursive "$MODPATH/system" 0 0 0755 0755
