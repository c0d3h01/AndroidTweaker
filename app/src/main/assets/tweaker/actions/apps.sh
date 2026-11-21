#!/system/bin/sh
# actions/apps.sh — per-app profiles through Android's own game mode.
# Heavy apps get performance mode, light apps get battery mode, and the heavy
# list is also published as the RT package blacklist (as FDE.AI does).

apply_apps() {
  for _pkg in ${FEATURE_HEAVY:-}; do
    cmd game mode performance "$_pkg" >/dev/null 2>&1
    log "apps: $_pkg -> performance"
  done
  for _pkg in ${FEATURE_LIGHT:-}; do
    cmd game mode battery "$_pkg" >/dev/null 2>&1
    log "apps: $_pkg -> battery"
  done
  if [ -n "${FEATURE_HEAVY:-}" ]; then
    settings put system rt_pkg_black_list "$FEATURE_HEAVY" >/dev/null 2>&1
  fi
}

# apps_reset — drop every per-app override this engine may have written.
apps_reset() {
  for _pkg in ${FEATURE_HEAVY:-} ${FEATURE_LIGHT:-}; do
    cmd game mode standard "$_pkg" >/dev/null 2>&1
  done
  settings delete system rt_pkg_black_list >/dev/null 2>&1
  log "apps: per-app profiles reset"
}
