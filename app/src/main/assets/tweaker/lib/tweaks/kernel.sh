#!/system/bin/sh
# lib/tweaks/kernel.sh — CPU bandwidth, cgroup shares, entropy, wakelocks.
# These are the "core tweaks" FDE applies on top of a governor change.
# Every value is optional; missing profile vars fall back to the defaults here.

apply_kernel() {
  _S=${SYSFS_ROOT:-}

  # CPU bandwidth: let cpufreq react to I/O wait and keep boost flags explicit.
  for _n in "$_S"/sys/devices/system/cpu/cpu*/cpufreq/* "$_S"/sys/devices/system/cpu/cpufreq/*; do
    [ -e "${_n}/boost" ] && write "${_n}/boost" "${KERNEL_CPUFREQ_BOOST:-1}"
    [ -e "${_n}/io_is_busy" ] && write "${_n}/io_is_busy" "${KERNEL_IO_IS_BUSY:-1}"
    [ -e "${_n}/iowait_boost_enable" ] && write "${_n}/iowait_boost_enable" "${KERNEL_IOWAIT_BOOST:-1}"
    [ -e "${_n}/sampling_rate" ] && write "${_n}/sampling_rate" "${KERNEL_SAMPLING_RATE:-20000}"
    [ -e "${_n}/timer_slack" ] && write "${_n}/timer_slack" "${KERNEL_TIMER_SLACK:-10000}"
  done

  # EAS capacity boost (WALT kernels).
  write "$_S/proc/sys/kernel/sched_cfs_boost" "${KERNEL_CFS_BOOST:-0}"

  # cgroup CPU shares: keep visible work ahead of background work.
  for _g in "$_S"/dev/cpuctl/top-app "$_S"/dev/cpuctl/foreground \
    "$_S"/dev/cpuctl/system-background "$_S"/dev/cpuctl/background \
    "$_S"/dev/cpuctl/bg_*; do
    [ -e "$_g/cpu.shares" ] || continue
    case "$_g" in
    *background*) write "$_g/cpu.shares" "${KERNEL_SHARES_BG:-22}" ;;
    *) write "$_g/cpu.shares" "${KERNEL_SHARES_FG:-52}" ;;
    esac
  done

  # Entropy: raise the wakeup watermarks so reads do not stall.
  write "$_S/proc/sys/kernel/random/read_wakeup_threshold" "${KERNEL_ENTROPY_READ:-128}"
  write "$_S/proc/sys/kernel/random/write_wakeup_threshold" "${KERNEL_ENTROPY_WRITE:-512}"

  # Networking and Wi-Fi wakelocks are pure battery drain on idle profiles.
  if [ "${KERNEL_WAKELOCKS:-1}" = "0" ]; then
    for _w in "$_S"/sys/module/wakeup/parameters/enable_wlan*_ws \
      "$_S"/sys/module/wakeup/parameters/enable_qcom_rx_wakelock_ws; do
      [ -e "$_w" ] && write "$_w" "N"
    done
  fi

  # Printk spam costs wakeups; keep kernel messages off the console.
  write "$_S/proc/sys/kernel/printk_devkmsg" "off"
}
