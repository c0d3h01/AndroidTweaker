#!/system/bin/sh
# Standalone doze actions; mode-specific writes live directly in profiles/.
DOZE_CONSTANTS="inactive_to=60000,sensing_to=0,locating_to=0,location_accuracy=2000,motion_inactive_to=0,idle_after_inactive_to=0,idle_pending_to=60000,max_idle_pending_to=120000,idle_pending_factor=2.0,idle_to=900000,max_idle_to=21600000,idle_factor=2.0,max_temp_app_whitelist_duration=60000,mms_temp_app_whitelist_duration=30000,sms_temp_app_whitelist_duration=20000,light_after_inactive_to=10000,light_pre_idle_to=60000,light_idle_to=180000,light_idle_factor=2.0,light_max_idle_to=900000,light_idle_maintenance_min_budget=30000,light_idle_maintenance_max_budget=60000"



apply_doze_features() {
  for _pkg in ${FEATURE_DOZE_WHITELIST:-}; do
    dumpsys deviceidle whitelist "+$_pkg" >/dev/null 2>&1
  done
  if [ "${FEATURE_FORCE_DOZE:-0}" = "1" ]; then
    log "doze: force idle armed (applies on screen off)"
  fi
}

doze_force() {
  if [ "${FEATURE_FORCE_DOZE:-0}" != "1" ]; then
    return 0
  fi
  dumpsys deviceidle force-idle >/dev/null 2>&1
  log "doze: forced idle"
}

doze_unforce() {
  dumpsys deviceidle unforce >/dev/null 2>&1
}

doze_restore() {
  settings delete global device_idle_constants >/dev/null 2>&1
  dumpsys deviceidle unforce >/dev/null 2>&1
  log "doze: restored stock timings"
}
