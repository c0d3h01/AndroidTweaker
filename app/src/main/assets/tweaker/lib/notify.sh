#!/system/bin/sh
# lib/notify.sh — single notification entrypoint.
# The app shows apply state in its own UI, so engine notices go to the
# tweak log instead of spawning a second su/notification process.

notify() {
  log "notify: $*" >/dev/null
}
