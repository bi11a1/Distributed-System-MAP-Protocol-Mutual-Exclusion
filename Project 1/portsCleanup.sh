#!/bin/bash

# This closes the currently open ports that needs to be freed for the servers
# Code adapted from posted example by Jordan Frimpter
# Command to grant permission to file to run [RUN THIS]: chmod +x portsCleanup.sh

# Change this to your netid [CHANGE THIS]
netid=mxh230007

# Root directory of project [CHANGE THIS]
PROJDIR=/people/cs/m/mxh230007/public_html/Project1

# Directory where the config file is located on your local system [CHANGE THIS]
CONFIGLOCAL=/people/cs/m/mxh230007/public_html/Project1/config.txt

# extension for hosts [CHANGE THIS if using a different host system (setting to "" should suffice)]
hostExtension="utdallas.edu"

# loop through hosts, remove comment lines starting with # and $ and any carriage returns
n=0
hostname_array=()
port_array=()
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
    	port=$( echo $line | awk '{ print $3 }' )
        host=$( echo $line | awk '{ print $2 }' )

        # add host extension to string if missing from domain name
            if [[ "$host" != *"$hostExtension"* ]];
        then
            host="$host.$hostExtension"
        fi

        hostname_array+=("$netid@$host")
	port_array+=($port)

        # increment loop counter
        n=$(( n + 1 ))
    done


    # Cloeses all the processes running on the server port for each machines
    for ((i = 0; i < ${#hostname_array[@]}; i++)); do
        hostname=${hostname_array[i]}
	port=${port_array[i]}

	echo "Closing listed PID's below for: $hostname:$port"
	ssh $hostname lsof -t -i :$port
        ssh_result=$(ssh $hostname "kill \$(lsof -t -i :$port)" 2>&1)

        # Check if SSH command was successful
        if [ $? -eq 0 ]; then
            echo "Successfully closed these processes."
        else
            echo "Error: No running process on the port."
        fi
    done
)

sleep 1
echo "Done! Ports are ready to use."
