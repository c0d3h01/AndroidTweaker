#!/system/bin/sh
# Standalone network actions; mode-specific writes live directly in profiles/.
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
