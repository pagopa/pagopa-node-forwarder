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
  folder="pagopa-mock-ec-${port}"
  if [ ! -d "$folder" ];
  then
    echo -n "configuring pagopa-mock-ec-${port}"
    git clone https://github.com/pagopa/pagopa-mock-ec.git "pagopa-mock-ec-${port}"
    cd "pagopa-mock-ec-${port}"

    cat <<-EOT >> .env
PORT=${port}
PPT_NODO=/servizi/PagamentiTelematiciRPT
TIMEOUT_DELAY=10
CC_BANK_PRIMARY_EC=IT96R0123454321000000012345
CC_BANK_SECONDARY_EC=IT04O0100003245350008332100
TEST_DEBUG=Y
AUX_DIGIT=3
EOT

    yarn install
    yarn build
    cd ..
  fi
  port=$(($port+1))
done

echo -n "starting project"

port=$start
while [ "$port" -le $end ]; do
  echo "kill old execution on ${port}"
  lsof -i :$port -sTCP:LISTEN |awk 'NR > 1 {print $2}' | xargs kill -15

  echo "starting execution on ${port}"

  cd "pagopa-mock-ec-${port}"
  yarn start &
  cd ..

  port=$(($port+1))
done

exit 1
