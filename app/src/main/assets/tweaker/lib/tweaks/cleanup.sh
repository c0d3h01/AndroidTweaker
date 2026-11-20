#!/system/bin/sh
# lib/tweaks/cleanup.sh — RAM, cache, log and app-library maintenance.
# Everything here is reversible except bloatware removal, which the app
# gates behind an explicit confirmation.

clean_ram() {
  _S=${SYSFS_ROOT:-}
  am kill-all >/dev/null 2>&1
  write "$_S/proc/sys/vm/drop_caches" "3"
  write "$_S/proc/sys/vm/compact_memory" "1"
  log "clean: RAM reclaimed"
}

clean_cache() {
  pm trim-caches 999G >/dev/null 2>&1
  for _d in /data/local/tmp /data/anr /data/tombstones /data/dropbox /data/system/dropbox; do
    rm -rf "$_d"/* >/dev/null 2>&1
  done
  log "clean: caches and crash logs trimmed"
}

clean_logs() {
  logcat -b all -c >/dev/null 2>&1
  logcat -G 128K >/dev/null 2>&1
  log "clean: log buffers cleared"
}

# clean_art — recompile the app dex files with Android's own compiler.
clean_art() {
  cmd package compile -m speed-profile -f -a >/dev/null 2>&1
  log "clean: ART dex recompiled (speed-profile)"
}

# remove_bloat — uninstall packages for the main device user.
remove_bloat() {
  _failed=0
  for _pkg in ${FEATURE_BLOAT:-}; do
    if pm uninstall -k --user 0 "$_pkg"; then
      log "clean: removed $_pkg for user 0"
    else
      log "clean: could not remove $_pkg for user 0"
      _failed=1
    fi
  done
  return "$_failed"
}

restore_app() {
  [ -n "${1:-}" ] || return 1
  cmd package install-existing --user 0 "$1" || return 1
  log "clean: restored $1 for user 0"
}

# apply_cleanup — the always-on part of the cleanup group.
apply_cleanup() {
  if [ -n "${FEATURE_PHANTOM_LIMIT:-}" ]; then
    settings put global settings_enable_monitor_phantom_procs true >/dev/null 2>&1
    device_config put activity_manager max_phantom_processes "$FEATURE_PHANTOM_LIMIT" >/dev/null 2>&1
    log "clean: phantom process limit $FEATURE_PHANTOM_LIMIT"
  fi
  if [ "${FEATURE_DISABLE_LOGS:-0}" = "1" ]; then
    for _p in debug.hwui.skia_atrace_enabled=false persist.traced.enable=0 \
      persist.sys.watchdog_enhanced=false persist.sys.oom_crash_on_watchdog=false \
      persist.vendor.ssr.enable_ramdumps=0 dalvik.vm.dex2oat-minidebuginfo=false \
      dalvik.vm.minidebuginfo=false; do
      setprop "${_p%%=*}" "${_p#*=}" >/dev/null 2>&1
    done
    logcat -G 128K >/dev/null 2>&1
    log "clean: debug logging reduced"
  fi
}
