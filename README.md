# AWS API Gateway + Lambda + ElasticSearch Service Demo
This project is to demo a simple general search facade api built in AWS that takes arbitrary query and perform the search on an AWS elasticsearch cluster backend.  


#### \[ Do not use this in production !!\]

## Goals
1. Import data from a CSV file to elasticsearch cluster
1. Build an api using Lambda function as proxy to facilitate elasticsearch search API.
1. Expose the api via API Gateway
1. Manage CI/CD flow from development to production

## Best Practice
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
- Unit test should be run every build, every commit.
- Dev/staging deployment should happen automatically after every pull request approval.
- Dev/staging deployment should trigger integration tests.
- Promote step (deploy tested artifact to prod) should be manual via CI server after integration test is done.  

## In this demo
As mentioned above, we will simplify the stack in the demo as proof of concept approach.
- It's my first time ever using CloudFormation and I found it took me a bit time to learn all of the resource specs aforementioned and fine tune the template and I had only few hours to work on the initial version of this thing. Therefore, I ran out of time so the template only captures about 90% of the infrastructure and integration. My demo infrastructure is set up by CloudFormation with a few manual touches. I might come back and finish it up or use terraform in the next version.
- No KMS encrypted for lambda
- No API gateway Client certificate
- No Firehose pipeline. I use a simple python script(csv_importer.py) instead.


## Complete Walk Through

Before
This shell scripts are merely for demonstration. You should set up your own CI flow using the commands in the scripts. For example, update this shell script and place in your CI server as a task triggered by your source code pull request approval for setting an integration test instant. For production, you can use this script with a dedicated role to protect the entire stack.

#### 1. Set up your development environment
You need python and virtualenv installed because we will be using the newest version of awscli in this demo.
Dependency versions will be provided in the future.
Don't miss the '.' before _env_setup.sh_
```bash
. env_setup.sh
```

#### 2. Create a role that has S3, ElasticSearch, CloudWatch, API-Gateway, Lambda permission and add it to your local profile, called _es-demo_.
  * We will come back to add the list of resources and actions permission in cloudformation when we have time.
  * if you don't know how to do that, visit [http://docs.aws.amazon.com/cli/latest/userguide/cli-multiple-profiles.html]

#### 3. Create the entire infrastructure
Use 'demo' as environment name.
```bash
./init.sh demo
```

#### 4. Ingest data
  * Wait till the elastic search cloud is up
  * this will take a while. I implemented this to upload data in batch so even when you terminate the process at some point, the data will be partially imported.  
  * You can create a route53 alias point to the new elastic search endpoint so the python script can be integrated into the same dev flow based on environment key
```bash
python csv_importer.py {elastic search endpoint}
```

#### 5. Deployment the new version
Simply update the ElasticSearchAWSStreamLambdaFacade class and run:
```bash
deploy.sh demo
```
This will deploy the version to dev stage in your environment

#### 6. Promote a version to production
```bash
promote.sh demo
```

#### 7. Destroy the infrastructure
```bash
destroy.sh demo
```

## In Depth:
  * Though the initial version is far from the best practice we discussed above, we manage to:
    1. set up entire infrastructure and deploy the initial version and make it available via api gateway within one action as matter of seconds.
    2. dev deployment and prod deployment/promotion also happen in one command within few seconds.
    3. it supports indefinite number of environments with a unique env key and comes with internal concept of dev and prod instances.
    4. destroy any environment also within seconds.
    5. this development is portable and self-contained.
    6. the facade implementation exposes an api that's decoupled from data schema and take arbitrary query with exposing any other elasticsearch api.
  * One requirement is to use Java as lambda runtime and we need the artifact in a S3 bucket before we create the lambda function. java8 runtime is not supported by CloudFormation with "ZipFile" property, so this is the reason there are two stacks instead of one. I believe there is a more graceful way to use cloud formation. I just need a bit more time to learn how it works. This can be easily achieved by aws cli or terraform.
  * Another original requirement is to support 3 fields on the type in an index. I thought it'd be even more useful to create a generic search facade api but decouple the persistence level schema with frontend API spec and it will support all arbitrary search terms and fields.
  * The response can be further parsed and re-structured. The response format should rely on the actually product requirements.
  * Pretty domain name setup in Route53 is not captured in this template
  * I am aware that S3 or other place is better for the test data instead of placing in the source code repo. I will come back to this later. 
  * Authorization and security related resources are not set up entirely in this template.
  * There are many many other options for managing different stages or environments. It's not limited to the way this demo does it. It depends on actual requirements.
  * I experimented a few different ways of ingesting data into elasticsearch but didn't really spend a lot of time on it. Either elastic search's bulk api or index api approaches are slow in this demo. There is room for optimization.
  * Unit tests triggered during every build and after every commit. Integration test suite should be automatically run after any merge of branch. Regression and any other type of post-production testing should be executed after promotion step. I didn't have time to implement the integration, regression or unit test part of this demo. I will come back and add it.
  * I haven't figured out how to use stage variable as function binding in cloud formation's api gateway method set up. I will have to come back to this.
  * There are probably hundreds more places I can improve. Please just treat the initial version as a proof of concept

## Collaboration
  email: jethro@jethrolai.com
