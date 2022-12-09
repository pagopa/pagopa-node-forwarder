#!/usr/bin/env bash

#

mock_instances=$1
script_version=$2

if [ -z "$mock_instances" ]
then
  mock_instances=5
fi

if [ -z "$script_version" ]
then
  script_version="python"
fi

run_script="run_n_mockec.sh"
if [ "$script_version" = "python" ]; then
    run_script="run_n_mockec.sh"
else
    run_script="run_n_mockec_nodejs.sh"
fi

# configure haproxy
sudo cp /etc/haproxy/haproxy.cfg.backup haproxy.cfg
# TODO change mocked.ddns.net.pem with AWS certificate
sudo chown ubuntu:ubuntu haproxy.cfg
cat <<EOT >> haproxy.cfg

frontend http_front
   bind *:8081
   bind *:8080 ssl crt /etc/haproxy/certs/mockec.ddns.net.pem verify required ca-file /etc/haproxy/certs/api.devuat.platform.pagopa.it.pem
   http-request redirect scheme https unless { ssl_fc }
   stats uri /haproxy?stats
   default_backend http_back

backend http_back
EOT

start=9000
end=$(($start + $mock_instances))
port=$start
while [ "$port" -le $end ]; do
  echo "   server mockec${port} 127.0.0.1:${port} check" >> haproxy.cfg
  port=$(($port+1))
done

sudo mv haproxy.cfg /etc/haproxy/haproxy.cfg

sh $run_script $mock_instances

sudo service haproxy restart

exit 1
