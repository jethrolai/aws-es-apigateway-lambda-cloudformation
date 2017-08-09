# AWS API Gateway + Lambda + ElasticSearch Service Demo
This project is to demo a simple general search facade api built in AWS that takes arbitrary query and perform the search on an AWS elasticsearch cluster backend.  
#### \[ Do not use this in production !!\]

## Goals
1. Import data from a CSV file to elasticsearch cluster
1. Build an api using Lambda function as proxy to facilitate elasticsearch search API.
1. Expose the api via API Gateway
1. Manage CI/CD flow from development to production

### Best Practice
Under certain time constraints, I will build a non-production ready demo but here is a list of opinionated best practice over demo implementation

- a CloudFormation template to set up the infrastructure which includes:
  - a development IAM group with a role and policy to allow developers within the group to freely create and destroy entire stack assuming the role
  - two Lambda functions of Java implementation to delegate, translate requests and responses between frontend and backend
    - one is for development where the other one is for production
    - an access control policy should be attached to this lambda function to only allow invocation to the api gateway counterpart
    - use KMS to encrypt environment setting
  - a S3 bucket for lambda build artifact
    - the S3 bucket should be hardened with bucket policy specifying only the developer role can write/read objects, create/destroy buckets
    - we can further define the policy with greater granularity to separate object write/read and bucket permission to decouple application and infrastructure development (if necessary)
  - an API gateway with a resource, two stages, one model, one method, two deployment, two usage plans, two api keys
    - dev and production stages should each have a stage variable, say, _lamdba_ whose value is the name of its lambda function counterpart
    - in api method under Integration Request, specifying ${stageVariables.lambda} as lambda function instead of static string.
    - use self signed client certificate to further secure the communication between itself and lambda backend.
  - a Kinesis Firehose stream should be set up for data ingestion.
    - a lambda function can be used to process the incoming traffic
    - grok or other processor on elasticsearch cluster can also be used to further define the structure of data
  - Other resources for User friendly api url, CI/CD, monitoring and metrics etc.

### In this demo
As mentioned above, we will simplify the stack in the demo as proof of concept approach.
- It's my first time ever using CloudFormation and I found it took me a bit time to learn all of the resource specs aforementioned and fine tune the template and I had only few hours to work on the initial version of this thing. Therefore, I ran out of time so the template only captures about 90% of the infrastructure and integration. My demo infrastructure is set up by CloudFormation with a few manual touches. I might come back and finish it up or use terraform in the next version.
- No KMS encrypted for lambda
- No API gateway Client certificate
- No Firehose pipeline. I use a simple python script(csv_importer.py) instead.


## Complete Walk Through
Before you start, make sure you have maven python and virtualenv installed because we will be using the newest version of awscli in this demo.
Dependency versions will be provided in the future.
<aside class="notice">
This shell scripts are merely for demonstration. You should set up your own CI flow using the commands in the scripts. For example, update this shell script and place in your CI server as a task triggered by your source code pull request approval for setting an integration test instant. For production, you can use this script with a dedicated role to protect the entire stack.
</aside>

1. set up your development environment
```
virtuanlenv .venv
source .venv/bin/activate
pip install -r requirements.txt
brew install maven
```

1. Create a role that has S3, ElasticSearch, CloudWatch, API-Gateway, Lambda permission and add it to your local profile, called _es-demo_.
  * We will come back to add the list of resources and actions permission in cloudformation when we have time.
  * if you don't know how to do that, visit [http://docs.aws.amazon.com/cli/latest/userguide/cli-multiple-profiles.html]
1. Create the entire infrastructure
Use 'demo' as environment name.
```
./init.sh demo
```
1. Ingest data
  * Wait till the elastic search cloud is up
  * this will take a while. I implemented this to upload data in batch so even when you terminate the process at some point, the data will be partially imported.  
  * You can create a route53 alias point to the new elastic search endpoint so the python script can be integrated into the same dev flow based on environment key
```
python csv_importer.py {elastic search endpoint}
```
1. Deployment the new version
Simply update the ElasticSearchAWSStreamLambdaFacade class and run:
```
deploy.sh demo
```
This will deploy the version to dev components in your environment
1. Promote a version to production
```
promote.sh demo
```
1. Destroy the infrastructure
```
destroy.sh demo
```

## In Depth:
  * A requirement is to use Java as lambda runtime and we need the artifact in a S3 bucket before we create the lambda function. java8 runtime is not supported by CloudFormation with "ZipFile" property, so this is the reason there are two stacks instead of one. I believe there is a more graceful way to use cloud formation. I just need a bit more time to learn how it works. This can be easily achieved by aws cli or terraform.
  * Another original requirement is to support 3 fields on the type in an index. I thought it'd be even more useful to create a generic search facade api but decouple the persistence level schema with frontend API spec and it will support all arbitrary search terms and fields.
  * Pretty domain name setup in Route53 is not captured in this template
  * Authorization and security related resources are not set up entirely in this template.
  * There are many many other options for managing different stages or environments. It's not limited to the way this demo does it. It depends on actual requirements.
  * I experimented a few different ways of ingesting data into elasticsearch but didn't really spend a lot of time on it. Either elastic search's bulk api or index api approaches are slow in this demo. There is room for optimization.
  * I didn't have time to implement the integration, regression or unit test part of this demo. I will come back and add it.
  * I don't know how to use stage variable as function binding in cloud formation's api gateway method set up. I will have to come back to this.
  * There are probably hundreds more places I can improve. Please just treat the initial version as a proof of concept
  * Here is a hidden gem for you for reading this till the end. I set up this demo on [https://api.jethrolai.com/es]. Please go and check it out. This live demo might be terminated shortly after the release of the initial version.
  * Wanna see how it works? Simply do `curl https://api.jethrolai.com/es?PLAN_NAME=`

## Collaboration
  email: jethro@jethrolai.com
