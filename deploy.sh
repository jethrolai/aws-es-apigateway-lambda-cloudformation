#!/usr/bin/env bash

## This can be part of Jenkins or CI server's task configure

if [ $# -eq 0 ]
  then
    (>&2 echo "You must specify the environment as argument")
    exit 1
fi

echo "Building and deploying new version ..."
echo "Building artifact ..."
mvn clean package
echo "Done"
echo "Uploading the artifact target/aws-es-lambda-apigateway-1.0.2-SNAPSHOT.jar to s3://es-demo-lambda-artifacts-$1/ ..."
aws s3 cp --profile es-demo target/aws-es-lambda-apigateway-1.0.2-SNAPSHOT.jar s3://es-demo-lambda-artifacts-$1/
echo "Done"
echo "Publishing new version to dev lambda "
aws lambda update-function-code --profile es-demo --function-name es-demo-lambda-$1-dev --s3-bucket es-demo-lambda-artifacts-$1 --s3-key aws-es-lambda-apigateway-1.0.2-SNAPSHOT.jar --publish
echo "Done "
