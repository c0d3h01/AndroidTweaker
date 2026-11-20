#!/system/bin/sh
# lib/tweaks/thermal.sh — thermal and power-efficiency toggles.
# Reads profile vars (MSM_VDD, MSM_CORE, MSM_ENABLED, WQ_EFFICIENT, SCHED_MC).

apply_thermal() {
  _S=${SYSFS_ROOT:-}
  if [ -d "$_S/sys/module/msm_thermal" ]; then
    write "$_S/sys/module/msm_thermal/vdd_restriction/enabled" "$MSM_VDD"
    write "$_S/sys/module/msm_thermal/core_control/enabled" "$MSM_CORE"
    write "$_S/sys/module/msm_thermal/parameters/enabled" "$MSM_ENABLED"
  fi
  if [ -e "$_S/sys/module/workqueue/parameters/power_efficient" ]; then
    write "$_S/sys/module/workqueue/parameters/power_efficient" "$WQ_EFFICIENT"
  fi
  if [ -e "$_S/sys/devices/system/cpu/sched_mc_power_savings" ]; then
    write "$_S/sys/devices/system/cpu/sched_mc_power_savings" "$SCHED_MC"
  fi
}

# throttle <off|on> — the FDE.AI throttling switch: properties, thermal
# services and the trip-point sysfs nodes the kernel actually reads.
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
