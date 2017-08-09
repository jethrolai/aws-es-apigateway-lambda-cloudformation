#!/usr/bin/env bash

## This can be part of Jenkins or CI server's task configure

if [ $# -eq 0 ]
  then
    (>&2 echo "You must specify the environment as argument")
    exit 1
fi

echo "Destroying es-demo envrionment $1 ..."
echo "Removing all artifacts in s3://es-demo-lambda-artifacts-$1/"
aws s3 rm --recursive --profile es-demo s3://es-demo-lambda-artifacts-$1/
echo "Done"
echo "Deleting cloud formation stack: es-demo-s3-$1"
aws cloudformation delete-stack --profile es-demo --stack-name es-demo-s3-$1
echo "Done"
echo "Deleting cloud formation stack: es-demo-$1"
aws cloudformation delete-stack --profile es-demo --stack-name es-demo-$1
echo "Done"

echo "es-demo envrionment $1 deleted."
