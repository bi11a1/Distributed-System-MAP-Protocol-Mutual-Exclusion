#!/bin/bash

# Code adapted from posted example by Jordan Frimpter
# Command to grant permission to file to run [RUN THIS]: chmod +x cleanup.sh
# Note: I made some modification to the Jordan Frimpter's code.

# Change this to your netid [CHANGE THIS]
netid=mxh230007

# Root directory of project [CHANGE THIS]
PROJDIR=/people/cs/m/mxh230007/public_html/Project1

# Directory where the config file is located on your local system [CHANGE THIS]
CONFIGLOCAL=/people/cs/m/mxh230007/public_html/Project1/config.txt

# extension for hosts [CHANGE THIS if using a different host system (setting to "" should suffice)]
hostExtension="utdallas.edu"

# loop through hosts, remove comment lines starting with # and $ and any carriage returns
# Modifcation: hostname_array() stores the all of the host names before the start of cleanup
n=0
hostname_array=()

# remove comments | remove other comments | remove carriage returns
cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" | sed -e "s/\r$//" |
(
    # read the first valid line and collect only the number of hosts
    read i
    echo $i
    ii=$( echo $i| awk '{ print $1 }' )
    echo Hosts: $ii

    # for each host, loop
    while [[ $n -lt $ii ]]
    do
        # read the port number and host address
    	read line
    	p=$( echo $line | awk '{ print $1 }' )
        host=$( echo $line | awk '{ print $2 }' )

        # add host extension to string if missing from domain name
            if [[ "$host" != *"$hostExtension"* ]];
        then
            host="$host.$hostExtension"
        fi
        # echo $host
	# echo "$netid@$host"
        hostname_array+=("$netid@$host")

        # sleep 1

        # increment loop counter
        n=$(( n + 1 ))
    done


    # Print the hostnames
    echo "Hostnames:"
    for hostname in "${hostname_array[@]}"; do
        echo "$hostname"
        # Perform cleanup
        ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no "$hostname" "killall -u $netid"
	# sleep 1
    done
)

sleep 1
echo "Cleanup complete"
