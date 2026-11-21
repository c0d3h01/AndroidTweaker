#!/system/bin/sh
# actions/maintenance.sh — first-boot maintenance: fstrim set + dexopt props.
# Ex-V1tweaker() minus its notification (caller notifies).

apply_misc() {
  if [ "${DRY_RUN:-0}" = "1" ]; then
    log "dry-run: fstrim /system /data /cache /vendor /product + dexopt resetprops"
    return 0
  fi
  for _p in /system /data /cache /vendor /product; do
    fstrim "$_p" >/dev/null 2>&1
  done
  if command -v resetprop >/dev/null 2>&1; then
    resetprop persist.bg.dexopt.enable true
    resetprop pm.dexopt.bg-dexopt everything
    resetprop pm.dexopt.forced-dexopt everything
  else
    setprop persist.bg.dexopt.enable true
    setprop pm.dexopt.bg-dexopt everything
    setprop pm.dexopt.forced-dexopt everything
  fi
}
