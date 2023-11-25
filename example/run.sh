
export PROJECT_ROOT=$(pwd)
CONNECT_DOCKER_DIR=$PROJECT_ROOT/example

echo "Project root folder is $PROJECT_ROOT"
echo "Example docker folder is $CONNECT_DOCKER_DIR"

# build docker image - prepare encryption converter and gcp key file
cp $PROJECT_ROOT/target/kafka-config-provider-gcp-1.0-SNAPSHOT.jar $CONNECT_DOCKER_DIR
cp $GCP_JSON_KEY_FILEPATH $CONNECT_DOCKER_DIR/gcp-sa.json

# build docker image - docker building
cd $CONNECT_DOCKER_DIR
docker rmi -f kafka-connect-example:cp750-1.0
docker build -t kafka-connect-example:cp750-1.0 .

# build docker image - remove the tmp files
rm $CONNECT_DOCKER_DIR/*.jar
rm $CONNECT_DOCKER_DIR/*.json

# run docker-compose - remove the existing docker
docker-compose -f docker-compose.yml rm --force
# run docker-compose - start
docker-compose -f docker-compose.yml up