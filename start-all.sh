#!/usr/bin/env sh
# Starts the SOAP server, the RMI server and the portal (in this order).
# Server output goes to employee-soap/server.log and library-rmi/server.log; Ctrl+C stops everything.
set -e
cd "$(dirname "$0")"
[ -f dscc-portal/target/dscc-portal.jar ] || ./build-all.sh

echo "Starting Employee SOAP server..."
./run.sh soap-server "$@" > employee-soap/server.log 2>&1 &
SOAP_PID=$!
sleep 6
echo "Starting Library RMI server..."
./run.sh rmi-server "$@" > library-rmi/server.log 2>&1 &
RMI_PID=$!
trap 'kill $SOAP_PID $RMI_PID 2>/dev/null' EXIT INT TERM
sleep 4
echo "Starting the portal..."
./run.sh portal
