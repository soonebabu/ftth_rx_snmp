#!/bin/bash

args=(9 10 12 17 18)

for arg in "${args[@]}"
do
  logfile="/home/barunntc/snmp-app/logs/log_${arg}_$(date +%Y%m%d_%H%M%S).txt"
  sudo docker run --rm --network host \
    -v /home/barunntc/snmp-app/logs:/app/logs \
    ftth-monitor-image "$arg" > "$logfile" 2>&1 &
  echo "Started container with arg=$arg logging to $logfile"
done
