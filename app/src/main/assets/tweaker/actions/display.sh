#!/system/bin/sh
# Standalone display actions; mode-specific writes live directly in profiles/.
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
