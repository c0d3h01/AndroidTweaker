#!/system/bin/sh
MODDIR=${0%/*}

# Wait until the device has finished booting
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 5
done

# Give the system a few extra seconds to settle
sleep 30

if [ -f "$MODDIR/AndroidTweaker.apk" ]; then
    pm install -r -g "$MODDIR/AndroidTweaker.apk" >/dev/null 2>&1
    rm -f "$MODDIR/AndroidTweaker.apk"
fi

"$MODDIR/system/bin/ATweaker" >/dev/null 2>&1
exit 0
