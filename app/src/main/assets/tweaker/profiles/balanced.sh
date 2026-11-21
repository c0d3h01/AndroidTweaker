#!/system/bin/sh
# Balanced mode: direct tweak operations, executed by the dispatcher.
# Read top to bottom to see exactly which nodes this mode requests.
# core/common.sh supplies write(), want() and load_custom(); detect.sh supplies limits.
# ${NAME-default} uses a saved override when set, otherwise the inline mode default.
# Missing nodes are skipped by write(); a requested value is not proof of a device benefit.

apply_profile() {
  load_custom

  # Process priority: give UI services more CPU scheduling preference.
  # This changes contention priority, not CPU speed; background work may wait longer.
  if want renice; then
    renice -n -5 "$(pgrep system_server)" >/dev/null 2>&1
    renice -n -5 "$(pgrep com.miui.home)" >/dev/null 2>&1
    renice -n -5 "$(pgrep launcher)" >/dev/null 2>&1
    renice -n -5 "$(pgrep lawnchair)" >/dev/null 2>&1
    renice -n -5 "$(pgrep home)" >/dev/null 2>&1
    renice -n -5 "$(pgrep watchapp)" >/dev/null 2>&1
    renice -n -5 "$(pgrep trebuchet)" >/dev/null 2>&1
    renice -n -1 "$(pgrep dialer)" >/dev/null 2>&1
    renice -n -1 "$(pgrep keyboard)" >/dev/null 2>&1
    renice -n -1 "$(pgrep inputmethod)" >/dev/null 2>&1
    renice -n -9 "$(pgrep fluid)" >/dev/null 2>&1
    renice -n -10 "$(pgrep composer)" >/dev/null 2>&1
    renice -n -1 "$(pgrep com.android.phone)" >/dev/null 2>&1
    renice -n -10 "$(pgrep surfaceflinger)" >/dev/null 2>&1
    renice -n 1 "$(pgrep kswapd0)" >/dev/null 2>&1
    renice -n 1 "$(pgrep ksmd)" >/dev/null 2>&1
    renice -n -6 "$(pgrep msm_irqbalance)" >/dev/null 2>&1
    renice -n -9 "$(pgrep kgsl_worker)" >/dev/null 2>&1
    renice -n 6 "$(pgrep android.gms)" >/dev/null 2>&1
  fi

  # CPU clocks and governors: set ramp-up delays, boost duration and frequency bounds.
  # Battery favors lower clocks; Performance favors response at higher energy cost.
  # Only supported governor nodes are touched; frequency values are in kHz.
  if want cpu; then
    _S=${SYSFS_ROOT:-}
    for _cpu in "$_S"/sys/devices/system/cpu/cpu*/cpufreq/; do
      if [ ! -e "${_cpu}scaling_available_governors" ]; then
        continue
      fi
      _avail=$(cat "${_cpu}scaling_available_governors")
      case "$_avail" in
      *schedutil*)
        write "${_cpu}scaling_governor" schedutil
        write "${_cpu}schedutil/up_rate_limit_us" "${CPU_SU_UP-4000}"
        write "${_cpu}schedutil/down_rate_limit_us" "${CPU_SU_DOWN-16000}"
        write "${_cpu}schedutil/pl" "${CPU_SU_PL-0}"
        write "${_cpu}schedutil/iowait_boost_enable" "${CPU_SU_IOWAIT-0}"
        write "${_cpu}schedutil/rate_limit_us" "${CPU_SU_RATE-16000}"
        write "${_cpu}schedutil/hispeed_load" "${CPU_SU_HISPEED_LOAD-89}"
        write "${_cpu}schedutil/hispeed_freq" "${CPU_SU_HISPEED_FREQ-$CPU_MAXFREQ}"
        ;;
      *interactive*)
        write "${_cpu}scaling_governor" interactive
        write "${_cpu}interactive/timer_rate" "${CPU_INT_TIMER_RATE-40000}"
        write "${_cpu}interactive/boost" "${CPU_INT_BOOST-0}"
        write "${_cpu}interactive/timer_slack" "${CPU_INT_TIMER_SLACK-40000}"
        write "${_cpu}interactive/use_migration_notif" "${CPU_INT_MIGRATION-1}"
        write "${_cpu}interactive/ignore_hispeed_on_notif" "${CPU_INT_IGNORE_HISPEED-1}"
        write "${_cpu}interactive/use_sched_load" "${CPU_INT_SCHED_LOAD-1}"
        if [ -n "${CPU_INT_BOOSTPULSE-0}" ]; then
          write "${_cpu}interactive/boostpulse" "${CPU_INT_BOOSTPULSE-0}"
        fi
        write "${_cpu}interactive/fastlane" "${CPU_INT_FASTLANE-1}"
        write "${_cpu}interactive/fast_ramp_down" "${CPU_INT_FAST_RAMP_DOWN-0}"
        write "${_cpu}interactive/sampling_rate" "${CPU_INT_SAMPLING-40000}"
        write "${_cpu}interactive/sampling_rate_min" "${CPU_INT_SAMPLING_MIN-50000}"
        write "${_cpu}interactive/min_sample_time" "${CPU_INT_MIN_SAMPLE-50000}"
        write "${_cpu}interactive/go_hispeed_load" "${CPU_INT_GO_HISPEED-89}"
        write "${_cpu}interactive/hispeed_freq" "${CPU_INT_HISPEED_FREQ-$CPU_MAXFREQ}"
        ;;
      esac
    done

    if [ -d "$_S/sys/module/cpu_boost" ]; then
      if [ -n "${CPU_BOOST_FREQ-}" ]; then
        write "$_S/sys/module/cpu_boost/parameters/input_boost_freq" "${CPU_BOOST_FREQ-}"
      fi
      if [ -n "${CPU_BOOST_MS-}" ]; then
        write "$_S/sys/module/cpu_boost/parameters/input_boost_ms" "${CPU_BOOST_MS-}"
      fi
    fi
    if [ -d "$_S/sys/module/cpu_input_boost" ]; then
      if [ -n "${CPU_IBOOST_DUR-}" ]; then
        write "$_S/sys/module/cpu_input_boost/parameters/input_boost_duration" "${CPU_IBOOST_DUR-}"
      fi
      if [ -n "${CPU_IBOOST_FREQ-}" ]; then
        write "$_S/sys/module/cpu_input_boost/parameters/input_boost_freq_hp" "${CPU_IBOOST_FREQ-}"
        write "$_S/sys/module/cpu_input_boost/parameters/input_boost_freq_lp" "${CPU_IBOOST_FREQ-}"
      fi
    fi

    for _clk in "$_S"/sys/devices/system/cpu/cpufreq/policy*/ "$_S"/sys/devices/system/cpu/cpu*/cpufreq/; do
      if [ -e "${_clk}scaling_min_freq" ]; then
        write "${_clk}scaling_min_freq" "${CPU_MIN_FREQ-100000}"
        write "${_clk}scaling_max_freq" "${CPU_MAX_FREQ-$CPU_MAXFREQ}"
      fi
    done

    if [ -n "${CPU_CORE_CTL-1}" ]; then
      for _ctl in "$_S"/sys/devices/system/cpu/cpu*/core_ctl; do
        if [ -e "$_ctl/enable" ]; then
          write "$_ctl/enable" "1"
        elif [ -e "$_ctl/disable" ]; then
          write "$_ctl/disable" "0"
        fi
      done
    fi

    if [ -e "$_S/sys/devices/system/cpu/cpuidle/use_deepest_state" ]; then
      write "$_S/sys/devices/system/cpu/cpuidle/use_deepest_state" "${CPU_DEEPEST-1}"
    fi
  fi

  # GPU clocks and idle policy: tune frame response versus idle power.
  # GPU frequency values are in Hz. Vendor-specific nodes may be absent.
  # Performance retains legacy thermal-bypass requests; these can increase heat.
  if want gpu; then
    _S=${SYSFS_ROOT:-}
    write "$GPU_DIR/throttling" "${GPU_THROTTLE-1}"
    write "$GPU_DIR/thermal_pwrlevel" "${GPU_PWRLEVEL-0}"
    write "$GPU_DIR/devfreq/adrenoboost" "${GPU_ADRENOBOOST-0}"
    write "$GPU_DIR/force_no_nap" "${GPU_NONAP-0}"
    write "$GPU_DIR/bus_split" "${GPU_BUS_SPLIT-1}"
    write "$GPU_DIR/devfreq/max_freq" "${GPU_MAX_FREQ_SET-$GPU_MAXFREQ}"
    write "$GPU_DIR/devfreq/min_freq" "${GPU_MIN_FREQ_SET-100000000}"
    write "$GPU_DIR/default_pwrlevel" "${GPU_DEF_PWRLEVEL-$GPU_MINPL}"
    write "$GPU_DIR/force_bus_on" "${GPU_FORCE_BUS-0}"
    write "$GPU_DIR/force_clk_on" "${GPU_FORCE_CLK-0}"
    write "$GPU_DIR/force_rail_on" "${GPU_FORCE_RAIL-0}"
    write "$GPU_DIR/idle_timer" "${GPU_IDLE_TIMER-96}"

    if [ -e "$_S/proc/gpufreq/gpufreq_limited_thermal_ignore" ]; then
      write "$_S/proc/gpufreq/gpufreq_limited_thermal_ignore" "${GPU_THERMAL_IGNORE-0}"
    fi
    if [ -e "$_S/proc/mali/dvfs_enable" ]; then
      write "$_S/proc/mali/dvfs_enable" "1"
    fi
    if [ -e "$_S/sys/module/pvrsrvkm/parameters/gpu_dvfs_enable" ]; then
      write "$_S/sys/module/pvrsrvkm/parameters/gpu_dvfs_enable" "1"
    fi
    if [ -e "$_S/sys/module/simple_gpu_algorithm/parameters/simple_gpu_activate" ]; then
      write "$_S/sys/module/simple_gpu_algorithm/parameters/simple_gpu_activate" "1"
    fi

    if [ -d "$_S/sys/module/adreno_idler" ]; then
      write "$_S/sys/module/adreno_idler/parameters/adreno_idler_active" "${GPU_IDLER-Y}"
      if [ "${GPU_IDLER-Y}" = "Y" ]; then
        write "$_S/sys/module/adreno_idler/parameters/adreno_idler_idleworkload" "${GPU_IDLER_WORKLOAD-6000}"
        write "$_S/sys/module/adreno_idler/parameters/adreno_idler_downdifferential" "${GPU_IDLER_DOWNDIFF-25}"
        write "$_S/sys/module/adreno_idler/parameters/adreno_idler_idlewait" "${GPU_IDLER_IDLEWAIT-25}"
      fi
    fi
  fi

  # Storage queue: tune read-ahead (KiB), request depth and merge behavior.
  # Larger queues can help throughput but can increase latency and memory use.
  if want io; then
    _S=${SYSFS_ROOT:-}
    for _q in "$_S"/sys/block/*/queue/; do
      write "${_q}add_random" "${IO_ADD_RANDOM-0}"
      write "${_q}iostats" "${IO_IOSTATS-0}"
      write "${_q}read_ahead_kb" "${IO_READ_AHEAD-128}"
      write "${_q}nomerges" "${IO_NOMERGES-2}"
      write "${_q}rq_affinity" "${IO_RQ_AFFINITY-1}"
      write "${_q}nr_requests" "${IO_NR_REQUESTS-64}"
    done
  fi

  # Scheduler: tune task wakeup, migration and fairness controls.
  # These legacy vendor controls may not exist or have the same effect on every kernel.
  if want sched; then
    _S=${SYSFS_ROOT:-}
    if [ -e "$_S/sys/kernel/debug/sched_features" ]; then
      for _f in ${SCHED_FEATURES-NEXT_BUDDY TTWU_QUEUE NO_GENTLE_FAIR_SLEEPERS WAKEUP_PREEMPTION}; do
        write "$_S/sys/kernel/debug/sched_features" "$_f"
      done
    fi
    write "$_S/proc/sys/kernel/sched_child_runs_first" "${SCHED_CHILD_FIRST-0}"
    write "$_S/proc/sys/kernel/sched_boost" "${SCHED_BOOST-0}"
    write "$_S/proc/sys/kernel/perf_cpu_time_max_percent" "${SCHED_PERF_PCT-15}"
    write "$_S/proc/sys/kernel/sched_autogroup_enabled" "${SCHED_AUTOGROUP-1}"
    write "$_S/proc/sys/kernel/random/read_wakeup_threshold" "${SCHED_RAND_READ-64}"
    write "$_S/proc/sys/kernel/random/write_wakeup_threshold" "${SCHED_RAND_WRITE-256}"
    write "$_S/proc/sys/kernel/random/urandom_min_reseed_secs" "${SCHED_RESEED-90}"
    write "$_S/proc/sys/kernel/sched_tunable_scaling" "${SCHED_TUNABLE_SCALING-0}"
    write "$_S/proc/sys/kernel/sched_latency_ns" "${SCHED_LATENCY-4000000}"
    write "$_S/proc/sys/kernel/sched_min_granularity_ns" "${SCHED_MIN_GRAN-500000}"
    write "$_S/proc/sys/kernel/sched_wakeup_granularity_ns" "${SCHED_WAKEUP_GRAN-2000000}"
    write "$_S/proc/sys/kernel/sched_migration_cost_ns" "${SCHED_MIGRATION_COST-5000000}"
    write "$_S/proc/sys/kernel/sched_min_task_util_for_colocation" "${SCHED_COLOCATION-0}"
    write "$_S/proc/sys/kernel/sched_nr_migrate" "${SCHED_NR_MIGRATE-32}"
    write "$_S/proc/sys/kernel/sched_schedstats" "${SCHED_STATS-0}"
    write "$_S/proc/sys/kernel/sched_sync_hint_enable" "${SCHED_SYNC_HINT-0}"
    write "$_S/proc/sys/kernel/sched_user_hint" "${SCHED_USER_HINT-0}"
    if [ -n "${SCHED_CONSERVATIVE_PL-}" ]; then
      write "$_S/proc/sys/kernel/sched_conservative_pl" "${SCHED_CONSERVATIVE_PL-}"
    fi
    if [ -e "$_S/sys/kernel/sched/gentle_fair_sleepers" ]; then
      write "$_S/sys/kernel/sched/gentle_fair_sleepers" "${SCHED_GENTLE-1}"
    fi
    write "$_S/proc/sys/kernel/printk_devkmsg" "off"
  fi

  # Schedtune: bias foreground and background task placement.
  # Boost favors responsiveness at an energy cost; modern uclamp-only devices may skip these nodes.
  if want stune; then
    _S=${SYSFS_ROOT:-}
    if [ -d "$_S/dev/stune/" ]; then
      write "$_S/dev/stune/background/schedtune.boost" "${STUNE_BG_BOOST-0}"
      write "$_S/dev/stune/background/schedtune.prefer_idle" "${STUNE_BG_IDLE-0}"
      write "$_S/dev/stune/foreground/schedtune.boost" "${STUNE_FG_BOOST-0}"
      write "$_S/dev/stune/foreground/schedtune.prefer_idle" "${STUNE_FG_IDLE-1}"
      write "$_S/dev/stune/rt/schedtune.boost" "${STUNE_RT_BOOST-0}"
      write "$_S/dev/stune/rt/schedtune.prefer_idle" "${STUNE_RT_IDLE-0}"
      write "$_S/dev/stune/top-app/schedtune.boost" "${STUNE_TOP_BOOST-10}"
      write "$_S/dev/stune/top-app/schedtune.prefer_idle" "${STUNE_TOP_IDLE-1}"
      write "$_S/dev/stune/schedtune.boost" "${STUNE_BOOST-0}"
      write "$_S/dev/stune/schedtune.prefer_idle" "${STUNE_IDLE-0}"
    fi
  fi

  # Vendor thermal and efficiency controls: preserve this mode's existing policy.
  # Disabling vendor thermal controls can increase temperature; no performance gain is guaranteed.
  if want thermal; then
    _S=${SYSFS_ROOT:-}
    if [ -d "$_S/sys/module/msm_thermal" ]; then
      write "$_S/sys/module/msm_thermal/vdd_restriction/enabled" "${MSM_VDD-0}"
      write "$_S/sys/module/msm_thermal/core_control/enabled" "${MSM_CORE-1}"
      write "$_S/sys/module/msm_thermal/parameters/enabled" "${MSM_ENABLED-Y}"
    fi
    if [ -e "$_S/sys/module/workqueue/parameters/power_efficient" ]; then
      write "$_S/sys/module/workqueue/parameters/power_efficient" "${WQ_EFFICIENT-Y}"
    fi
    if [ -e "$_S/sys/devices/system/cpu/sched_mc_power_savings" ]; then
      write "$_S/sys/devices/system/cpu/sched_mc_power_savings" "${SCHED_MC-1}"
    fi
  fi

  # Virtual memory: tune dirty-page writeback, swapping and cache reclaim.
  # Tradeoffs depend on RAM, storage and workload; higher values do not universally improve speed.
  if want vm; then
    _S=${SYSFS_ROOT:-}
    sync
    if command -v sysctl >/dev/null 2>&1; then
      sysctl -w vm.drop_caches=3 >/dev/null 2>&1
    fi
    if [ -n "${VM_DROP_WRITE-3}" ]; then
      write "$_S/proc/sys/vm/drop_caches" "${VM_DROP_WRITE-3}"
    fi
    write "$_S/proc/sys/vm/dirty_background_ratio" "${VM_DIRTY_BG-10}"
    write "$_S/proc/sys/vm/dirty_ratio" "${VM_DIRTY_RATIO-30}"
    write "$_S/proc/sys/vm/dirty_expire_centisecs" "${VM_DIRTY_EXPIRE-1000}"
    write "$_S/proc/sys/vm/dirty_writeback_centisecs" "${VM_DIRTY_WRITEBACK-3000}"
    write "$_S/proc/sys/vm/page-cluster" "${VM_PAGE_CLUSTER-0}"
    write "$_S/proc/sys/vm/stat_interval" "${VM_STAT_INTERVAL-60}"
    write "$_S/proc/sys/vm/swappiness" "${VM_SWAPPINESS-100}"
    write "$_S/proc/sys/vm/laptop_mode" "${VM_LAPTOP_MODE-0}"
    write "$_S/proc/sys/vm/vfs_cache_pressure" "${VM_VFS_PRESSURE-50}"
  fi

  # Filesystem limits: adjust notification and lease controls where available.
  # These are compatibility settings, not a guarantee of faster storage.
  if want fs; then
    _S=${SYSFS_ROOT:-}
    if [ -d "$_S/proc/sys/fs" ]; then
      write "$_S/proc/sys/fs/dir-notify-enable" "0"
      write "$_S/proc/sys/fs/lease-break-time" "20"
      write "$_S/proc/sys/fs/leases-enable" "1"
    fi
  fi

  # Touch and display power controls: tune touch boost, panel savings and idle behavior.
  # Touch boost can improve input response while increasing power; nodes are vendor-specific.
  if want display; then
    _S=${SYSFS_ROOT:-}
    if [ -e "$_S/sys/touchpanel/double_tap" ] && [ -e "$_S/proc/tp_gesture" ]; then
      write "$_S/sys/touchpanel/double_tap" "1"
      write "$_S/proc/tp_gesture" "1"
    elif [ -e "$_S/proc/tp_gesture" ]; then
      write "$_S/proc/tp_gesture" "1"
    elif [ -e "$_S/sys/touchpanel/double_tap" ]; then
      write "$_S/sys/touchpanel/double_tap" "1"
    fi

    if [ -e "$_S/sys/module/msm_performance/parameters/touchboost" ]; then
      write "$_S/sys/module/msm_performance/parameters/touchboost" "${TOUCHBOOST-0}"
    elif [ -e "$_S/sys/power/pnpmgr/touch_boost" ]; then
      write "$_S/sys/power/pnpmgr/touch_boost" "${TOUCHBOOST-0}"
    fi

    if [ -n "${BATTERY_SAVER-}" ] && [ -d "$_S/sys/module/battery_saver" ]; then
      write "$_S/sys/module/battery_saver/parameters/enabled" "${BATTERY_SAVER-}"
    fi

    if [ -n "${ARCH_POWER-}" ] && [ -e "$_S/sys/kernel/sched/arch_power" ]; then
      write "$_S/sys/kernel/sched/arch_power" "${ARCH_POWER-}"
    fi

    if [ -n "${PM2_IDLE-Y}" ] && [ -e "$_S/sys/module/pm2/parameters/idle_sleep_mode" ]; then
      write "$_S/sys/module/pm2/parameters/idle_sleep_mode" "${PM2_IDLE-Y}"
    fi

    if [ -e "$_S/sys/class/lcd/panel/power_reduce" ]; then
      write "$_S/sys/class/lcd/panel/power_reduce" "${LCD_POWER-0}"
    fi
  fi

  # Same-page merging: control scanning for identical memory pages.
  # Merging can save RAM but costs CPU time; support depends on the kernel.
  if want ksm; then
    _S=${SYSFS_ROOT:-}
    if [ -e "$_S/sys/kernel/mm/ksm/run" ]; then
      write "$_S/sys/kernel/mm/ksm/run" "${KSM_RUN-1}"
    elif [ -e "$_S/sys/kernel/mm/uksm/run" ]; then
      write "$_S/sys/kernel/mm/uksm/run" "${KSM_RUN-1}"
    fi
  fi

  # Kernel and CPU-share controls: prioritize visible tasks and tune wakeup behavior.
  # Entropy thresholds and wakelock switches are legacy controls; effects vary by kernel.
  if want kernel; then
    _S=${SYSFS_ROOT:-}

    # CPU bandwidth: let cpufreq react to I/O wait and keep boost flags explicit.
    for _n in "$_S"/sys/devices/system/cpu/cpu*/cpufreq/* "$_S"/sys/devices/system/cpu/cpufreq/*; do
      [ -e "${_n}/boost" ] && write "${_n}/boost" "${KERNEL_CPUFREQ_BOOST-1}"
      [ -e "${_n}/io_is_busy" ] && write "${_n}/io_is_busy" "${KERNEL_IO_IS_BUSY-1}"
      [ -e "${_n}/iowait_boost_enable" ] && write "${_n}/iowait_boost_enable" "${KERNEL_IOWAIT_BOOST-1}"
      [ -e "${_n}/sampling_rate" ] && write "${_n}/sampling_rate" "${KERNEL_SAMPLING_RATE-20000}"
      [ -e "${_n}/timer_slack" ] && write "${_n}/timer_slack" "${KERNEL_TIMER_SLACK-10000}"
    done

    # EAS capacity boost (WALT kernels).
    write "$_S/proc/sys/kernel/sched_cfs_boost" "${KERNEL_CFS_BOOST-0}"

    # cgroup CPU shares: keep visible work ahead of background work.
    for _g in "$_S"/dev/cpuctl/top-app "$_S"/dev/cpuctl/foreground \
      "$_S"/dev/cpuctl/system-background "$_S"/dev/cpuctl/background \
      "$_S"/dev/cpuctl/bg_*; do
      [ -e "$_g/cpu.shares" ] || continue
      case "$_g" in
      *background*) write "$_g/cpu.shares" "${KERNEL_SHARES_BG-22}" ;;
      *) write "$_g/cpu.shares" "${KERNEL_SHARES_FG-52}" ;;
      esac
    done

    # Legacy entropy wakeup thresholds; availability and effect depend on the kernel.
    write "$_S/proc/sys/kernel/random/read_wakeup_threshold" "${KERNEL_ENTROPY_READ-128}"
    write "$_S/proc/sys/kernel/random/write_wakeup_threshold" "${KERNEL_ENTROPY_WRITE-512}"

    # Optional vendor network wakelock controls; disabling may affect background delivery.
    if [ "${KERNEL_WAKELOCKS-1}" = "0" ]; then
      for _w in "$_S"/sys/module/wakeup/parameters/enable_wlan*_ws \
        "$_S"/sys/module/wakeup/parameters/enable_qcom_rx_wakelock_ws; do
        [ -e "$_w" ] && write "$_w" "N"
      done
    fi

    # Printk spam costs wakeups; keep kernel messages off the console.
    write "$_S/proc/sys/kernel/printk_devkmsg" "off"
  fi

  # TCP buffering and connection timing: preserve the mode's network policy.
  # Larger buffers consume memory; shorter timers can affect long-lived connections.
  if want network; then
    _S=${SYSFS_ROOT:-}
    write "$_S/proc/sys/net/ipv4/tcp_fin_timeout" "${NET_TCP_FIN-30}"
    write "$_S/proc/sys/net/ipv4/tcp_tw_reuse" "${NET_TCP_TW_REUSE-1}"
    write "$_S/proc/sys/net/ipv4/tcp_keepalive_time" "${NET_TCP_KEEPALIVE-600}"
    write "$_S/proc/sys/net/ipv4/tcp_rmem" "${NET_TCP_RMEM-4096 87380 6291456}"
    write "$_S/proc/sys/net/ipv4/tcp_wmem" "${NET_TCP_WMEM-4096 87380 6291456}"
    write "$_S/proc/sys/net/core/rmem_max" "${NET_RMEM_MAX-6291456}"
    write "$_S/proc/sys/net/core/wmem_max" "${NET_WMEM_MAX-6291456}"
  fi

  # Android idle timing: Battery requests earlier idle maintenance; other modes clear it.
  # Earlier idle can reduce background activity but delay background delivery.
  if want doze; then
    if [ "${DOZE_IDLE-off}" = "battery" ]; then
      settings put global device_idle_constants "$DOZE_CONSTANTS" >/dev/null 2>&1
      dumpsys deviceidle enable all >/dev/null 2>&1
      log "doze: aggressive idle timings on"
    else
      settings delete global device_idle_constants >/dev/null 2>&1
    fi
  fi
  return 0
}
