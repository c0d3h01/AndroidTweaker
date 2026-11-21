#!/system/bin/sh
# lib/tweaks/display.sh — double-tap-to-wake, LCD, idle sleep, touch boost.
# DT2W writes are identical everywhere (hardcoded); the rest reads profile
# vars (LCD_POWER, PM2_IDLE optional, TOUCHBOOST, BATTERY_SAVER and
# ARCH_POWER optional).

apply_display() {
  _S=${SYSFS_ROOT:-}
  if [ -e "$_S/sys/touchpanel/double_tap" ] && [ -e "$_S/proc/tp_gesture" ]; then
    write "$_S/sys/touchpanel/double_tap" "1"
    write "$_S/proc/tp_gesture" "1"
  elif [ -e "$_S/proc/tp_gesture" ]; then
    write "$_S/proc/tp_gesture" "1"
  elif [ -e "$_S/sys/touchpanel/double_tap" ]; then
    write "$_S/sys/touchpanel/double_tap" "1"
  fi

  if [ -e "$_S/sys/module/msm_performance/parameters/touchboost" ]; then
    write "$_S/sys/module/msm_performance/parameters/touchboost" "$TOUCHBOOST"
  elif [ -e "$_S/sys/power/pnpmgr/touch_boost" ]; then
    write "$_S/sys/power/pnpmgr/touch_boost" "$TOUCHBOOST"
  fi

  if [ -n "${BATTERY_SAVER:-}" ] && [ -d "$_S/sys/module/battery_saver" ]; then
    write "$_S/sys/module/battery_saver/parameters/enabled" "$BATTERY_SAVER"
  fi

  if [ -n "${ARCH_POWER:-}" ] && [ -e "$_S/sys/kernel/sched/arch_power" ]; then
    write "$_S/sys/kernel/sched/arch_power" "$ARCH_POWER"
  fi

  if [ -n "${PM2_IDLE:-}" ] && [ -e "$_S/sys/module/pm2/parameters/idle_sleep_mode" ]; then
    write "$_S/sys/module/pm2/parameters/idle_sleep_mode" "$PM2_IDLE"
  fi

  if [ -e "$_S/sys/class/lcd/panel/power_reduce" ]; then
    write "$_S/sys/class/lcd/panel/power_reduce" "$LCD_POWER"
  fi
}

# set_resolution <default|low|high|max|16:9> — wm size/density profiles.
set_resolution() {
  _nat=$(wm size 2>/dev/null | awk '/Physical size/ {print $3}')
  case "$1" in
  default)
    wm size reset >/dev/null 2>&1
    wm density reset >/dev/null 2>&1
    ;;
  low)
    [ -n "$_nat" ] || return 1
    _w=${_nat%x*}
    _h=${_nat#*x}
    wm size "$((_w * 3 / 4))x$((_h * 3 / 4))" >/dev/null 2>&1
    ;;
  high | max)
    [ -n "$_nat" ] || return 1
    wm size "$_nat" >/dev/null 2>&1
    wm density reset >/dev/null 2>&1
    ;;
  16:9)
    [ -n "$_nat" ] || return 1
    _w=${_nat%x*}
    _h=${_nat#*x}
    wm size "$((_h * 9 / 16))x$_h" >/dev/null 2>&1
    ;;
  *)
    return 1
    ;;
  esac
  log "display: resolution $1"
}

# set_peak_fps <n|0> — cap the refresh rate instead of fighting the GPU.
set_peak_fps() {
  if [ "${1:-0}" = "0" ]; then
    settings delete system peak_refresh_rate >/dev/null 2>&1
    settings delete system min_refresh_rate >/dev/null 2>&1
    log "display: refresh rate restored"
    return 0
  fi
  settings put system peak_refresh_rate "$1" >/dev/null 2>&1
  settings put system min_refresh_rate "$1" >/dev/null 2>&1
  log "display: refresh capped at $1 Hz"
}
