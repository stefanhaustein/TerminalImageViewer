#!/bin/bash

interval_seconds="$1"
if [ -z "${interval_seconds}" ]
then
  interval_seconds=4

  echo "Using default interval time. Pass number of seconds as program argument to change this."
fi
echo "Will show image every ${interval_seconds} seconds."

`command -v tput > /dev/null 2>&1`
if [ $? -eq 0 ]
then
  cols=`tput cols`
  rows=`tput lines`

  not=
else
  cols=80
  rows=24

  not=\ not
fi
echo "tput command${not} found. Using ${cols}x${rows} terminal size."

echo "Starting slideshow..."
sleep 2

while true
do
  clear
  ./tiv.sh -w "$cols" -h "$rows" http://thecatapi.com/api/images/get
  sleep "$interval_seconds"
done
