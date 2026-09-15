#!/bin/sh
# Wait until boot completed
until [ "$(getprop sys.boot_completed)" = "1" ] && [ -f /data/system/packages.list ]; do
	sleep 1
done

SRC="/data/adb/net-switch/isolated.json"
SYNC_DIR="/data/system/net-switch"
SYNC_FILE="$SYNC_DIR/isolated.json"

# 同步名单到 system_server 可读位置
# /data/system 是 system_data_file context，system_server 日常读取（如 appops.xml）
sync_isolated() {
	[ -f "$SRC" ] || return
	mkdir -p "$SYNC_DIR"
	cp "$SRC" "$SYNC_FILE"
	chown system:system "$SYNC_DIR" "$SYNC_FILE"
	chmod 755 "$SYNC_DIR"
	chmod 644 "$SYNC_FILE"
	echo "net-switch: synced isolated.json to $SYNC_FILE" >>/dev/kmsg
}

sync_isolated

# iptables 层：按名单 REJECT
packages="$(sed 's|[]\"[]||g; s|,| |g' "$SRC")"
for apk in $packages; do
	uid="$(grep "^$apk" /data/system/packages.list | awk '{print $2; exit}')"
	[ ! -z $uid ] && {
		iptables -I OUTPUT -m owner --uid-owner $uid -j REJECT
		ip6tables -I OUTPUT -m owner --uid-owner $uid -j REJECT
		echo "net-switch: blocked $apk with uid: $uid" >>/dev/kmsg
	}
done

# 监听名单变化，10 秒轮询同步（WebUI/CLI 改动后自动生效）
while true; do
	sleep 10
	sync_isolated
done
