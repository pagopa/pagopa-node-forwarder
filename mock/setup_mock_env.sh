#!/usr/bin/env bash

mock_instances=$1

if [ -z "$mock_instances" ]
then
  mock_instances=5
fi

# install packages
sudo apt update -y
sudo apt install -y python3 python3-pip haproxy

# install yarn
curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list
sudo apt install -y yarn

# install python modules
sudo pip3 install -r requirements.txt

# create certs folder for haproxy
cat api-dev-platform-pagopa-it-chain.pem api-uat-platform-pagopa-it-chain.pem > api.devuat.platform.pagopa.it.pem
cat fullchain.pem privkey.pem > mockec.ddns.net.pem
sudo mkdir /etc/haproxy/certs
sudo mv mockec.ddns.net.pem /etc/haproxy/certs
sudo mv api.devuat.platform.pagopa.it.pem /etc/haproxy/certs
sudo chmod -R go-rwx /etc/haproxy/certs

# configure haproxy
#sudo cp /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.backup
#
#sudo cp /etc/haproxy/haproxy.cfg haproxy.cfg
#sudo chown ubuntu:ubuntu haproxy.cfg
#cat <<EOT >> haproxy.cfg
#
#frontend http_front
#   bind *:8081
#   bind *:8080  ssl crt /etc/haproxy/certs/mockec.ddns.net.pem verify required ca-file /etc/haproxy/certs/api.devuat.platform.pagopa.it.pem
#   http-request redirect scheme https unless { ssl_fc }
#   stats uri /haproxy?stats
#   default_backend http_back
#
#backend http_back
#   balance roundrobin
#   server mockec1 127.0.0.1:8085 check
#   server mockec2 127.0.0.1:8086 check
#   server mockec3 127.0.0.1:8087 check
#   server mockec4 127.0.0.1:8088 check
#   server mockec5 127.0.0.1:8089 check
#EOT
#
#
#
#sudo mv haproxy.cfg /etc/haproxy/haproxy.cfg
#
#sh run_n_mockec.sh &
#sudo service haproxy restart

