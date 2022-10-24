# K6 tests

This is a set of [k6](https://k6.io) tests.

To invoke k6 test passing parameter use -e (or --env) flag:

```
-e MY_VARIABLE=MY_VALUE
```

## How to run 🚀

Use this command to launch the load tests:

```
k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json <script-name>.js
```

