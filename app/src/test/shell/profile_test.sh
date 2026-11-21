#!/bin/sh
set -eu
engine=${PROFILE_TEST_ENGINE:-app/src/main/assets/tweaker}
engine=$(cd "$engine" && pwd -P)
fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
# Representative legacy Android kernel nodes. All commands below are mocked;
# this fixture never touches the host's kernel, settings or processes.
for node in \
  sys/devices/system/cpu/cpu1/cpufreq/scaling_available_governors \
  dev/stune/foreground/schedtune.boost \
  sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors \
  sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq \
  sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq \
  sys/devices/system/cpu/cpu0/core_ctl/enable \
  sys/devices/system/cpu/cpuidle/use_deepest_state \
  sys/module/cpu_boost/parameters/input_boost_ms \
  sys/module/cpu_input_boost/parameters/input_boost_duration \
  sys/module/adreno_idler/parameters/adreno_idler_active \
  sys/module/msm_thermal/parameters/enabled \
  sys/module/workqueue/parameters/power_efficient \
  sys/devices/system/cpu/sched_mc_power_savings \
  sys/module/msm_performance/parameters/touchboost \
  sys/module/battery_saver/parameters/enabled \
  sys/kernel/sched/arch_power \
  sys/module/pm2/parameters/idle_sleep_mode \
  sys/class/lcd/panel/power_reduce \
  dev/cpuctl/foreground/cpu.shares \
  dev/cpuctl/background/cpu.shares \
  sys/block/sda/queue/scheduler \
  sys/block/sda/queue/read_ahead_kb; do
  mkdir -p "$fixture/${node%/*}"
  printf 'schedutil interactive\n' > "$fixture/$node"
done
printf 'interactive\n' > "$fixture/sys/devices/system/cpu/cpu1/cpufreq/scaling_available_governors"
mkdir -p "$fixture/gpu" "$fixture/gpu/devfreq"
printf '5\n' > "$fixture/gpu/max_pwrlevel"
run_profile() (
  mode=$1
  variant=$2
  MODDIR=$engine
  SYSFS_ROOT=$fixture
  CPU_MAXFREQ=3000000 CPU_HALF=1500000
  GPU_DIR=$fixture/gpu GPU_DEVFREQ=$fixture/gpu/devfreq
  GPU_MAXFREQ=900000000 GPU_TOPMIN_FREQ=300000000 GPU_HALF=450000000 GPU_MINPL=5
  MPDECISION=start
  . "$engine/${PROFILE_TEST_CORE:-core}/common.sh"
  # Stable command traces let the direct scripts be compared with the previous
  # implementation. No command can reach a real device in this harness.
  write() { printf 'write|%s|%s\n' "${1#"$fixture"}" "$2"; }
  log() { printf 'log|%s\n' "$*"; }
  settings() { printf 'settings|%s\n' "$*"; }
  dumpsys() { printf 'dumpsys|%s\n' "$*"; }
  renice() { printf 'renice|%s\n' "$*"; }
  pgrep() { printf '100\n'; }
  sync() { :; }
  sysctl() { printf 'sysctl|%s\n' "$*"; }
  start() { printf 'start|%s\n' "$*"; }
  stop() { printf 'stop|%s\n' "$*"; }
  load_custom() {
    if [ "$variant" = overrides ]; then
      CPU_MAX_FREQ=1200000 GPU_MIN_FREQ_SET=200000000
      VM_SWAPPINESS=60 CPU_BOOST_FREQ=
    fi
  }
  want() { [ "$variant" != disabled ]; }
  if [ -n "${PROFILE_TEST_LEGACY:-}" ]; then
    for module in "$engine"/tuning/*/*.sh; do . "$module"; done
    . "$engine/runtime/profile.sh"
  else
    . "$engine/actions/doze.sh"
  fi
  . "$engine/profiles/$mode.sh"
  apply_profile
)
for mode in battery balanced performance; do
  for variant in defaults overrides disabled; do
    run_profile "$mode" "$variant" > "$fixture/trace"
    actual=$(cksum < "$fixture/trace")
    if [ -n "${PROFILE_TEST_RECORD:-}" ]; then
      printf '%s %s %s\n' "$mode" "$variant" "$actual"
    else
      expected=$(awk -v m="$mode" -v v="$variant" '$1 == m && $2 == v {print $3, $4}' app/src/test/shell/fixtures/profile-traces.cksum)
      [ "$actual" = "$expected" ] || { echo "Profile trace changed: $mode $variant ($actual != $expected)" >&2; exit 1; }
    fi
  done
done
[ -n "${PROFILE_TEST_RECORD:-}" ] || echo 'Direct profile traces OK (defaults, overrides, disabled groups)'
