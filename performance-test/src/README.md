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


# Load test 
88146 requests => 25 VUs
61822 requests => 17 VUs
111721 requests => 31 VUs
135056 requests => 38 VUs
60051 requests => 17 VUs
100878 requests => 28 VUs
371840 requests => 103 VUs
657122 requests => 183 VUs
701950 requests => 195 VUs
660340 requests => 183 VUs
533896 requests => 148 VUs
254192 requests => 71 VUs
228293 requests => 63 VUs
312490 requests => 87 VUs
403109 requests => 112 VUs
427699 requests => 119 VUs
410615 requests => 114 VUs
243673 requests => 68 VUs
169375 requests => 47 VUs
108531 requests => 30 VUs
71026 requests => 20 VUs
46218 requests => 13 VUs
43560 requests => 12 VUs
30582 requests => 8 VUs

