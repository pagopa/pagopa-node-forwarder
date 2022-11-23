### How To

Launch one of the following command according to environment from the current directory `perf-tests`.

_Note_: configure environment through `*.environment.json` file.

##### K6 Scripts
- `node_pa.js`: to call node-forwarder using a `paVerifyPaymentNotice`
- `psp_node_pa.js`: to call node using a `verifyPaymentNotice`

#### Example usage:
##### Local
k6 run --env VARS="local.environment.json" \
--env ID_CI="77777777777" \
--env ID_STATION="77777777777_07" \
--env NOTICE_NUMBER="307111111112222222" \
--env TEST_TYPE="./test-types/single.json" \
--env HOST_URL="mockec.ddns.net" \
--env HOST_PORT=8080 \
--env HOST_PATH="/servizi/PagamentiTelematiciRPT" \
node_pa.js
 
 
##### Dev

k6 run --env VARS="dev.environment.json" \
--env API_SUBSCRIPTION_KEY="<SUBSCRIPTION_KEY>" \
--env ID_CI="77777777777" \
--env ID_BROKER_CI="77777777777" \
--env ID_STATION="77777777777_07" \
--env NOTICE_NUMBER="307111111112222222" \
--env TEST_TYPE="./test-types/single.json" \
--env HOST_URL="mockec.ddns.net" \
--env HOST_PORT=8080 \
--env HOST_PATH="/servizi/PagamentiTelematiciRPT" \
node_pa.js
