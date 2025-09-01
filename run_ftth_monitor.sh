#!/bin/bash

types=(9 10 12 17 18)
regions=(fwrd krd mwrd erd wrd crd)
profile="prod"
dockername="ftth-monitor-image-5"

# Directory for logs
log_dir="/var/www/html/adsl/log"

# Clear old log files first
rm -f "$log_dir"/log_query_onudata_*.txt

# Run the final "nType=0" job first with logging
final_log="$log_dir/log_query_onudata_final_${profile}_$(date +%Y%m%d_%H%M%S).txt"
sudo docker run --rm --network host --name log_query_onudata_final ${dockername} "0" "$profile" > "$final_log" 2>&1 &

# Run type–region jobs in parallel
for type in "${types[@]}"; do
    for region in "${regions[@]}"; do
        logfile="$log_dir/log_query_onudata_${type}_${region}_${profile}_$(date +%Y%m%d_%H%M%S).txt"

        sudo docker run --rm --network host \
            --name monitor_${type}_${region}_${profile}_$(date +%s%N) \
            -v /home/barunntc/snmp-app/logs:/app/logs \
            ${dockername} "$type" "$region" "$profile" > "$logfile" 2>&1 &

        #echo "Started container for type=$type region=$region → $logfile"
    done
done

