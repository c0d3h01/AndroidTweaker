#!/system/bin/sh
modpath="/data/adb/modules_update/AXI/"
INJECTOR=$MODPATH/system/bin/

h01print() {
  ui_print ""
  ui_print "*******************************************"
  ui_print "*          Android Tweaker                *"
  ui_print "*  Unlocking Your Device's Potential      *"
  ui_print "*******************************************"
  ui_print ""
  ui_print "[ * ] Version: 1.4.9"
  ui_print ""
  ui_print "[ * ] Supercharge Your Device's Performance"
  ui_print ""
  ui_print "[ * ] Devloped by @c0d3h01 - telegram"
  ui_print ""
  ui_print "[ * ] If you found any bug give report Devloper"
  ui_print ""
  ui_print "[ * ] Channel - @c0d3h01prjts is only official channel!"
  ui_print ""
}

h01xz() {
  [[ "$IS64BIT" == "true" ]] && tar -xf "$MODPATH/injector2.tar.xz" -C "$MODPATH" || tar -xf "$MODPATH/injector1.tar.xz" -C "$MODPATH"
  ui_print " Injecting Components"
  mkdir -p $INJECTOR
  mv -f $MODPATH/injector/ATweaker $INJECTOR/ATweaker
  ui_print "- Injector Successfully Injected ✓"
  ui_print ""
}
h01xz
h01print
note="Boost your device's performance and enjoy a smoother experience with our optimizer, designed specifically for $(getprop ro.product.device)!"
sed -i "/description=/c description=$note" "${modpath}module.prop"
nohup am start -a android.intent.action.VIEW -d https://t.me/c0d3h01prjts >/dev/null 2>&1 &
>/dev/null 2>&1 &
