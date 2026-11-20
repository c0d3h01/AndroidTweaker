#!/system/bin/sh
# lib/dispatcher.sh — CLI parse + route for the app's on-device engine.
# The app exports MODDIR to its extracted assets and runs:
#   MODDIR=<dir> sh <dir>/lib/dispatcher.sh apply <profile>
#   MODDIR=<dir> sh <dir>/lib/dispatcher.sh feature
#   MODDIR=<dir> sh <dir>/lib/dispatcher.sh charge|doze|throttle|clean|apps|...
# There is no daemon or property watcher; the app applies profiles directly.
# Under Shizuku the same script runs as uid 2000: `settings`, `cmd`, `pm` and
# `dumpsys` keep working, sysfs groups are skipped by want().

if [ -z "${MODDIR:-}" ] || [ ! -f "$MODDIR/lib/common.sh" ]; then
  MODDIR=$(cd "${0%/*}/.." 2>/dev/null && pwd -P)
fi
export MODDIR

. "$MODDIR/lib/common.sh"
. "$MODDIR/lib/notify.sh"
. "$MODDIR/lib/detect.sh"
. "$MODDIR/lib/tweaks/renice.sh"
. "$MODDIR/lib/tweaks/cpu.sh"
. "$MODDIR/lib/tweaks/gpu.sh"
. "$MODDIR/lib/tweaks/io.sh"
. "$MODDIR/lib/tweaks/sched.sh"
. "$MODDIR/lib/tweaks/stune.sh"
. "$MODDIR/lib/tweaks/kernel.sh"
. "$MODDIR/lib/tweaks/thermal.sh"
. "$MODDIR/lib/tweaks/vm.sh"
. "$MODDIR/lib/tweaks/fs.sh"
. "$MODDIR/lib/tweaks/display.sh"
. "$MODDIR/lib/tweaks/ksm.sh"
. "$MODDIR/lib/tweaks/charging.sh"
. "$MODDIR/lib/tweaks/doze.sh"
. "$MODDIR/lib/tweaks/network.sh"
. "$MODDIR/lib/tweaks/cleanup.sh"
. "$MODDIR/lib/tweaks/apps.sh"
. "$MODDIR/lib/tweaks/misc.sh"

usage() {
  cat >&2 <<'EOF'
usage: dispatcher.sh <command>
  apply <battery|balanced|performance|gaming>
  feature                     apply the global feature switches
  charge [reset]              enforce or release the monitor's charging hold
  doze [force|restore]        idle timings, forced idle, or restore
  throttle <on|off>           thermal throttling
  clean <ram|cache|logs|art>  one-shot maintenance
  apps | apps-reset           per-app game-mode profiles
  bloat                       remove the selected bloatware for user 0
  bg-kill | power-save on|off | datasaver on|off
  restore <pkg>               reinstall an app removed for the user
  resolution <default|low|high|16:9>
  fps <n|0>                   refresh-rate cap
  misc
EOF
  exit 1
}

# Global switches applied after every profile.
apply_features() {
  apply_doze_features
  apply_cleanup
  apply_network_settings
  apply_apps
  if [ "${FEATURE_THROTTLE_OFF:-0}" = "1" ]; then
    is_root && throttle off
  else
    is_root && throttle on
  fi
  [ "${RESOLUTION:-default}" != "default" ] && set_resolution "$RESOLUTION"
  [ -n "${PEAK_FPS:-}" ] && [ "$PEAK_FPS" != "0" ] && set_peak_fps "$PEAK_FPS"
  return 0
}

do_apply() {
  case "$1" in
  battery | balanced | performance | gaming) ;;
  *)
    usage
    ;;
  esac
  notify "applying : $1"
  detect_devices
  CUSTOM_PROFILE="$1"
  . "$MODDIR/profiles/$1.sh"
  if want system; then
    case "$1" in
    battery) settings put global low_power 1 ;;
    *) settings put global low_power 0 ;;
    esac
  fi
  apply_features
  notify "Applied : $1"
}

case "${1:-}" in
apply)
  [ -n "${2:-}" ] || usage
  load_features
  do_apply "$2"
  ;;
feature)
  load_features
  apply_features
  notify "Features applied"
  ;;
charge)
  load_features
  case "${2:-apply}" in
  apply) apply_charge ;;
  reset) reset_charge ;;
  *) usage ;;
  esac
  ;;
doze)
  load_features
  case "${2:-apply}" in
  force) doze_force ;;
  unforce) doze_unforce ;;
  restore) doze_restore ;;
  *) apply_doze_features ;;
  esac
  ;;
throttle)
  load_features
  case "${2:-}" in
  on | off) throttle "$2" ;;
  *) usage ;;
  esac
  ;;
clean)
  load_features
  case "${2:-}" in
  ram) clean_ram ;;
  cache) clean_cache ;;
  logs) clean_logs ;;
  art) clean_art ;;
  *) usage ;;
  esac
  ;;
apps)
  load_features
  apply_apps
  ;;
apps-reset)
  load_features
  apps_reset
  ;;
bloat)
  load_features
  remove_bloat
  ;;
bg-kill)
  am kill-all >/dev/null 2>&1
  ;;
power-save)
  settings put global low_power "$([ "${2:-}" = "on" ] && echo 1 || echo 0)" >/dev/null 2>&1
  ;;
datasaver)
  cmd netpolicy set restrict-background "$([ "${2:-}" = "on" ] && echo true || echo false)" >/dev/null 2>&1
  ;;
restore)
  load_features
  restore_app "${2:-}"
  ;;
resolution)
  set_resolution "${2:-default}"
  ;;
fps)
  set_peak_fps "${2:-0}"
  ;;
misc)
  load_features
  apply_misc
  notify "Misc Tweaks successfully executed"
  ;;
*)
  usage
  ;;
esac
