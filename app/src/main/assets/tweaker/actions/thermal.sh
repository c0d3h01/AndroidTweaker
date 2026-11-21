#!/system/bin/sh
# Standalone thermal actions; mode-specific writes live directly in profiles/.
THERMAL_SERVICES="thermald vendor.thermald mi_thermald thermal vendor.thermal thermal-engine vendor.thermal-engine thermal_manager vendor.thermal_manager thermalserviced vendor.thermalserviced thermalloadalgod"

throttle() {
  _S=${SYSFS_ROOT:-}
  case "$1" in
  off)
    for _p in sys.thermal.enable persist.sys.thermal.enable; do
      setprop "$_p" false >/dev/null 2>&1
      resetprop "$_p" false >/dev/null 2>&1
    done
    for _s in $THERMAL_SERVICES; do
      stop "$_s" >/dev/null 2>&1
    done
    for _t in "$_S"/sys/class/thermal/thermal_zone*/mode; do
      [ -e "$_t" ] && write "$_t" "disabled"
    done
    log "thermal: throttling disabled"
    ;;
  on)
    for _p in sys.thermal.enable persist.sys.thermal.enable; do
      setprop "$_p" true >/dev/null 2>&1
      resetprop "$_p" true >/dev/null 2>&1
    done
    for _s in $THERMAL_SERVICES; do
      start "$_s" >/dev/null 2>&1
    done
    for _t in "$_S"/sys/class/thermal/thermal_zone*/mode; do
      [ -e "$_t" ] && write "$_t" "enabled"
    done
    log "thermal: throttling enabled"
    ;;
  esac
}
