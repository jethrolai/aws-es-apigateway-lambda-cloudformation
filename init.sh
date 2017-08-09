#!/usr/bin/env bash

## This can be part of Jenkins or CI server's task configure

if [ $# -eq 0 ]
  then
    (>&2 echo "You must specify the environment as argument")
    exit 1
fi

echo "Initializing es-demo envrionment $1 ..."
echo "Creating cloud formation stack: es-demo-s3-$1 ..."
aws cloudformation create-stack --profile es-demo --parameters ParameterKey=env,ParameterValue=$1 --stack-name es-demo-s3-$1 --template-body "$(< infra/es-cf-s3.json)"
echo "Done"
echo "Building initial artifact ..."
mvn clean package
echo "Done"
echo "Uploading initial artifact target/aws-es-lambda-apigateway-1.0.2-SNAPSHOT.jar to s3://es-demo-lambda-artifacts-$1/ ..."
aws s3 cp --profile es-demo target/aws-es-lambda-apigateway-1.0.2-SNAPSHOT.jar s3://es-demo-lambda-artifacts-$1/
echo "Done"
echo "Creating cloud formation stack: es-demo-$1 ..."
aws cloudformation create-stack --profile es-demo --capabilities CAPABILITY_IAM --parameters ParameterKey=env,ParameterValue=$1 --stack-name es-demo-$1 --template-body "$(< infra/es-cf-infra.json)"
echo "Done"
echo "es-demo envrionment $1 initilization completed. CloudFormation stacks: es-demo-s3-$1 and es-demo-$1 created"
