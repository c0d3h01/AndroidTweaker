#!/system/bin/sh
# lib/common.sh — shared helpers for the on-device tweak engine.
# Sourced by the dispatcher, profiles and lib/tweaks modules.
# The app exports MODDIR to its extracted asset directory;
# resolve_moddir() fills it when a script is run directly.

# DRY_RUN=1: write() echoes the change instead of touching sysfs.

resolve_moddir() {
  if [ -n "$MODDIR" ] && [ -d "$MODDIR" ]; then
    return 0
  fi
  _rmd="${0%/*}"
  if [ "$_rmd" = "$0" ]; then
    _rmd="."
  fi
  _rmi=0
  while [ "$_rmi" -lt 4 ]; do
    if [ -f "$_rmd/lib/common.sh" ]; then
      MODDIR=$(cd "$_rmd" 2>/dev/null && pwd -P)
      return 0
    fi
    _rmd="$_rmd/.."
    _rmi=$((_rmi + 1))
  done
  return 1
}

log() {
  _logfile="${MODDIR:-/tmp}/tweaks.log"
  [ -f "$_logfile" ] || : >"$_logfile" 2>/dev/null
  chmod 666 "$_logfile" 2>/dev/null
  echo "$*" >>"$_logfile" 2>/dev/null
  echo "$*"
}

has() {
  [ -e "$1" ]
}

write() {
  if [ ! -f "$1" ]; then
    log "skip: $1 missing" >/dev/null
    return 1
  fi
  _curval=$(cat "$1" 2>/dev/null)
  if [ "$_curval" = "$2" ]; then
    log "skip: $1 already $2" >/dev/null
    return 1
  fi
  if [ "${DRY_RUN:-0}" = "1" ]; then
    log "dry-run: $1 $_curval -> $2"
    return 0
  fi
  chmod +w "$1" 2>/dev/null
  if ! echo "$2" >"$1" 2>/dev/null; then
    log "fail: $1 -> $2"
    return 1
  fi
  log "write: $1 $_curval -> $2"
  return 0
}

is_root() {
  [ "$(id -u 2>/dev/null)" = "0" ]
}

# Groups that only work as uid 0. Shizuku runs the engine as shell, so these
# are skipped instead of logging a wall of failed sysfs writes.
ROOT_ONLY_GROUPS="cpu gpu io sched stune thermal vm fs ksm renice kernel network"

# want <group> — false when the profile's custom conf disabled that group, or
# when the group needs root and the engine runs through Shizuku.
# Unset TWEAK_GROUPS means no conf was written, so every group applies.
want() {
  if ! is_root; then
    case " $ROOT_ONLY_GROUPS " in
    *" $1 "*)
      log "group needs root: $1"
      return 1
      ;;
    esac
  fi
  if [ -z "${TWEAK_GROUPS+set}" ]; then
    return 0
  fi
  case " $TWEAK_GROUPS " in
  *" $1 "*) return 0 ;;
  esac
  log "group off: $1"
  return 1
}

# Source the app-written overrides (group selection + value limits) for the
# profile the dispatcher is applying. Absent file keeps built-in defaults.
load_custom() {
  if [ -z "${CUSTOM_PROFILE:-}" ]; then
    return 0
  fi
  _custom_conf="$MODDIR/custom/$CUSTOM_PROFILE.conf"
  if [ -f "$_custom_conf" ]; then
    . "$_custom_conf"
    log "custom: $_custom_conf groups=[${TWEAK_GROUPS:-unset}]"
  fi
  return 0
}

# Source the app-written feature switches (charging, doze, cleanup, apps).
# This file is global: it is not tied to the profile being applied.
load_features() {
  _feature_conf="$MODDIR/custom/features.conf"
  if [ -f "$_feature_conf" ]; then
    . "$_feature_conf"
    log "features: $_feature_conf"
  fi
  return 0
}
