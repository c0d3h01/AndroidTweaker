#!/bin/sh
set -eu

fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
MODDIR="$fixture/engine"
SYSFS_ROOT="$fixture/sysfs"
battery="$SYSFS_ROOT/sys/class/power_supply/battery"
mkdir -p "$MODDIR/custom" "$battery"

. app/src/main/assets/tweaker/core/common.sh
. app/src/main/assets/tweaker/actions/charging.sh
is_root() { return 0; }

check() {
  value=$(cat "$1")
  if [ "$value" != "$2" ]; then
    echo "Expected $1 to be $2, got $value" >&2
    exit 1
  fi
}

printf '1\n' > "$battery/charging_enabled"
printf '1\n' > "$battery/input_suspend"
printf '78\n' > "$battery/capacity"
printf '430\n' > "$battery/temp"
apply_charge
check "$battery/charging_enabled" 1
check "$battery/input_suspend" 1

FEATURE_TEMP_FUSE=42
apply_charge
check "$battery/charging_enabled" 0
printf '410\n' > "$battery/temp"
apply_charge
check "$battery/charging_enabled" 0
printf '400\n' > "$battery/temp"
apply_charge
check "$battery/charging_enabled" 1

FEATURE_TEMP_FUSE=
FEATURE_CHARGE_LIMIT=80
printf '80\n' > "$battery/capacity"
apply_charge
check "$battery/charging_enabled" 0
printf '79\n' > "$battery/capacity"
apply_charge
check "$battery/charging_enabled" 0
reset_charge
check "$battery/charging_enabled" 1
check "$battery/input_suspend" 1

printf '0\n' > "$battery/input_suspend"
FEATURE_CHARGE_BYPASS=1
apply_charge
check "$battery/input_suspend" 1
printf '2\n' > "$battery/input_suspend"
reset_charge
check "$battery/charging_enabled" 1
check "$battery/input_suspend" 2
printf '%s\n' 'Charging policy OK'
