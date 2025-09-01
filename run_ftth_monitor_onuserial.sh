#!/bin/bash

profile="prod"

# Directory for logs
log_dir="/var/www/html/adsl/log"

# Clear old log files first
rm -f "$log_dir"/log_query_onuserial*.txt

sudo docker run --rm --network host --name monitor_ONU_Serial_$(date +%s%N) ftth-monitor-image-5  "1" "$profile" > "$log_dir/log_query_onuserial" 2>&1 &

