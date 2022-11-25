# sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey> ID_PSP ID_BROKER_PSP ID_CHANNEL ID_CI ID_BROKER_CI ID_STATION NOTICE_NUMBER HOST_URL HOST_PORT HOST_PATH

ENVIRONMENT=$1
TYPE=$2
SCRIPT=$3
DB_NAME=$4
API_SUBSCRIPTION_KEY=$5
ID_PSP=$6
ID_BROKER_PSP=$7
ID_CHANNEL=$8
ID_CI=$9
ID_BROKER_CI=$10
ID_STATION=$11
NOTICE_NUMBER=$12
HOST_URL=$13
HOST_PORT=$14
HOST_PATH=$15
DEBUG=$16

if [ -z "$ENVIRONMENT" ]
then
  echo "No env specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey>"
  exit 1
fi

if [ -z "$TYPE" ]
then
  echo "No test type specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey>"
  exit 1
fi
if [ -z "$SCRIPT" ]
then
  echo "No script name specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey>"
  exit 1
fi

if [ -z "$DB_NAME" ]
then
  DB_NAME="k6"
  echo "No DB name specified: 'k6' is used."
fi

export env=${ENVIRONMENT}
export type=${TYPE}
export script=${SCRIPT}
export db_name=${DB_NAME}
export sub_key=${API_SUBSCRIPTION_KEY}
export id_psp=${ID_PSP}
export id_broker_psp=${ID_BROKER_PSP}
export id_channel=${ID_CHANNEL}
export id_ci=${ID_CI}
export id_broker_ci=${ID_BROKER_CI}
export id_station=${ID_STATION}
export notice_number=${NOTICE_NUMBER}
export host_url=${HOST_URL}
export host_port=${HOST_PORT}
export host_path=${HOST_PATH}
export debug=${DEBUG}

stack_name=$(cd .. && basename "$PWD")
docker compose -p "${stack_name}-k6" up -d --remove-orphans --force-recreate --build
docker logs -f k6
docker stop nginx
