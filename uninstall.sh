#!/system/bin/sh
# Uninstall the companion app, then revert backed-up files.
MODDIR=${0%/*}

# Read the companion package name from module.prop.
pkg=$(sed -n 's/^companionPkg=//p' "$MODDIR/module.prop" 2>/dev/null | head -n 1)
[ -n "$pkg" ] && pm uninstall "$pkg" >/dev/null 2>&1

# Don't modify anything after this
if [ -f $INFO ]; then
  while read LINE; do
    if [ "$(echo -n $LINE | tail -c 1)" == "~" ]; then
      continue
    elif [ -f "$LINE~" ]; then
      mv -f $LINE~ $LINE
    else
      rm -f $LINE
      while true; do
        LINE=$(dirname $LINE)
        [ "$(ls -A $LINE 2>/dev/null)" ] && break 1 || rm -rf $LINE
      done
    fi
  done <$INFO
  rm -f $INFO
fi
