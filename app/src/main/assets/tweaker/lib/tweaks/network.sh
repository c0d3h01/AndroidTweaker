#!/system/bin/sh
# lib/tweaks/network.sh — TCP stack, Wi-Fi sleep and background scanning.
# apply_network is the profile group (sysfs, root only); the switches below
# run through `settings`, so they also work with Shizuku.

apply_network() {
  _S=${SYSFS_ROOT:-}
  write "$_S/proc/sys/net/ipv4/tcp_fin_timeout" "${NET_TCP_FIN:-30}"
  write "$_S/proc/sys/net/ipv4/tcp_tw_reuse" "${NET_TCP_TW_REUSE:-1}"
  write "$_S/proc/sys/net/ipv4/tcp_keepalive_time" "${NET_TCP_KEEPALIVE:-600}"
  write "$_S/proc/sys/net/ipv4/tcp_rmem" "${NET_TCP_RMEM:-4096 87380 6291456}"
  write "$_S/proc/sys/net/ipv4/tcp_wmem" "${NET_TCP_WMEM:-4096 87380 6291456}"
  write "$_S/proc/sys/net/core/rmem_max" "${NET_RMEM_MAX:-6291456}"
  write "$_S/proc/sys/net/core/wmem_max" "${NET_WMEM_MAX:-6291456}"
}

# apply_network_settings — feature switches shared by every profile.
apply_network_settings() {
  if [ -n "${FEATURE_SCAN_ALWAYS:-}" ]; then
    settings put global wifi_scan_always_enabled "$FEATURE_SCAN_ALWAYS" >/dev/null 2>&1
    settings put global ble_scan_always_enabled "$FEATURE_SCAN_ALWAYS" >/dev/null 2>&1
  fi
  if [ -n "${FEATURE_WIFI_SLEEP:-}" ]; then
    # 2 = keep Wi-Fi awake while plugged in, 0 = system default.
    _policy=0
    [ "$FEATURE_WIFI_SLEEP" = "1" ] && _policy=2
    settings put global wifi_sleep_policy "$_policy" >/dev/null 2>&1
  fi
}
