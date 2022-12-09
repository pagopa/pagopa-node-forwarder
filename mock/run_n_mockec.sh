#!/usr/bin/env bash

mock_instances=$1

if [ -z "$mock_instances" ]
then
  mock_instances=5
fi

start=9000
end=$(($start + $mock_instances))
port=$start
while [ "$port" -le $end ]; do
  echo "kill old execution on ${port}"
  lsof -i :$port -sTCP:LISTEN |awk 'NR > 1 {print $2}' | xargs kill -15

  echo "starting execution on ${port}"
  python3 mockec.py $port &
  port=$(($port+1))
done

exit 1
