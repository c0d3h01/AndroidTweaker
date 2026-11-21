#!/system/bin/sh
# Software charging holds are applied only by the battery monitor.

_record_charge_node() {
  [ -f "$1" ] || return 0
  _old=$(cat "$1" 2>/dev/null) || return 0
  case "$_old" in 0 | 1) ;; *) return 0 ;; esac
  [ "$_old" != "$2" ] || return 0
  if [ "${DRY_RUN:-0}" != 1 ]; then
    printf '%s|%s|%s\n' "$1" "$_old" "$2" >> "$_restore" || return 1
  fi
  if write "$1" "$2"; then
    _held=1
  fi
}

_hold_charge() {
  [ -s "$_restore" ] && return 0
  _supply="${SYSFS_ROOT:-}/sys/class/power_supply"
  _held=0
  umask 077
  for _node in "$_supply"/*/charging_enabled "$_supply"/*/charge_enabled \
    "$_supply"/*/battery_charging_enabled "$_supply"/*/charging_enable; do
    _record_charge_node "$_node" 0 || return 1
  done
  for _node in "$_supply"/*/charge_disable "$_supply"/*/input_suspend \
    "$_supply"/battery/batt_slate_mode "$_supply"/battery/battery_input_suspend; do
    _record_charge_node "$_node" 1 || return 1
  done
  if [ "$_held" = 0 ]; then
    rm -f "$_restore"
    log "charge: no supported charging control changed"
    return 1
  fi
}

reset_charge() (
  is_root || return 1
  [ "${DRY_RUN:-0}" != 1 ] || return 0
  _restore="$MODDIR/custom/charge.restore"
  [ -s "$_restore" ] || { rm -f "$MODDIR/custom/charge.reason"; return 0; }
  umask 077
  _pending="$_restore.pending"
  : > "$_pending" || return 1
  while IFS='|' read -r _node _old _applied; do
    case "$_node" in "${SYSFS_ROOT:-}"/sys/class/power_supply/*) ;; *) continue ;; esac
    [ -f "$_node" ] || continue
    if ! _current=$(cat "$_node" 2>/dev/null) ||
      { [ "$_current" = "$_applied" ] && ! write "$_node" "$_old"; }; then
      printf '%s|%s|%s\n' "$_node" "$_old" "$_applied" >> "$_pending"
    fi
  done < "$_restore"
  if [ -s "$_pending" ]; then
    mv "$_pending" "$_restore"
    return 1
  fi
  rm -f "$_pending" "$_restore" "$MODDIR/custom/charge.reason"
)

apply_charge() (
  is_root || return 1
  _restore="$MODDIR/custom/charge.restore"
  _reason_file="$MODDIR/custom/charge.reason"
  _previous=$(cat "$_reason_file" 2>/dev/null) || _previous=
  _battery="${SYSFS_ROOT:-}/sys/class/power_supply/battery"
  _limit=${FEATURE_CHARGE_LIMIT:-}
  _fuse=${FEATURE_TEMP_FUSE:-}
  _reason=
  if [ -n "$_fuse" ]; then
    _temp=$(cat "$_battery/temp" 2>/dev/null) || _temp=
    case "$_temp" in
    '' | *[!0-9]*) [ "$_previous" = fuse ] && return 0 ;;
    *)
      if [ "$_temp" -ge $((_fuse * 10)) ] ||
        { [ "$_previous" = fuse ] && [ "$_temp" -gt $((_fuse * 10 - 20)) ]; }; then
        _reason=fuse
      fi
      ;;
    esac
  fi
  if [ -z "$_reason" ] && [ "${FEATURE_CHARGE_BYPASS:-0}" = 1 ]; then
    _reason=bypass
  fi
  if [ -z "$_reason" ] && [ -n "$_limit" ]; then
    _cap=$(cat "$_battery/capacity" 2>/dev/null) || _cap=
    case "$_cap" in
    '' | *[!0-9]*) return 0 ;;
    esac
    if [ "$_cap" -ge "$_limit" ] || { [ "$_previous" = limit ] && [ "$_cap" -gt $((_limit - 2)) ]; }; then
      _reason=limit
    fi
  fi
  if [ -z "$_reason" ]; then
    reset_charge
  else
    _hold_charge || return 1
    if [ "${DRY_RUN:-0}" != 1 ] && [ "$_reason" != "$_previous" ]; then
      printf '%s\n' "$_reason" > "$_reason_file" || return 1
      log "charge: held by $_reason"
    fi
  fi
)
